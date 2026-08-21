import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";
import { getJwtSecret } from "../config";

export interface AuthenticatedUser {
  userId: string;
  storeId: string;
  username: string;
  role: string;
}

export interface AuthenticatedRequest extends Request {
  user?: AuthenticatedUser;
}

export function authenticateToken(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers["authorization"];
  const [scheme, token] = authHeader?.split(" ") ?? [];

  if (scheme !== "Bearer" || !token) {
    return res.status(401).json({
      error: "UNAUTHORIZED",
      message: "Missing authorization token"
    });
  }

  try {
    const payload = jwt.verify(token, getJwtSecret()) as AuthenticatedUser;

    if (!payload.userId || !payload.storeId || !payload.role) {
      return res.status(403).json({
        error: "FORBIDDEN",
        message: "Invalid token claims"
      });
    }

    req.user = {
      userId: payload.userId,
      storeId: payload.storeId,
      username: payload.username,
      role: payload.role
    };

    return next();
  } catch {
    return res.status(403).json({
      error: "FORBIDDEN",
      message: "Invalid or expired token"
    });
  }
}
