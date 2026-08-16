import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import { PrismaClient } from "@prisma/client";
import { authenticateToken } from "./middleware/auth.middleware";
import { createAuthRouter } from "./routes/auth.routes";
import { createSyncRouter } from "./routes/sync.routes";

dotenv.config();

const app = express();
const prisma = new PrismaClient();
const PORT = process.env.PORT || 4000;

app.use(cors());
app.use(express.json());

// Health check endpoint
app.get("/health", (req, res) => {
  res.json({ status: "OK", timestamp: new Date().toISOString() });
});

// Routes
app.use("/api/v1/auth", createAuthRouter(prisma));
app.use("/api/v1/sync", authenticateToken as any, createSyncRouter(prisma));

app.listen(PORT, () => {
  console.log(`[POS Server] Running on port ${PORT}`);
});
