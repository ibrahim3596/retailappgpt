package com.retailpos.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrationsV23 {
    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS expenses (id TEXT NOT NULL, storeId TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, paymentMethod TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_storeId_createdAt ON expenses(storeId, createdAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_storeId_category_createdAt ON expenses(storeId, category, createdAt)")
        }
    }
}
