import { describe, it, expect, vi, beforeEach } from "vitest";
import type { PrismaClient } from "@prisma/client";
import {
  processSaleCommand,
  processCustomerPaymentCommand,
  processExpensePushCommand,
  SyncConflictError
} from "./sync.service";
import { calculateBilling } from "./billing.service";

// Mock Prisma
const mockFindUnique = vi.fn();
const mockFindMany = vi.fn();
const mockCreate = vi.fn();
const mockProductUpdate = vi.fn();
const mockCustomerUpdate = vi.fn();
const mockCount = vi.fn();
const mockExpenseUpsert = vi.fn();

const mockPrisma = {
  $transaction: async (fn: any) => fn({
    syncCommandLog: { findUnique: mockFindUnique, create: mockCreate },
    product: { findMany: mockFindMany, update: mockProductUpdate },
    batch: { findMany: mockFindMany, update: mockProductUpdate },
    invoice: { count: mockCount, create: mockCreate },
    stockMovement: { create: mockCreate },
    customer: { findFirst: mockFindUnique, update: mockCustomerUpdate },
    customerLedger: { create: mockCreate },
    expense: { upsert: mockExpenseUpsert },
  }),
} as unknown as PrismaClient;

beforeEach(() => {
  vi.clearAllMocks();
});

const testProduct = {
  id: "prod-1",
  storeId: "store-1",
  name: "Test Product",
  mrpPaise: BigInt(10000),
  sellingPricePaise: BigInt(9000),
  purchasePricePaise: BigInt(7000),
  gstRate: "18.00",
  isTaxInclusive: true,
  currentStock: BigInt(100),
  version: 1,
};

const testBatch = {
  id: "batch-1",
  storeId: "store-1",
  productId: "prod-1",
  batchNumber: "B001",
  quantity: BigInt(50),
  expiryDate: new Date("2027-12-31"),
};

describe("processSaleCommand", () => {
  it("returns ALREADY_PROCESSED for duplicate idempotency key", async () => {
    mockFindUnique.mockResolvedValue({ id: "existing-log" });

    const result = await processSaleCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-1",
        items: [{ productId: "prod-1", quantity: 1 }],
        paymentMethod: "CASH" as any,
        amountReceivedPaise: BigInt(9000),
        discountPaise: BigInt(0),
        isInterstate: false,
      },
      "key-1"
    );

    expect(result.status).toBe("ALREADY_PROCESSED");
  });

  it("throws INSUFFICIENT_STOCK when product not found", async () => {
    mockFindUnique.mockResolvedValue(null);
    mockFindMany.mockResolvedValue([]);

    await expect(
      processSaleCommand(
        mockPrisma as any,
        "store-1",
        {
          installationId: "inst-1",
          localTransactionId: "local-1",
          items: [{ productId: "unknown-prod", quantity: 1 }],
          paymentMethod: "CASH" as any,
          amountReceivedPaise: BigInt(9000),
          discountPaise: BigInt(0),
          isInterstate: false,
        },
        "key-1"
      )
    ).rejects.toThrow(/Unknown product/);
  });

  it("throws CREDIT_LIMIT_EXCEEDED for credit sale beyond limit", async () => {
    mockFindUnique
      .mockResolvedValueOnce(null) // not idempotent duplicate
      .mockResolvedValueOnce({ // customer
        id: "cust-1",
        currentBalancePaise: BigInt(900000),
        creditLimitPaise: BigInt(1000000),
        name: "Test Customer",
      });
    mockFindMany
      .mockResolvedValueOnce([testProduct]) // products
      .mockResolvedValueOnce([testBatch]); // batches
    mockCount.mockResolvedValue(0);
    mockProductUpdate.mockResolvedValue({ ...testProduct, currentStock: BigInt(99) });

    await expect(
      processSaleCommand(
        mockPrisma as any,
        "store-1",
        {
          installationId: "inst-1",
          localTransactionId: "local-1",
          customerId: "cust-1",
          items: [{ productId: "prod-1", quantity: 1 }],
          paymentMethod: "CREDIT" as any,
          amountReceivedPaise: BigInt(0),
          discountPaise: BigInt(0),
          isInterstate: false,
        },
        "key-1"
      )
    ).rejects.toThrow(SyncConflictError);
  });

  it("throws UNDERPAYMENT when cash payment is less than total", async () => {
    mockFindUnique.mockResolvedValue(null);
    mockFindMany.mockResolvedValue([testProduct, testBatch]);
    mockCount.mockResolvedValue(0);

    await expect(
      processSaleCommand(
        mockPrisma as any,
        "store-1",
        {
          installationId: "inst-1",
          localTransactionId: "local-1",
          items: [{ productId: "prod-1", quantity: 1 }],
          paymentMethod: "CASH" as any,
          amountReceivedPaise: BigInt(100), // way below 9000
          discountPaise: BigInt(0),
          isInterstate: false,
        },
        "key-1"
      )
    ).rejects.toThrow(/less than the bill total/);
  });

  it("succeeds for valid cash sale", async () => {
    mockFindUnique.mockResolvedValue(null);
    mockFindMany
      .mockResolvedValueOnce([testProduct]) // products
      .mockResolvedValueOnce([testBatch]); // batches
    mockCount.mockResolvedValue(0);
    mockProductUpdate.mockResolvedValue({ ...testProduct, currentStock: BigInt(99) });
    mockCreate.mockResolvedValue({ id: "inv-1" });

    const result = await processSaleCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-1",
        items: [{ productId: "prod-1", quantity: 1 }],
        paymentMethod: "CASH" as any,
        amountReceivedPaise: BigInt(9000),
        discountPaise: BigInt(0),
        isInterstate: false,
      },
      "key-1"
    );

    expect(result.status).toBe("SUCCESS");
    expect(result.invoiceId).toBe("inv-1");
    expect(mockCreate).toHaveBeenCalled();
  });

  it("creates customer ledger entry for credit sale", async () => {
    mockFindUnique
      .mockResolvedValueOnce(null) // not idempotent duplicate
      .mockResolvedValueOnce({ // customer
        id: "cust-1",
        currentBalancePaise: BigInt(0),
        creditLimitPaise: BigInt(1000000),
        name: "Test Customer",
      });
    mockFindMany
      .mockResolvedValueOnce([testProduct])
      .mockResolvedValueOnce([testBatch]);
    mockCount.mockResolvedValue(0);
    mockProductUpdate
      .mockResolvedValueOnce({ ...testProduct, currentStock: BigInt(99) });
    mockCustomerUpdate
      .mockResolvedValueOnce({ id: "cust-1", currentBalancePaise: BigInt(9000) });
    mockCreate
      .mockResolvedValueOnce({ id: "inv-1" })
      .mockResolvedValueOnce({ id: "log-1" });

    const result = await processSaleCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-1",
        customerId: "cust-1",
        items: [{ productId: "prod-1", quantity: 1 }],
        paymentMethod: "CREDIT" as any,
        amountReceivedPaise: BigInt(0),
        discountPaise: BigInt(0),
        isInterstate: false,
      },
      "key-1"
    );

    expect(result.status).toBe("SUCCESS");
    // Verify customer balance was incremented
    expect(mockCustomerUpdate).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { id: "cust-1" },
        data: expect.objectContaining({
          currentBalancePaise: expect.anything()
        })
      })
    );
  });
});

