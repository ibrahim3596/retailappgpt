import { PrismaClient } from "@prisma/client";
import { z } from "zod";
import {
  SaleCommandSchema,
  CustomerPaymentCommandSchema,
  ProductUpsertCommandSchema,
  CustomerUpsertCommandSchema
} from "../contracts/schemas";
import { calculateBilling } from "./billing.service";

/** A business-rule rejection (not a bug): surfaces as HTTP 409 to clients. */
export class SyncConflictError extends Error {
  constructor(
    public readonly code:
      | "INSUFFICIENT_STOCK"
      | "CREDIT_LIMIT_EXCEEDED"
      | "UNDERPAYMENT"
      | "CUSTOMER_REQUIRED"
      | "CUSTOMER_NOT_FOUND",
    message: string
  ) {
    super(message);
    this.name = "SyncConflictError";
  }
}

const MAX_INVOICE_NUMBER_ATTEMPTS = 5;

export async function processSaleCommand(
  prisma: PrismaClient,
  storeId: string,
  command: z.infer<typeof SaleCommandSchema>,
  idempotencyKey: string
) {
  // Two concurrent first-time sales can both compute the same invoice number;
  // the unique constraint on officialInvoiceNumber turns the loser into a
  // retry with a fresh count.
  for (let attempt = 1; attempt <= MAX_INVOICE_NUMBER_ATTEMPTS; attempt++) {
    try {
      return await applySaleCommand(prisma, storeId, command, idempotencyKey);
    } catch (err) {
      const isUniqueViolation =
        typeof err === "object" &&
        err !== null &&
        "code" in err &&
        (err as { code?: string }).code === "P2002";
      if (isUniqueViolation && attempt < MAX_INVOICE_NUMBER_ATTEMPTS) continue;
      throw err;
    }
  }
  throw new Error("Unreachable");
}

