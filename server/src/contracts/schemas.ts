import { z } from "zod";

// BigInt JSON Helper Transform (Accepts string of digits or numbers, converts safely to BigInt)
export const MoneyPaiseSchema = z.union([z.string(), z.number()]).transform((val) => {
  if (typeof val === "number") return BigInt(Math.round(val));
  return BigInt(val);
});

export const SaleCommandSchema = z.object({
  installationId: z.string().uuid(),
  localTransactionId: z.string().min(1),
  customerId: z.string().uuid().nullable().optional(),
  items: z.array(z.object({
    productId: z.string().uuid(),
    quantity: z.number().positive()
  })).min(1),
  paymentMethod: z.enum(["CASH", "UPI", "CARD", "CREDIT"]),
  amountReceivedPaise: MoneyPaiseSchema,
  discountPaise: MoneyPaiseSchema.default("0"),
  isInterstate: z.boolean().default(false),
  clientTimestamp: z.string().optional()
});

export const CustomerPaymentCommandSchema = z.object({
  installationId: z.string().uuid(),
  localTransactionId: z.string().min(1),
  customerId: z.string().uuid(),
  amountPaise: MoneyPaiseSchema,
  paymentMethod: z.enum(["CASH", "UPI", "CARD"]),
  notes: z.string().optional()
});

export const SyncPullRequestSchema = z.object({
  lastSyncedAt: z.string().optional(),
  installationId: z.string().uuid()
});

export const LoginRequestSchema = z.object({
  storeId: z.string().min(1),
  username: z.string().min(1),
  password: z.string().min(1),
  installationId: z.string().uuid()
});

export const RefreshTokenRequestSchema = z.object({
  refreshToken: z.string().min(1),
  installationId: z.string().uuid()
});
