import type { Request, Response } from "express";
import { Router } from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import crypto from "crypto";
import rateLimit from "express-rate-limit";
import type { PrismaClient } from "@prisma/client";
import { LoginRequestSchema, RefreshTokenRequestSchema, GoogleLinkRequestSchema, SetupStoreWithGoogleSchema } from "../contracts/schemas";
import { getJwtSecret } from "../middleware/auth.middleware";

const REFRESH_TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000; // 30 days

/** Brute-force protection for credential endpoints. */
const authRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 20,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { error: "RATE_LIMITED", message: "Too many attempts, try again later" }
});

function signAccessToken(user: { id: string; storeId: string; username: string; role: string }) {
  return jwt.sign(
    { userId: user.id, storeId: user.storeId, username: user.username, role: user.role },
    getJwtSecret(),
    { expiresIn: "12h" }
  );
}

export function createAuthRouter(prisma: PrismaClient) {
  const router = Router();

  router.post("/login", authRateLimiter, async (req: Request, res: Response) => {
    const parsed = LoginRequestSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ error: "BAD_REQUEST", message: "Invalid login request" });
    }
    const body = parsed.data;

    try {
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

      // Refresh tokens are hashed at rest and rotated on use.
      const rawRefreshToken = crypto.randomBytes(32).toString("hex");
      const refreshTokenHash = crypto.createHash("sha256").update(rawRefreshToken).digest("hex");

      await prisma.refreshToken.create({
        data: {
          userId: user.id,
          tokenHash: refreshTokenHash,
          installationId: body.installationId,
          expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL_MS)
        }
      });

      res.json({
        accessToken: signAccessToken(user),
        refreshToken: rawRefreshToken,
        storeId: user.storeId,
        username: user.username,
        role: user.role
      });
    } catch {
      res.status(500).json({ error: "INTERNAL_ERROR", message: "Login failed" });
    }
  });

  router.post("/refresh", authRateLimiter, async (req: Request, res: Response) => {
    const parsed = RefreshTokenRequestSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ error: "BAD_REQUEST", message: "Invalid refresh request" });
    }
    const body = parsed.data;
    const tokenHash = crypto.createHash("sha256").update(body.refreshToken).digest("hex");

    try {
      const existingToken = await prisma.refreshToken.findUnique({
        where: { tokenHash },
        include: { user: true }
      });

      if (!existingToken || existingToken.revoked || existingToken.expiresAt < new Date()) {
        return res.status(401).json({ error: "INVALID_REFRESH_TOKEN", message: "Refresh token invalid or expired" });
      }

      // Rotate: the presented refresh token is burned and a new one issued, so a
      // stolen refresh token cannot be replayed indefinitely.
      const rawRefreshToken = crypto.randomBytes(32).toString("hex");
      const newTokenHash = crypto.createHash("sha256").update(rawRefreshToken).digest("hex");

      await prisma.$transaction([
        prisma.refreshToken.update({
          where: { id: existingToken.id },
          data: { revoked: true }
        }),
        prisma.refreshToken.create({
          data: {
            userId: existingToken.userId,
            tokenHash: newTokenHash,
            installationId: body.installationId,
            expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL_MS)
          }
        })
      ]);

      res.json({
        accessToken: signAccessToken(existingToken.user),
        refreshToken: rawRefreshToken
      });
    } catch {
      res.status(500).json({ error: "INTERNAL_ERROR", message: "Token refresh failed" });
    }
  });

  router.post("/logout", async (req: Request, res: Response) => {
    const refreshToken = req.body?.refreshToken;
    if (typeof refreshToken !== "string" || refreshToken.length === 0) {
      return res.status(400).json({ error: "BAD_REQUEST", message: "refreshToken is required" });
    }
    const tokenHash = crypto.createHash("sha256").update(refreshToken).digest("hex");
    try {
      await prisma.refreshToken.updateMany({ where: { tokenHash, revoked: false }, data: { revoked: true } });
      return res.json({ status: "LOGGED_OUT" });
    } catch {
      return res.status(500).json({ error: "INTERNAL_ERROR", message: "Logout failed" });
    }
  });

  /**
   * Link a Google (Supabase) identity to an existing local owner account.
   * The owner must provide their username + PIN for verification.
   * On success, the user's supabaseUserId is updated and new tokens are issued.
   */
  router.post("/link-google", authRateLimiter, async (req: Request, res: Response) => {
    const parsed = GoogleLinkRequestSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ error: "BAD_REQUEST", message: "Invalid link request" });
    }
    const body = parsed.data;

    try {
      const user = await prisma.user.findUnique({
        where: { storeId_username: { storeId: body.storeId, username: body.username } }
      });

      if (!user) {
        return res.status(401).json({ error: "INVALID_CREDENTIALS", message: "Invalid credentials" });
      }

      if (user.role !== "OWNER") {
        return res.status(403).json({ error: "FORBIDDEN", message: "Only owners can link Google accounts" });
      }

      // Check if already linked to a different Google account
      if (user.supabaseUserId && user.supabaseUserId !== body.supabaseUserId) {
        return res.status(409).json({ error: "ALREADY_LINKED", message: "This account is already linked to another Google identity" });
      }

      const isMatch = await bcrypt.compare(body.pin, user.passwordHash);
      if (!isMatch) {
        return res.status(401).json({ error: "INVALID_CREDENTIALS", message: "Invalid PIN" });
      }

      // Link the Supabase user ID
      const updatedUser = await prisma.user.update({
        where: { id: user.id },
        data: { supabaseUserId: body.supabaseUserId }
      });

      // Issue new tokens for the linked account
      const rawRefreshToken = crypto.randomBytes(32).toString("hex");
      const refreshTokenHash = crypto.createHash("sha256").update(rawRefreshToken).digest("hex");

      await prisma.refreshToken.create({
        data: {
          userId: updatedUser.id,
          tokenHash: refreshTokenHash,
          installationId: body.installationId,
          expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL_MS)
        }
      });

      res.json({
        accessToken: signAccessToken(updatedUser),
        refreshToken: rawRefreshToken,
        storeId: updatedUser.storeId,
        username: updatedUser.username,
        role: updatedUser.role
      });
    } catch {
      res.status(500).json({ error: "INTERNAL_ERROR", message: "Link failed" });
    }
  });

  /**
   * Create a new store and owner account linked to a Google (Supabase) identity.
   * This is used when a user signs in with Google for the first time and wants
   * to create a new store instead of linking to an existing one.
   */
  router.post("/setup-store-google", authRateLimiter, async (req: Request, res: Response) => {
    const parsed = SetupStoreWithGoogleSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(400).json({ error: "BAD_REQUEST", message: "Invalid setup request" });
    }
    const body = parsed.data;

    try {
      // Check if this Google account is already linked to any user
      const existingUser = await prisma.user.findUnique({
        where: { supabaseUserId: body.supabaseUserId }
      });
      if (existingUser) {
        return res.status(409).json({ error: "ALREADY_LINKED", message: "This Google account is already linked to an existing store" });
      }

      // Create new store
      const store = await prisma.store.create({
        data: {
          name: body.storeName,
          phone: body.phone
        }
      });

      // Hash the PIN
      const passwordHash = await bcrypt.hash(body.pin, 12);

      // Create owner user linked to Google
      const user = await prisma.user.create({
        data: {
          storeId: store.id,
          username: body.ownerName.toLowerCase().replace(/[^a-z0-9]/g, ""),
          passwordHash,
          fullName: body.ownerName,
          role: "OWNER",
          supabaseUserId: body.supabaseUserId
        }
      });

      // Issue tokens
      const rawRefreshToken = crypto.randomBytes(32).toString("hex");
      const refreshTokenHash = crypto.createHash("sha256").update(rawRefreshToken).digest("hex");

      await prisma.refreshToken.create({
        data: {
          userId: user.id,
          tokenHash: refreshTokenHash,
          installationId: body.installationId,
          expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL_MS)
        }
      });

      res.json({
        accessToken: signAccessToken(user),
        refreshToken: rawRefreshToken,
        storeId: store.id,
        username: user.username,
        role: user.role
      });
    } catch {
      res.status(500).json({ error: "INTERNAL_ERROR", message: "Store setup failed" });
    }
  });

  return router;
}
