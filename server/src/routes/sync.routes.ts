import { Router } from "express";
import type { PrismaClient } from "@prisma/client";
import { z } from "zod";
import type { AuthenticatedRequest} from "../middleware/auth.middleware";
import { authenticateToken } from "../middleware/auth.middleware";
import {
  SaleCommandSchema,
  CustomerPaymentCommandSchema,
  ProductUpsertCommandSchema,
  CustomerUpsertCommandSchema,
  SyncPullRequestSchema
} from "../contracts/schemas";
import {
  processSaleCommand,
  processCustomerPaymentCommand,
  processProductUpsertCommand,
  processCustomerUpsertCommand,
  SyncConflictError
} from "../services/sync.service";
import { serializeBigInt } from "../services/billing.service";
import rateLimit from "express-rate-limit";

/** Upper bound on items in one pushed sale; keeps request bodies bounded. */
const MAX_SALE_ITEMS = 500;

/** Rate limiter for sync endpoints to prevent abuse. */
const syncRateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  limit: 100, // 100 requests per minute per IP
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { error: "RATE_LIMITED", message: "Too many sync requests, try again later" }
});

export function createSyncRouter(prisma: PrismaClient) {
  const router = Router();

  // Apply authentication middleware to all sync routes
  router.use(authenticateToken);
  // Apply rate limiting
  router.use(syncRateLimiter);

  // Command Push Endpoint
  router.post("/push", async (req: AuthenticatedRequest, res) => {
    try {
      // Store scoping comes exclusively from the verified JWT: a client can
      // never address another store's data by supplying IDs.
      const storeId = req.user!.storeId;
      const idempotencyKey = (req.headers["x-idempotency-key"] as string) || req.body.idempotencyKey;

      if (!idempotencyKey || typeof idempotencyKey !== "string") {
        return res.status(400).json({ error: "MISSING_IDEMPOTENCY_KEY", message: "X-Idempotency-Key header is required" });
      }

      const commandType = req.body.commandType;

      if (commandType === "SALE") {
        const payload = SaleCommandSchema.parse(req.body.payload);
        if (payload.items.length > MAX_SALE_ITEMS) {
          return res.status(400).json({ error: "TOO_MANY_ITEMS", message: `Maximum ${MAX_SALE_ITEMS} items per sale` });
        }
        const result = await processSaleCommand(prisma, storeId, payload, idempotencyKey);
        return res.json(serializeBigInt(result));
      } else if (commandType === "CUSTOMER_PAYMENT") {
        const payload = CustomerPaymentCommandSchema.parse(req.body.payload);
        const result = await processCustomerPaymentCommand(prisma, storeId, payload, idempotencyKey);
        return res.json(serializeBigInt(result));
      } else if (commandType === "PRODUCT_UPSERT") {
        const payload = ProductUpsertCommandSchema.parse(req.body.payload);
        const result = await processProductUpsertCommand(prisma, storeId, payload, idempotencyKey);
        return res.json(serializeBigInt(result));
      } else if (commandType === "CUSTOMER_UPSERT") {
        const payload = CustomerUpsertCommandSchema.parse(req.body.payload);
        const result = await processCustomerUpsertCommand(prisma, storeId, payload, idempotencyKey);
        return res.json(serializeBigInt(result));
      } else {
        return res.status(400).json({ error: "UNSUPPORTED_COMMAND", message: `Command ${commandType} is not supported` });
      }
    } catch (err) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({
          error: "VALIDATION_FAILED",
          message: "Command payload failed validation",
          issues: err.issues.map((i) => ({ path: i.path.join("."), code: i.code }))
        });
      }
      if (err instanceof SyncConflictError) {
        return res.status(409).json({ error: err.code, message: err.message });
      }
      console.error("[POS Server] push failed:", err);
      return res.status(500).json({ error: "COMMAND_PROCESSING_FAILED", message: "Command could not be processed" });
    }
  });

  // Pull Incremental Updates Endpoint
  router.get("/pull", async (req: AuthenticatedRequest, res) => {
    try {
      const storeId = req.user!.storeId;
      const query = SyncPullRequestSchema.parse(req.query);

      const since = query.lastSyncedAt ? new Date(query.lastSyncedAt) : new Date(0);

      // Bounded pages: an unbounded query on a store with a large catalog is a
      // self-inflicted DoS once pulled from a phone.
      const PAGE_LIMIT = 1000;

      const products = await prisma.product.findMany({
        where: { storeId, updatedAt: { gt: since } },
        include: { batches: true },
        orderBy: { updatedAt: "asc" },
        take: PAGE_LIMIT
      });

      const customers = await prisma.customer.findMany({
        where: { storeId, updatedAt: { gt: since } },
        orderBy: { updatedAt: "asc" },
        take: PAGE_LIMIT
      });

      res.json(
        serializeBigInt({
          products,
          customers,
          hasMore: products.length === PAGE_LIMIT || customers.length === PAGE_LIMIT,
          pulledAt: new Date().toISOString()
        })
      );
    } catch (err) {
      if (err instanceof z.ZodError) {
        return res.status(400).json({ error: "VALIDATION_FAILED", message: "Invalid pull request" });
      }
      console.error("[POS Server] pull failed:", err);
      return res.status(500).json({ error: "PULL_FAILED", message: "Pull could not be completed" });
    }
  });

  return router;
}
