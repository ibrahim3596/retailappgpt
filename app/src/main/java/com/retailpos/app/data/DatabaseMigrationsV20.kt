package com.retailpos.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrationsV20 {
    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS sale_cost_allocations (id TEXT NOT NULL, saleId TEXT NOT NULL, saleLineId TEXT NOT NULL, productId TEXT NOT NULL, quantity REAL NOT NULL, unitCost REAL NOT NULL, totalCost REAL NOT NULL, batchId TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_cost_allocations_saleId ON sale_cost_allocations(saleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_cost_allocations_saleLineId ON sale_cost_allocations(saleLineId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_cost_allocations_productId ON sale_cost_allocations(productId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS returns (id TEXT NOT NULL, storeId TEXT NOT NULL, originalSaleId TEXT NOT NULL, refundMethod TEXT NOT NULL, refundAmount REAL NOT NULL, reason TEXT NOT NULL, staffRole TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_returns_storeId_createdAt ON returns(storeId, createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_returns_storeId_originalSaleId ON returns(storeId, originalSaleId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS return_lines (returnId TEXT NOT NULL, saleLineId TEXT NOT NULL, productId TEXT NOT NULL, quantity REAL NOT NULL, refundAmount REAL NOT NULL, restoredCost REAL NOT NULL, PRIMARY KEY(returnId, saleLineId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_return_lines_returnId ON return_lines(returnId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_return_lines_saleLineId ON return_lines(saleLineId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_return_lines_productId ON return_lines(productId)")
        }
    }
}
