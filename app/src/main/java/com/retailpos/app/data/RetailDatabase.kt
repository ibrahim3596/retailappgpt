package com.retailpos.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        ProductBarcodeEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        InventoryMovementEntity::class,
        InventoryBatchEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class RetailDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productBarcodeDao(): ProductBarcodeDao
    abstract fun saleDao(): SaleDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: RetailDatabase? = null

        fun get(context: Context): RetailDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RetailDatabase::class.java,
                    "retailpos.db"
                )
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_1_2,
                        DatabaseMigrations.MIGRATION_2_3,
                        DatabaseMigrations.MIGRATION_3_4,
                        DatabaseMigrations.MIGRATION_4_5
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
