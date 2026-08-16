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

    // 3. Server-Authoritative Billing Calculation
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

    // 4. Generate Sequential Official Invoice Number
    const count = await tx.invoice.count({ where: { storeId } });
    const year = new Date().getFullYear();
    const officialInvoiceNumber = `INV-${year}-${String(count + 1).padStart(6, "0")}`;

    // 5. Create Invoice & Invoice Items
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

    // 6. FEFO Batch Allocation & Atomic Stock Decrement with Versioning
    for (const item of command.items) {
      const prod = productMap.get(item.productId);
      if (!prod) continue;

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

    // 7. Customer Credit Ledger Update
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

    // 8. Log Command Execution
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

    const updatedCustomer = await tx.customer.update({
      where: { id: command.customerId },
      data: {
        currentBalancePaise: { decrement: command.amountPaise },
        version: { increment: 1 }
      }
    });

    const ledger = await tx.customerLedger.create({
      data: {
        storeId,
        customerId: command.customerId,
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