describe("processCustomerPaymentCommand", () => {
  it("marks duplicate payments idempotent", async () => {
    mockFindUnique.mockResolvedValue({ id: "existing-payment-log" });

    const result = await processCustomerPaymentCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-pay-1",
        customerId: "cust-1",
        amountPaise: 500n,
        paymentMethod: "CASH",
        notes: "Khata payment",
      },
      "key-pay-1"
    );

    expect(result.status).toBe("ALREADY_PROCESSED");
  });

  it("throws UNDERPAYMENT when amount is non-positive", async () => {
    mockFindUnique.mockResolvedValue(null);

    await expect(
      processCustomerPaymentCommand(
        mockPrisma as any,
        "store-1",
        {
          installationId: "inst-1",
          localTransactionId: "local-pay-2",
          customerId: "cust-1",
          amountPaise: 0n,
          paymentMethod: "CASH",
          notes: "Khata payment",
        },
        "key-pay-2"
      )
    ).rejects.toThrow(/positive/i);
  });

  it("throws CUSTOMER_NOT_FOUND when customer missing", async () => {
    mockFindUnique
      .mockResolvedValueOnce(null) // not duplicate
      .mockResolvedValueOnce(null); // customer not found

    await expect(
      processCustomerPaymentCommand(
        mockPrisma as any,
        "store-1",
        {
          installationId: "inst-1",
          localTransactionId: "local-pay-3",
          customerId: "unknown-cust",
          amountPaise: 100n,
          paymentMethod: "UPI",
          notes: "Payment",
        },
        "key-pay-3"
      )
    ).rejects.toThrow(/customer not found/i);
  });

  it("reduces customer balance on valid payment", async () => {
    mockFindUnique
      .mockResolvedValueOnce(null) // not duplicate
      .mockResolvedValueOnce({
        id: "cust-1",
        currentBalancePaise: BigInt(5000),
      });
    mockCustomerUpdate.mockResolvedValue({
      id: "cust-1",
      currentBalancePaise: BigInt(4500),
    });
    mockCreate.mockResolvedValueOnce({ id: "ledger-1" });
    mockCreate.mockResolvedValueOnce({ id: "log-1" });

    const result = await processCustomerPaymentCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-pay-4",
        customerId: "cust-1",
        amountPaise: 500n,
        paymentMethod: "CASH",
        notes: "Khata cash payment",
      },
      "key-pay-4"
    );

    expect(result.status).toBe("SUCCESS");
    expect(result.newBalancePaise).toBe(BigInt(4500));
  });

  it("throws UNDERPAYMENT when payment exceeds balance", async () => {
    mockFindUnique
      .mockResolvedValueOnce(null) // not duplicate
      .mockResolvedValueOnce({
        id: "cust-1",
        currentBalancePaise: BigInt(200),
      });

    await expect(
      processCustomerPaymentCommand(
        mockPrisma as any,
        "store-1",
        {
          installationId: "inst-1",
          localTransactionId: "local-pay-5",
          customerId: "cust-1",
          amountPaise: 500n,
          paymentMethod: "CASH",
          notes: "Overpayment",
        },
        "key-pay-5"
      )
    ).rejects.toThrow(/exceeds outstanding balance/);
  });
});