async function applySaleCommand(
  prisma: PrismaClient,
  storeId: string,
  command: z.infer<typeof SaleCommandSchema>,
  idempotencyKey: string
) {
  return await prisma.$transaction(async (tx) => {
    // 1. Idempotency Check
    const existingLog = await tx.syncCommandLog.findUnique({
      where: { idempotencyKey }
    });
    if (existingLog) {
      return { status: "ALREADY_PROCESSED", message: "Command already applied" };
    }

    // 2. Fetch authoritative product metadata
    const productIds = command.items.map((i) => i.productId);
    const productsList = await tx.product.findMany({
      where: { storeId, id: { in: productIds } }
    });
    const productMap = new Map(productsList.map((p) => [p.id, p]));

    const missing = productIds.filter((id) => !productMap.has(id));
    if (missing.length > 0) {
      throw new SyncConflictError("INSUFFICIENT_STOCK", `Unknown product(s): ${missing.join(", ")}`);
    }

    // 3. Stock validation before any mutation: both the aggregate counter and
    // the FEFO-allocated batches must cover the requested quantity.
    for (const item of command.items) {
      const prod = productMap.get(item.productId)!;
      const batches = await tx.batch.findMany({
        where: { storeId, productId: item.productId, quantity: { gt: 0 } }
      });
      const batchTotal = batches.reduce((sum, b) => sum + Number(b.quantity), 0);
      const available = Math.min(Number(prod.currentStock), batchTotal);
      if (available < item.quantity) {
        throw new SyncConflictError(
          "INSUFFICIENT_STOCK",
          `Insufficient stock for product ${prod.name}. Requested: ${item.quantity}, Available: ${available}`
        );
      }
    }

    // 4. Server-Authoritative Billing Calculation
    const productPriceMap = new Map(
      productsList.map((p) => [
        p.id,
        {
          mrpPaise: p.mrpPaise,
          sellingPricePaise: p.sellingPricePaise,
          gstRate: Number(p.gstRate),
          isTaxInclusive: p.isTaxInclusive
        }
      ])
    );

    const billing = calculateBilling(
      productPriceMap,
      command.items,
      command.isInterstate,
      command.discountPaise
    );

    // 4a. Payment validation: only credit sales may be received for less than
    // the bill total; everything else must settle in full.
    if (command.paymentMethod !== "CREDIT" && command.amountReceivedPaise < billing.grandTotalPaise) {
      throw new SyncConflictError("UNDERPAYMENT", "Payment received is less than the bill total");
    }

    // 4b. Credit sales must name an existing customer of this store and stay
    // within the customer's credit limit.
    if (command.paymentMethod === "CREDIT") {
      if (!command.customerId) {
        throw new SyncConflictError("CUSTOMER_REQUIRED", "Credit sale requires a customer");
      }
      const customer = await tx.customer.findFirst({
        where: { id: command.customerId, storeId }
      });
      if (!customer) {
        throw new SyncConflictError("CUSTOMER_NOT_FOUND", "Customer not found in this store");
      }
      const projected = customer.currentBalancePaise + billing.grandTotalPaise;
      if (projected > customer.creditLimitPaise) {
        throw new SyncConflictError(
          "CREDIT_LIMIT_EXCEEDED",
          `Credit limit exceeded for ${customer.name}`
        );
      }
    }

    // 5. Generate Sequential Official Invoice Number
    const count = await tx.invoice.count({ where: { storeId } });
    const year = new Date().getFullYear();
    const officialInvoiceNumber = `INV-${year}-${String(count + 1).padStart(6, "0")}`;

    // 6. Create Invoice & Invoice Items
    const invoice = await tx.invoice.create({
      data: {
        storeId,
        installationId: command.installationId,
        localTransactionId: command.localTransactionId,
        officialInvoiceNumber,
        customerId: command.customerId,
        subtotalPaise: billing.subtotalPaise,
        discountPaise: command.discountPaise,
        taxableValuePaise: billing.taxableValuePaise,
        cgstPaise: billing.cgstPaise,
        sgstPaise: billing.sgstPaise,
        igstPaise: billing.igstPaise,
        grandTotalPaise: billing.grandTotalPaise,
        paymentMethod: command.paymentMethod,
        amountReceivedPaise: command.amountReceivedPaise,
        changeDuePaise:
          command.amountReceivedPaise > billing.grandTotalPaise
            ? command.amountReceivedPaise - billing.grandTotalPaise
            : 0n,
        isInterstate: command.isInterstate,
        items: {
          create: billing.calculatedItems.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            mrpPaise: item.mrpPaise,
            unitPricePaise: item.unitPricePaise,
            gstRate: item.gstRate,
            taxableAmountPaise: item.taxableAmountPaise,
            cgstPaise: item.cgstPaise,
            sgstPaise: item.sgstPaise,
            igstPaise: item.igstPaise,
            lineTotalPaise: item.lineTotalPaise
          }))
        }
      }
    });

    // 7. FEFO Batch Allocation & Atomic Stock Decrement with Versioning
    for (const item of command.items) {
      const prod = productMap.get(item.productId)!;

      // FEFO Batch Deduction
      const batches = await tx.batch.findMany({
        where: { storeId, productId: item.productId, quantity: { gt: 0 } },
        orderBy: { expiryDate: "asc" }
      });

      let remainingToAllocate = item.quantity;
      for (const batch of batches) {
        if (remainingToAllocate <= 0) break;
        const available = Number(batch.quantity);
        const take = Math.min(available, remainingToAllocate);

        await tx.batch.update({
          where: { id: batch.id },
          data: { quantity: { decrement: take }, version: { increment: 1 } }
        });

        remainingToAllocate -= take;
      }

      if (remainingToAllocate > 0) {
        // Validated up front; reaching here means batches changed mid-transaction.
        throw new SyncConflictError(
          "INSUFFICIENT_STOCK",
          `Batch stock changed during sale for product ${prod.name}`
        );
      }

      // Main product stock decrement
      const updatedProd = await tx.product.update({
        where: { id: prod.id },
        data: {
          currentStock: { decrement: item.quantity },
          version: { increment: 1 }
        }
      });

      // Stock Movement Log
      await tx.stockMovement.create({
        data: {
          storeId,
          productId: prod.id,
          invoiceId: invoice.id,
          type: "SALE",
          quantity: -item.quantity,
          balanceAfter: updatedProd.currentStock
        }
      });
    }

    // 8. Customer Credit Ledger Update
    if (command.paymentMethod === "CREDIT" && command.customerId) {
      const updatedCustomer = await tx.customer.update({
        where: { id: command.customerId },
        data: {
          currentBalancePaise: { increment: billing.grandTotalPaise },
          version: { increment: 1 }
        }
      });

      await tx.customerLedger.create({
        data: {
          storeId,
          customerId: command.customerId,
          invoiceId: invoice.id,
          type: "DEBIT",
          amountPaise: billing.grandTotalPaise,
          balanceAfterPaise: updatedCustomer.currentBalancePaise,
          notes: `Credit Sale Invoice ${officialInvoiceNumber}`
        }
      });
    }

    // 9. Log Command Execution
    await tx.syncCommandLog.create({
      data: {
        storeId,
        installationId: command.installationId,
        localTransactionId: command.localTransactionId,
        commandType: "SALE",
        idempotencyKey,
        status: "SUCCESS"
      }
    });

    return {
      status: "SUCCESS",
      officialInvoiceNumber,
      invoiceId: invoice.id,
      grandTotalPaise: billing.grandTotalPaise
    };
  });
}

