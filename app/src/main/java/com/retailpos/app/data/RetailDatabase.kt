package com.retailpos.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.retailpos.app.core.payment.ActiveCartStore
import com.retailpos.app.core.payment.PendingPaymentStore

@Database(
    entities = [
        ProductEntity::class,
        ProductBarcodeEntity::class,
        ProductMetadataEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        InventoryMovementEntity::class,
        InventoryBatchEntity::class,
        CustomerEntity::class,
        CustomerLedgerEntry::class,
        ProductIdentificationCacheEntity::class,
        StoreSettingsEntity::class,
        StaffEntity::class,
        ProductIdentificationFeedbackEntity::class,
        HeldBillEntity::class,
        HeldBillLineEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseLineEntity::class,
        SupplierLedgerEntry::class,
        SaleCostAllocationEntity::class,
        ReturnEntity::class,
        ReturnLineEntity::class,
        FavoriteProductEntity::class,
        ExpenseEntity::class
    ],
    version = 25,
    exportSchema = false
)
abstract class RetailDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productBarcodeDao(): ProductBarcodeDao
    abstract fun productMetadataDao(): ProductMetadataDao
    abstract fun saleDao(): SaleDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun customerDao(): CustomerDao
    abstract fun khataDao(): KhataDao
    abstract fun productIdentificationCacheDao(): ProductIdentificationCacheDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun staffDao(): StaffDao
    abstract fun productIdentificationFeedbackDao(): ProductIdentificationFeedbackDao
    abstract fun heldBillDao(): HeldBillDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun supplierLedgerDao(): SupplierLedgerDao
    abstract fun returnDao(): ReturnDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun favoriteProductDao(): FavoriteProductDao

    companion object {
        private const val LOCAL_STORE_ID = "local-store"
        @Volatile private var INSTANCE: RetailDatabase? = null

        fun get(context: Context): RetailDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                RetailDatabase::class.java,
                "retailpos.db"
            )
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2,
                    DatabaseMigrations.MIGRATION_2_3,
                    DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5,
                    DatabaseMigrations.MIGRATION_5_6,
                    DatabaseMigrations.MIGRATION_6_7,
                    DatabaseMigrations.MIGRATION_7_8,
                    DatabaseMigrations.MIGRATION_8_9,
                    DatabaseMigrations.MIGRATION_9_10,
                    DatabaseMigrations.MIGRATION_10_11,
                    DatabaseMigrations.MIGRATION_11_12,
                    DatabaseMigrations.MIGRATION_12_13,
                    DatabaseMigrations.MIGRATION_13_14,
                    DatabaseMigrations.MIGRATION_14_15,
                    DatabaseMigrations.MIGRATION_15_16,
                    DatabaseMigrations.MIGRATION_16_17,
                    DatabaseMigrations.MIGRATION_17_18,
                    DatabaseMigrations.MIGRATION_18_19,
                    DatabaseMigrationsV20.MIGRATION_19_20,
                    DatabaseMigrationsV21.MIGRATION_20_21,
                    DatabaseMigrationsV22.MIGRATION_21_22,
                    DatabaseMigrationsV23.MIGRATION_22_23,
                    DatabaseMigrationsV24.MIGRATION_23_24,
                    DatabaseMigrationsV25.MIGRATION_24_25
                )
                .build()
                .also {
                    INSTANCE = it
                    val applicationContext = context.applicationContext
                    PendingPaymentStore.configure(applicationContext)
                    ActiveCartStore.configure(applicationContext)
                    ProductCatalogLookup.configurePersistentCache(LOCAL_STORE_ID, it.productIdentificationCacheDao())
                }
        }

        fun current(): RetailDatabase = INSTANCE ?: error("RetailDatabase has not been initialized")

        fun closeForRestore() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
