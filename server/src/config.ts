import dotenv from "dotenv";

dotenv.config();

const isProduction = process.env.NODE_ENV === "production";

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`${name} is required`);
  }
  return value;
}

export function getJwtSecret(): string {
  const value = process.env.JWT_SECRET?.trim();

  if (!value) {
    if (isProduction) {
      throw new Error("JWT_SECRET is required in production");
    }
    throw new Error("JWT_SECRET is not configured");
  }

  if (isProduction && value.length < 32) {
    throw new Error("JWT_SECRET must be at least 32 characters in production");
  }

  return value;
}

export function getDatabaseUrl(): string {
  return required("DATABASE_URL");
}

export const config = {
  port: Number.parseInt(process.env.PORT ?? "4000", 10),
  nodeEnv: process.env.NODE_ENV ?? "development",
  get jwtSecret(): string {
    return getJwtSecret();
  },
  get databaseUrl(): string {
    return getDatabaseUrl();
  }
};
