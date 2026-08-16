import { PrismaClient } from "@prisma/client";
import { z } from "zod";
import { SaleCommandSchema, CustomerPaymentCommandSchema } from "../contracts/schemas";
import { calculateBilling } from "./billing.service";

export async function processSaleCommand(
  prisma: PrismaClient,
  storeId: string,
  command: z.infer<typeof SaleCommandSchema>,
  idempotencyKey: string
) {
  return await prisma.$transaction(async (tx) => {
    const existingLog = await tx.syncCommandLog.findUnique({ where: { idempotencyKey } });
    if (existingLog) return { status: "ALREADY_PROCESSED", message: "Command already applied" };

    const productIds = [...new Set(command.items.map((i) => i.productId))];
    const productsList = await tx.product.findMany({ where: { storeId, id: { in: productIds } } });
    if (productsList.length !== productIds.length) throw new Error("One or more products do not belong to this store");

    const productMap = new Map(productsList.map((p) => [p.id, p]));
    const productPriceMap = new Map(productsList.map((p) => [p.id, {
      mrpPaise: p.mrpPaise,
      sellingPricePaise: p.sellingPricePaise,
      purchasePricePaise: p.purchasePricePaise,
      gstRate: Number(p.gstRate),
      isTaxInclusive: p.isTaxInclusive
    }]));

    const billing = calculateBilling(productPriceMap, command.items, command.isInterstate, command.discountPaise);

    if (command.amountReceivedPaise < 0n) throw new Error("Amount received cannot be negative");
    if (command.paymentMethod === "CREDIT" && !command.customerId) throw new Error("Customer is required for credit sales");

    const now = new Date();
    const yearMonth = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, "0")}`;
    const sequence = await tx.storeInvoiceSequence.upsert({
      where: { storeId_yearMonth: { storeId, yearMonth } },
      create: { storeId, yearMonth, lastSequence: 1 },
      update: { lastSequence: { increment: 1 } }
    });
    const officialInvoiceNumber = `INV-${yearMonth}-${String(sequence.lastSequence).padStart(6, "0")}`;

    // Lock the product rows by updating them only when sufficient stock exists.
    // PostgreSQL's row-level locking makes concurrent sales serialize safely here.
    for (const item of command.items) {
      const updated = await tx.product.updateMany({
        where: {
          id: item.productId,
          storeId,
          currentStock: { gte: item.quantity }
        },
        data: { currentStock: { decrement: item.quantity }, version: { increment: 1 } }
      });
      if (updated.count !== 1) throw new Error(`Insufficient stock for product ${item.productId}`);
    }

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
        changeDuePaise: command.amountReceivedPaise > billing.grandTotalPaise ? command.amountReceivedPaise - billing.grandTotalPaise : 0n,
        isInterstate: command.isInterstate,
        items: {
          create: billing.calculatedItems.map((item) => ({
            productId: item.productId,
            quantity: item.quantity,
            mrpPaise: item.mrpPaise,
            unitPricePaise: item.unitPricePaise,
            purchasePricePaise: item.purchasePricePaise,
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

    for (const item of command.items) {
      const batches = await tx.batch.findMany({
        where: { storeId, productId: item.productId, quantity: { gt: 0 } },
        orderBy: [{ expiryDate: "asc" }, { createdAt: "asc" }]
      });
      let remainingToAllocate = item.quantity;
      for (const batch of batches) {
        if (remainingToAllocate <= 0) break;
        const available = Number(batch.quantity);
        const take = Math.min(available, remainingToAllocate);
        if (take <= 0) continue;
        await tx.batch.update({ where: { id: batch.id }, data: { quantity: { decrement: take }, version: { increment: 1 } } });
        await tx.stockMovement.create({
          data: {
            storeId,
            productId: item.productId,
            batchId: batch.id,
            invoiceId: invoice.id,
            type: "SALE",
            quantity: -take,
            balanceAfter: batch.quantity.minus(take)
          }
        });
        remainingToAllocate -= take;
      }
      if (remainingToAllocate > 0) throw new Error(`Batch stock is inconsistent for product ${item.productId}`);

      const currentProduct = await tx.product.findUniqueOrThrow({ where: { id: item.productId } });
      await tx.stockMovement.create({
        data: {
          storeId,
          productId: item.productId,
          invoiceId: invoice.id,
          type: "SALE",
          quantity: -item.quantity,
          balanceAfter: currentProduct.currentStock
        }
      });
    }

    if (command.paymentMethod === "CREDIT") {
      const customer = await tx.customer.findFirst({ where: { id: command.customerId!, storeId } });
      if (!customer) throw new Error("Customer does not belong to this store");
      const newBalance = customer.currentBalancePaise + billing.grandTotalPaise;
      if (newBalance > customer.creditLimitPaise) throw new Error("Customer credit limit exceeded");
      const updatedCustomer = await tx.customer.update({ where: { id: customer.id }, data: { currentBalancePaise: newBalance, version: { increment: 1 } } });
      await tx.customerLedger.create({
        data: {
          storeId,
          customerId: customer.id,
          invoiceId: invoice.id,
          type: "DEBIT",
          amountPaise: billing.grandTotalPaise,
          balanceAfterPaise: updatedCustomer.currentBalancePaise,
          notes: `Credit Sale Invoice ${officialInvoiceNumber}`
        }
      });
    }

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

    return { status: "SUCCESS", officialInvoiceNumber, invoiceId: invoice.id, grandTotalPaise: billing.grandTotalPaise };
  });
}

export async function processCustomerPaymentCommand(
  prisma: PrismaClient,
  storeId: string,
  command: z.infer<typeof CustomerPaymentCommandSchema>,
  idempotencyKey: string
) {
  return await prisma.$transaction(async (tx) => {
    const existingLog = await tx.syncCommandLog.findUnique({ where: { idempotencyKey } });
    if (existingLog) return { status: "ALREADY_PROCESSED" };

    const customer = await tx.customer.findFirst({ where: { id: command.customerId, storeId } });
    if (!customer) throw new Error("Customer does not belong to this store");
    if (command.amountPaise <= 0n) throw new Error("Payment must be greater than zero");

    const newBalance = customer.currentBalancePaise - command.amountPaise;
    if (newBalance < 0n) throw new Error("Payment cannot exceed outstanding customer balance");
    const updatedCustomer = await tx.customer.update({ where: { id: customer.id }, data: { currentBalancePaise: newBalance, version: { increment: 1 } } });

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

    return { status: "SUCCESS", ledgerId: ledger.id, newBalancePaise: updatedCustomer.currentBalancePaise };
  });
}
