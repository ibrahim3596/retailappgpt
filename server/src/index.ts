import express from "express";
import cors from "cors";
import helmet from "helmet";
import dotenv from "dotenv";
import { PrismaClient } from "@prisma/client";
import { assertJwtSecretConfigured } from "./middleware/auth.middleware";
import { createAuthRouter } from "./routes/auth.routes";
import { createSyncRouter } from "./routes/sync.routes";

dotenv.config();

// Fail fast on an unauthenticated-able deployment rather than serving
// tokens signed with a default secret.
assertJwtSecretConfigured();

const app = express();
const prisma = new PrismaClient();
const PORT = process.env.PORT || 4000;

app.use(helmet());

// CORS: an explicit comma-separated allowlist via CORS_ORIGINS; mobile API
// clients do not need CORS at all, so the default is to send no wildcard.
const corsOrigins = process.env.CORS_ORIGINS
  ? process.env.CORS_ORIGINS.split(",").map((o) => o.trim()).filter(Boolean)
  : [];
if (corsOrigins.length > 0) {
  app.use(cors({ origin: corsOrigins }));
}

app.use(express.json({ limit: "256kb" }));

// Health check endpoint
app.get("/health", (_req, res) => {
  res.json({ status: "OK", timestamp: new Date().toISOString() });
});

// Routes
app.use("/api/v1/auth", createAuthRouter(prisma));
app.use("/api/v1/sync", createSyncRouter(prisma));

const server = app.listen(PORT, () => {
  console.error(`[POS Server] Running on port ${PORT}`);
});

function shutdown(signal: string): void {
  console.error(`[POS Server] ${signal} received, shutting down`);
  server.close(async () => {
    await prisma.$disconnect();
    process.exit(0);
  });
  // Force-exit if connections do not drain in time.
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on("SIGTERM", () => void shutdown("SIGTERM"));
process.on("SIGINT", () => void shutdown("SIGINT"));
