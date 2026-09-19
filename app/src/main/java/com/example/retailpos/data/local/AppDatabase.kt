package com.example.retailpos.data.local

import android.content.Context
import androidx.room.*
import com.example.retailpos.data.local.dao.*
import com.example.retailpos.data.local.entity.*

class RoomConverters {
    @TypeConverter
    fun fromVerificationStatus(status: VerificationStatus): String = status.name

    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus = try {
        VerificationStatus.valueOf(value)
    } catch (e: Exception) {
        VerificationStatus.UNKNOWN
    }

    @TypeConverter
    fun fromTaxType(taxType: TaxType): String = taxType.name

    @TypeConverter
    fun toTaxType(value: String): TaxType = try {
        TaxType.valueOf(value)
    } catch (e: Exception) {
        TaxType.INCLUSIVE
    }

    @TypeConverter
    fun fromStockMovementType(type: StockMovementType): String = type.name

    @TypeConverter
    fun toStockMovementType(value: String): StockMovementType = try {
        StockMovementType.valueOf(value)
    } catch (e: Exception) {
        StockMovementType.SALE
    }

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = try {
        SyncStatus.valueOf(value)
    } catch (e: Exception) {
        SyncStatus.PENDING
    }

    @TypeConverter
    fun fromLedgerEntryType(type: LedgerEntryType): String = type.name

    @TypeConverter
    fun toLedgerEntryType(value: String): LedgerEntryType = try {
        LedgerEntryType.valueOf(value)
    } catch (e: Exception) {
        LedgerEntryType.DEBIT
    }

    @TypeConverter
    fun fromProvenanceSource(source: ProvenanceSource): String = source.name

    @TypeConverter
    fun toProvenanceSource(value: String): ProvenanceSource = try {
        ProvenanceSource.valueOf(value)
    } catch (e: Exception) {
        ProvenanceSource.MANUAL
    }
}

@Database(
    entities = [
        StoreEntity::class,
        UserEntity::class,
        ProductEntity::class,
        BatchEntity::class,
        StockMovementEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        CustomerEntity::class,
        CreditLedgerEntryEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        SyncLogEntity::class,
        SyncCommandEntity::class,
        SyncConflictEntity::class,
        ProductProvenanceEntity::class,
        ProductProvenanceHistoryEntity::class,
        KhataPaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun batchDao(): BatchDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun creditLedgerDao(): CreditLedgerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun syncDao(): SyncDao
    abstract fun provenanceDao(): ProvenanceDao
    abstract fun khataPaymentDao(): KhataPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN supabaseUserId TEXT")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `khata_payment_queue` (
                        `id` TEXT NOT NULL,
                        `storeId` TEXT NOT NULL,
                        `customerId` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `syncStatus` TEXT NOT NULL DEFAULT 'PENDING',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `khata_payment_queue` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `suppliers` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `batches` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `purchases` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "retailpos_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
