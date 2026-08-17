package com.retailpos.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS product_barcodes (id TEXT NOT NULL, productId TEXT NOT NULL, storeId TEXT NOT NULL, value TEXT NOT NULL, type TEXT NOT NULL, isPrimary INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_barcodes_storeId_value ON product_barcodes(storeId, value)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_barcodes_productId ON product_barcodes(productId)")
            db.query("SELECT id, storeId, barcode, updatedAt FROM products WHERE barcode IS NOT NULL AND TRIM(barcode) <> ''").use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val storeIdIndex = cursor.getColumnIndexOrThrow("storeId")
                val barcodeIndex = cursor.getColumnIndexOrThrow("barcode")
                val updatedAtIndex = cursor.getColumnIndexOrThrow("updatedAt")
                while (cursor.moveToNext()) {
                    db.execSQL(
                        "INSERT INTO product_barcodes (id, productId, storeId, value, type, isPrimary, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(UUID.randomUUID().toString(), cursor.getString(idIndex), cursor.getString(storeIdIndex), cursor.getString(barcodeIndex).trim(), "UNKNOWN", 1, cursor.getLong(updatedAtIndex))
                    )
                }
            }
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS sales (id TEXT NOT NULL, storeId TEXT NOT NULL, subtotal REAL NOT NULL, total REAL NOT NULL, paymentMethod TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_storeId_createdAt ON sales(storeId, createdAt)")
            db.execSQL("CREATE TABLE IF NOT EXISTS sale_lines (id TEXT NOT NULL, saleId TEXT NOT NULL, productId TEXT NOT NULL, name TEXT NOT NULL, sku TEXT, quantity REAL NOT NULL, unit TEXT NOT NULL, unitPrice REAL NOT NULL, lineTotal REAL NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_lines_saleId ON sale_lines(saleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_lines_productId ON sale_lines(productId)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sales ADD COLUMN idempotencyKey TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE sales SET idempotencyKey = '__legacy__:' || id WHERE idempotencyKey = ''")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sales_storeId_idempotencyKey ON sales(storeId, idempotencyKey)")
        }
    }
}