describe("billing service consistency", () => {
  it("server and client use same GST calculation logic", () => {
    const products = new Map([["p1", {
      mrpPaise: BigInt(10000),
      sellingPricePaise: BigInt(9000),
      gstRate: 18,
      isTaxInclusive: true,
    }]]);

    const result = calculateBilling(products, [{ productId: "p1", quantity: 2 }], false, 0n);

    expect(result.subtotalPaise).toBe(BigInt(18000));
    expect(result.grandTotalPaise).toBe(BigInt(18000));
    expect(result.cgstPaise + result.sgstPaise).toBeGreaterThan(0n);
  });
});

describe("processExpensePushCommand", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns ALREADY_PROCESSED for duplicate idempotency key", async () => {
    mockFindUnique.mockResolvedValue({ id: "existing-expense-log" });

    const result = await processExpensePushCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-exp-1",
        localId: "exp-1",
        category: "RENT",
        amountPaise: 100000n,
        date: "2024-01-15T10:00:00.000Z",
        paymentMethod: "CASH",
        notes: "Shop rent"
      },
      "key-exp-1"
    );

    expect(result.status).toBe("ALREADY_PROCESSED");
  });

  it("throws UNDERPAYMENT when amount is non-positive", async () => {
    mockFindUnique.mockResolvedValue(null);

    await expect(
      processExpensePushCommand(
        mockPrisma as any,
        "store-1",
        {
          installationId: "inst-1",
          localTransactionId: "local-exp-2",
          localId: "exp-2",
          category: "ELECTRICITY",
          amountPaise: 0n,
          date: "2024-01-15T10:00:00.000Z",
          paymentMethod: "UPI"
        },
        "key-exp-2"
      )
    ).rejects.toThrow(/positive/i);
  });

  it("creates expense and sync log on valid push", async () => {
    mockFindUnique.mockResolvedValue(null);
    mockExpenseUpsert.mockResolvedValue({ id: "expense-1" });
    mockCreate.mockResolvedValue({ id: "log-1" });

    const result = await processExpensePushCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-exp-3",
        localId: "exp-3",
        category: "TRANSPORT",
        amountPaise: 50000n,
        date: "2024-01-15T10:00:00.000Z",
        paymentMethod: "CARD",
        notes: "Fuel"
      },
      "key-exp-3"
    );

    expect(result.status).toBe("SUCCESS");
    expect(result.expenseId).toBe("expense-1");
    expect(mockExpenseUpsert).toHaveBeenCalledTimes(1);
    expect(mockCreate).toHaveBeenCalledTimes(1); // syncCommandLog only
  });

  it("upserts on duplicate localId without creating duplicate expense", async () => {
    mockFindUnique.mockResolvedValue(null);
    mockExpenseUpsert.mockResolvedValue({ id: "expense-upsert" });
    mockCreate.mockResolvedValue({ id: "sync-log-upsert" });

    const result = await processExpensePushCommand(
      mockPrisma as any,
      "store-1",
      {
        installationId: "inst-1",
        localTransactionId: "local-exp-4",
        localId: "exp-4",
        category: "SALARIES",
        amountPaise: 200000n,
        date: "2024-01-15T10:00:00.000Z",
        paymentMethod: "CASH"
      },
      "key-exp-4"
    );

    expect(result.status).toBe("SUCCESS");
    expect(result.expenseId).toBe("expense-upsert");
    expect(mockExpenseUpsert).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining({
          storeId_localId: { storeId: "store-1", localId: "exp-4" }
        })
      })
    );
  });
});
