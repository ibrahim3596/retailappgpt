package com.retailpos.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductEntity::class, ProductBarcodeEntity::class, SaleEntity::class, SaleLineEntity::class, InventoryMovementEntity::class, InventoryBatchEntity::class, CustomerEntity::class, CustomerLedgerEntry::class, ProductIdentificationCacheEntity::class],
    version = 10,
    exportSchema = false
)
abstract class RetailDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productBarcodeDao(): ProductBarcodeDao
    abstract fun saleDao(): SaleDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun customerDao(): CustomerDao
    abstract fun khataDao(): KhataDao
    abstract fun productIdentificationCacheDao(): ProductIdentificationCacheDao

    companion object {
        @Volatile private var INSTANCE: RetailDatabase? = null
        fun get(context: Context): RetailDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, RetailDatabase::class.java, "retailpos.db")
                .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7, DatabaseMigrations.MIGRATION_7_8, DatabaseMigrations.MIGRATION_8_9, DatabaseMigrations.MIGRATION_9_10)
                .build().also { INSTANCE = it }
        }
    }
}
