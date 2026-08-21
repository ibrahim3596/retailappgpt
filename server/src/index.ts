import express from "express";
import cors from "cors";
import { PrismaClient } from "@prisma/client";
import { authenticateToken } from "./middleware/auth.middleware";
import { createAuthRouter } from "./routes/auth.routes";
import { createSyncRouter } from "./routes/sync.routes";
import { config } from "./config";

export const app = express();
const prisma = new PrismaClient();

app.use(cors());
app.use(express.json());

app.get("/health", (_req, res) => {
  res.json({
    status: "OK",
    version: "1.0.0",
    timestamp: new Date().toISOString()
  });
});

app.use("/api/v1/auth", createAuthRouter(prisma));
app.use("/api/v1/sync", authenticateToken, createSyncRouter(prisma));

if (require.main === module) {
  app.listen(config.port, () => {
    console.log(`[POS Server] Running on port ${config.port}`);
  });
}