export async function processCustomerPaymentCommand(
  prisma: PrismaClient,
  storeId: string,
  command: z.infer<typeof CustomerPaymentCommandSchema>,
  idempotencyKey: string
) {
  return await prisma.$transaction(async (tx) => {
    const existingLog = await tx.syncCommandLog.findUnique({
      where: { idempotencyKey }
    });
    if (existingLog) return { status: "ALREADY_PROCESSED" };

    // Payment must be positive and the customer must belong to this store.
    if (command.amountPaise <= 0n) {
      throw new SyncConflictError("UNDERPAYMENT", "Payment amount must be positive");
    }
    const customer = await tx.customer.findFirst({
      where: { id: command.customerId, storeId }
    });
    if (!customer) {
      throw new SyncConflictError("CUSTOMER_NOT_FOUND", "Customer not found in this store");
    }
    const newBalance = customer.currentBalancePaise - command.amountPaise;
    if (newBalance < 0n) {
      throw new SyncConflictError(
        "UNDERPAYMENT",
        `Payment exceeds outstanding balance by ${-(Number(newBalance))} paise`
      );
    }

    const updatedCustomer = await tx.customer.update({
      where: { id: customer.id },
      data: {
        currentBalancePaise: { decrement: command.amountPaise },
        version: { increment: 1 }
      }
    });

    const ledger = await tx.customerLedger.create({
      data: {
        storeId,
        customerId: customer.id,
        type: "CREDIT",
        amountPaise: command.amountPaise,
        balanceAfterPaise: updatedCustomer.currentBalancePaise,
        notes: command.notes || "Khata Payment Received"
      }
    });

    await tx.syncCommandLog.create({
      data: {
        storeId,
        installationId: command.installationId,
        localTransactionId: command.localTransactionId,
        commandType: "CUSTOMER_PAYMENT",
        idempotencyKey,
        status: "SUCCESS"
      }
    });

    return {
      status: "SUCCESS",
      ledgerId: ledger.id,
      newBalancePaise: updatedCustomer.currentBalancePaise
    };
  });
}

export async function processProductUpsertCommand(
  prisma: PrismaClient,
  storeId: string,
  command: z.infer<typeof ProductUpsertCommandSchema>,
  idempotencyKey: string
) {
  return await prisma.$transaction(async (tx) => {
    const existingLog = await tx.syncCommandLog.findUnique({
      where: { idempotencyKey }
    });
    if (existingLog) return { status: "ALREADY_PROCESSED", upserted: 0 };

    let upserted = 0;
    for (const p of command.products) {
      const data = {
        storeId,
        sku: p.sku,
        barcode: p.barcode ?? null,
        normalizedBarcode: p.normalizedBarcode ?? p.barcode ?? null,
        name: p.name,
        brand: p.brand ?? null,
        category: p.category ?? "General",
        variant: p.variant ?? null,
        hsnCode: p.hsnCode ?? null,
        mrpPaise: p.mrpPaise,
        sellingPricePaise: p.sellingPricePaise,
        purchasePricePaise: p.purchasePricePaise,
        gstRate: p.gstRate,
        isTaxInclusive: p.isTaxInclusive,
        currentStock: p.currentStock
      };
      const existing = await tx.product.findFirst({
        where: { storeId, OR: [{ id: p.id }, { sku: p.sku }] }
      });
      if (existing) {
        await tx.product.update({ where: { id: existing.id }, data });
      } else {
        await tx.product.create({ data: { ...data, id: p.id } });
      }
      upserted++;
    }

    await tx.syncCommandLog.create({
      data: {
        storeId,
        installationId: command.installationId,
        localTransactionId: command.localTransactionId,
        commandType: "STOCK_ADJUSTMENT", // closest CommandType; masters are logged for audit
        idempotencyKey,
        status: "SUCCESS"
      }
    });

    return { status: "SUCCESS", upserted };
  });
}

export async function processCustomerUpsertCommand(
  prisma: PrismaClient,
  storeId: string,
  command: z.infer<typeof CustomerUpsertCommandSchema>,
  idempotencyKey: string
) {
  return await prisma.$transaction(async (tx) => {
    const existingLog = await tx.syncCommandLog.findUnique({
      where: { idempotencyKey }
    });
    if (existingLog) return { status: "ALREADY_PROCESSED", upserted: 0 };

    let upserted = 0;
    for (const c of command.customers) {
      const data = {
        storeId,
        name: c.name,
        phone: c.phone ?? null,
        currentBalancePaise: c.currentBalancePaise,
        creditLimitPaise: c.creditLimitPaise
      };
      const existing = await tx.customer.findFirst({
        where: { storeId, OR: [{ id: c.id }, { phone: c.phone ?? "" }] }
      });
      if (existing) {
        await tx.customer.update({ where: { id: existing.id }, data });
      } else {
        await tx.customer.create({ data: { ...data, id: c.id } });
      }
      upserted++;
    }

    await tx.syncCommandLog.create({
      data: {
        storeId,
        installationId: command.installationId,
        localTransactionId: command.localTransactionId,
        commandType: "CUSTOMER_PAYMENT", // closest CommandType; masters are logged for audit
        idempotencyKey,
        status: "SUCCESS"
      }
    });

    return { status: "SUCCESS", upserted };
  });
}
