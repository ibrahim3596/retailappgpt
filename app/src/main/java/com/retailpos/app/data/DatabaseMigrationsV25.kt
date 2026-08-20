package com.retailpos.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrationsV25 {
    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE held_bill_lines ADD COLUMN overrideUnitPrice REAL")
            db.execSQL("ALTER TABLE held_bill_lines ADD COLUMN itemDiscountAmount REAL NOT NULL DEFAULT 0.0")
        }
    }
}
