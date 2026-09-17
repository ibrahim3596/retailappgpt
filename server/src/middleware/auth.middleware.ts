import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

export interface AuthenticatedUser {
  userId: string;
  storeId: string;
  username: string;
  role: string;
}

export interface AuthenticatedRequest extends Request {
  user?: AuthenticatedUser;
}

let cachedSecret: string | null = null;

/**
 * Returns the JWT signing secret. There is deliberately NO fallback value:
 * signing tokens with a publicly-known key would let anyone forge an
 * authenticated session. The process refuses to sign/verify without a
 * configured secret.
 */
export function getJwtSecret(): string {
  if (cachedSecret) return cachedSecret;
  const secret = process.env.JWT_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error(
      "JWT_SECRET is not configured (must be at least 32 characters). " +
        "Set it in the environment before starting the server."
    );
  }
  cachedSecret = secret;
  return secret;
}

/** Non-fatal check used at startup so a misconfigured deployment fails fast. */
export function assertJwtSecretConfigured(): void {
  getJwtSecret();
}

export function authenticateToken(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (!token) {
    return res.status(401).json({ error: "UNAUTHORIZED", message: "Missing authorization token" });
  }

  let secret: string;
  try {
    secret = getJwtSecret();
  } catch {
    return res.status(500).json({ error: "SERVER_MISCONFIGURED", message: "Authentication is not configured" });
  }

  jwt.verify(token, secret, (err, decoded) => {
    if (err || !decoded) {
      return res.status(403).json({ error: "FORBIDDEN", message: "Invalid or expired token" });
    }

    const payload = decoded as AuthenticatedUser;
    req.user = {
      userId: payload.userId,
      storeId: payload.storeId,
      username: payload.username,
      role: payload.role
    };

    next();
  });
}
