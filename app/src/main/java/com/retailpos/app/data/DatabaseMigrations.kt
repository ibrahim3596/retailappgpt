package com.retailpos.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS product_barcodes (
                    id TEXT NOT NULL,
                    productId TEXT NOT NULL,
                    storeId TEXT NOT NULL,
                    value TEXT NOT NULL,
                    type TEXT NOT NULL,
                    isPrimary INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_product_barcodes_storeId_value " +
                    "ON product_barcodes(storeId, value)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_product_barcodes_productId " +
                    "ON product_barcodes(productId)"
            )
            db.query(
                "SELECT id, storeId, barcode, updatedAt FROM products " +
                    "WHERE barcode IS NOT NULL AND TRIM(barcode) <> ''"
            ).use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val storeIdIndex = cursor.getColumnIndexOrThrow("storeId")
                val barcodeIndex = cursor.getColumnIndexOrThrow("barcode")
                val updatedAtIndex = cursor.getColumnIndexOrThrow("updatedAt")
                while (cursor.moveToNext()) {
                    val barcode = cursor.getString(barcodeIndex).trim()
                    db.execSQL(
                        "INSERT INTO product_barcodes " +
                            "(id, productId, storeId, value, type, isPrimary, createdAt) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(
                            UUID.randomUUID().toString(),
                            cursor.getString(idIndex),
                            cursor.getString(storeIdIndex),
                            barcode,
                            "UNKNOWN",
                            1,
                            cursor.getLong(updatedAtIndex)
                        )
                    )
                }
            }
        }
    }
}
