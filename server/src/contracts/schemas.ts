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
  notes: z.string().max(500).optional()
});

const MasterProductSchema = z.object({
  id: z.string().uuid(),
  sku: z.string().min(1).max(64),
  barcode: z.string().max(64).nullable().optional(),
  normalizedBarcode: z.string().max(64).nullable().optional(),
  name: z.string().min(1).max(200),
  brand: z.string().max(100).nullable().optional(),
  category: z.string().max(100).optional(),
  variant: z.string().max(100).nullable().optional(),
  hsnCode: z.string().max(16).nullable().optional(),
  unit: z.string().max(16).nullable().optional(),
  mrpPaise: MoneyPaiseSchema,
  sellingPricePaise: MoneyPaiseSchema,
  purchasePricePaise: MoneyPaiseSchema,
  gstRate: z.number().min(0).max(100),
  isTaxInclusive: z.boolean(),
  currentStock: z.number().min(0).default(0),
  minStock: z.number().min(0).optional(),
  updatedAt: z.number().optional()
});

export const ProductUpsertCommandSchema = z.object({
  installationId: z.string().uuid(),
  localTransactionId: z.string().min(1),
  products: z.array(MasterProductSchema).min(1).max(1000)
});

const MasterCustomerSchema = z.object({
  id: z.string().uuid(),
  name: z.string().min(1).max(200),
  phone: z.string().max(20).nullable().optional(),
  currentBalancePaise: MoneyPaiseSchema,
  creditLimitPaise: MoneyPaiseSchema,
  updatedAt: z.number().optional()
});

export const CustomerUpsertCommandSchema = z.object({
  installationId: z.string().uuid(),
  localTransactionId: z.string().min(1),
  customers: z.array(MasterCustomerSchema).min(1).max(1000)
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

export const GoogleLinkRequestSchema = z.object({
  storeId: z.string().min(1),
  username: z.string().min(1),
  pin: z.string().min(1),
  supabaseUserId: z.string().uuid(),
  installationId: z.string().uuid()
});

export const SetupStoreWithGoogleSchema = z.object({
  storeName: z.string().min(1),
  ownerName: z.string().min(1),
  phone: z.string().min(1),
  pin: z.string().min(4).max(6),
  supabaseUserId: z.string().uuid(),
  installationId: z.string().uuid()
});

export const RefreshTokenRequestSchema = z.object({
  refreshToken: z.string().min(1),
  installationId: z.string().uuid()
});
