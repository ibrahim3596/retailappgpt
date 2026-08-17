package com.retailpos.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductEntity::class, ProductBarcodeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RetailDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productBarcodeDao(): ProductBarcodeDao

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
                    .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
