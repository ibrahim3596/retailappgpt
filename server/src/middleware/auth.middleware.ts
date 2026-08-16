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

const JWT_SECRET = process.env.JWT_SECRET || "RETAIL_POS_SUPER_SECRET_KEY_2026";

export function authenticateToken(req: AuthenticatedRequest, res: Response, next: NextFunction) {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];

  if (!token) {
    return res.status(401).json({ error: "UNAUTHORIZED", message: "Missing authorization token" });
  }

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
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
