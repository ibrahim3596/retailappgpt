import { Router } from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import crypto from "crypto";
import { PrismaClient } from "@prisma/client";
import { LoginRequestSchema, RefreshTokenRequestSchema } from "../contracts/schemas";
import { getJwtSecret } from "../config";

export function createAuthRouter(prisma: PrismaClient) {
  const router = Router();

  router.post("/login", async (req, res) => {
    try {
      const body = LoginRequestSchema.parse(req.body);

      const user = await prisma.user.findUnique({
        where: { storeId_username: { storeId: body.storeId, username: body.username } }
      });

      if (!user) {
        return res.status(401).json({ error: "INVALID_CREDENTIALS", message: "Invalid credentials" });
      }

      const isMatch = await bcrypt.compare(body.password, user.passwordHash);
      if (!isMatch) {
        return res.status(401).json({ error: "INVALID_CREDENTIALS", message: "Invalid credentials" });
      }

      const accessToken = jwt.sign(
        { userId: user.id, storeId: user.storeId, username: user.username, role: user.role },
        getJwtSecret(),
        { expiresIn: "12h" }
      );

      const rawRefreshToken = crypto.randomBytes(32).toString("hex");
      const refreshTokenHash = crypto.createHash("sha256").update(rawRefreshToken).digest("hex");

      await prisma.refreshToken.create({
        data: {
          userId: user.id,
          tokenHash: refreshTokenHash,
          installationId: body.installationId,
          expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
        }
      });

      return res.json({
        accessToken,
        refreshToken: rawRefreshToken,
        storeId: user.storeId,
        username: user.username,
        role: user.role
      });
    } catch (err: any) {
      return res.status(400).json({ error: "BAD_REQUEST", message: err.message });
    }
  });

  router.post("/refresh", async (req, res) => {
    try {
      const body = RefreshTokenRequestSchema.parse(req.body);
      const tokenHash = crypto.createHash("sha256").update(body.refreshToken).digest("hex");

      const existingToken = await prisma.refreshToken.findUnique({
        where: { tokenHash },
        include: { user: true }
      });

      if (!existingToken || existingToken.revoked || existingToken.expiresAt < new Date()) {
        return res.status(401).json({ error: "INVALID_REFRESH_TOKEN", message: "Refresh token invalid or expired" });
      }

      const accessToken = jwt.sign(
        {
          userId: existingToken.user.id,
          storeId: existingToken.user.storeId,
          username: existingToken.user.username,
          role: existingToken.user.role
        },
        getJwtSecret(),
        { expiresIn: "12h" }
      );

      return res.json({ accessToken });
    } catch (err: any) {
      return res.status(400).json({ error: "BAD_REQUEST", message: err.message });
    }
  });

  return router;
}
