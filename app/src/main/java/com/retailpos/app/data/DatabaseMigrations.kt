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

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS inventory_movements (id TEXT NOT NULL, storeId TEXT NOT NULL, productId TEXT NOT NULL, quantityDelta REAL NOT NULL, reason TEXT NOT NULL, referenceType TEXT, referenceId TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_storeId_productId_createdAt ON inventory_movements(storeId, productId, createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_storeId_referenceType_referenceId ON inventory_movements(storeId, referenceType, referenceId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS inventory_batches (id TEXT NOT NULL, storeId TEXT NOT NULL, productId TEXT NOT NULL, batchNumber TEXT, expiryDate INTEGER, quantity REAL NOT NULL, purchasePrice REAL NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_batches_storeId_productId ON inventory_batches(storeId, productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_batches_storeId_productId_expiryDate ON inventory_batches(storeId, productId, expiryDate)")
            db.query("SELECT id, storeId, stock, updatedAt FROM products WHERE stock > 0").use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val storeIdIndex = cursor.getColumnIndexOrThrow("storeId")
                val stockIndex = cursor.getColumnIndexOrThrow("stock")
                val updatedAtIndex = cursor.getColumnIndexOrThrow("updatedAt")
                while (cursor.moveToNext()) {
                    val batchId = UUID.randomUUID().toString()
                    db.execSQL(
                        "INSERT INTO inventory_movements (id, storeId, productId, quantityDelta, reason, referenceType, referenceId, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(UUID.randomUUID().toString(), cursor.getString(storeIdIndex), cursor.getString(idIndex), cursor.getDouble(stockIndex), "INITIAL_STOCK", "MIGRATION", "4_5", cursor.getLong(updatedAtIndex))
                    )
                    db.execSQL(
                        "INSERT INTO inventory_batches (id, storeId, productId, batchNumber, expiryDate, quantity, purchasePrice, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(batchId, cursor.getString(storeIdIndex), cursor.getString(idIndex), null, null, cursor.getDouble(stockIndex), 0.0, cursor.getLong(updatedAtIndex))
                    )
                }
            }
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE inventory_movements ADD COLUMN batchId TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_batchId ON inventory_movements(batchId)")
        }
    }
}
