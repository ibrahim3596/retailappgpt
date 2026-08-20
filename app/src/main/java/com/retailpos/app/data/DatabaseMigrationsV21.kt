package com.retailpos.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrationsV21 {
    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS favorite_products (storeId TEXT NOT NULL, productId TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(storeId, productId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_products_storeId ON favorite_products(storeId)")
        }
    }
}
