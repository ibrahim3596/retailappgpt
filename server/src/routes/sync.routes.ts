import { Router } from "express";
import { PrismaClient } from "@prisma/client";
import { AuthenticatedRequest } from "../middleware/auth.middleware";
import { SaleCommandSchema, CustomerPaymentCommandSchema, SyncPullRequestSchema } from "../contracts/schemas";
import { processSaleCommand, processCustomerPaymentCommand } from "../services/sync.service";
import { serializeBigInt } from "../services/billing.service";

export function createSyncRouter(prisma: PrismaClient) {
  const router = Router();

  // Command Push Endpoint
  router.post("/push", async (req: AuthenticatedRequest, res) => {
    try {
      const storeId = req.user!.storeId;
      const idempotencyKey = (req.headers["x-idempotency-key"] as string) || req.body.idempotencyKey;

      if (!idempotencyKey) {
        return res.status(400).json({ error: "MISSING_IDEMPOTENCY_KEY", message: "X-Idempotency-Key header is required" });
      }

      const commandType = req.body.commandType;

      if (commandType === "SALE") {
        const payload = SaleCommandSchema.parse(req.body.payload);
        const result = await processSaleCommand(prisma, storeId, payload, idempotencyKey);
        return res.json(serializeBigInt(result));
      } else if (commandType === "CUSTOMER_PAYMENT") {
        const payload = CustomerPaymentCommandSchema.parse(req.body.payload);
        const result = await processCustomerPaymentCommand(prisma, storeId, payload, idempotencyKey);
        return res.json(serializeBigInt(result));
      } else {
        return res.status(400).json({ error: "UNSUPPORTED_COMMAND", message: `Command ${commandType} is not supported` });
      }
    } catch (err: any) {
      res.status(400).json({ error: "COMMAND_PROCESSING_FAILED", message: err.message });
    }
  });

  // Pull Incremental Updates Endpoint
  router.get("/pull", async (req: AuthenticatedRequest, res) => {
    try {
      const storeId = req.user!.storeId;
      const query = SyncPullRequestSchema.parse(req.query);

      const since = query.lastSyncedAt ? new Date(query.lastSyncedAt) : new Date(0);

      const products = await prisma.product.findMany({
        where: { storeId, updatedAt: { gt: since } },
        include: { batches: true }
      });

      const customers = await prisma.customer.findMany({
        where: { storeId, updatedAt: { gt: since } }
      });

      res.json(
        serializeBigInt({
          products,
          customers,
          pulledAt: new Date().toISOString()
        })
      );
    } catch (err: any) {
      res.status(400).json({ error: "PULL_FAILED", message: err.message });
    }
  });

  return router;
}
