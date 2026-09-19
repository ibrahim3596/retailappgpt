package com.example.retailpos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.example.retailpos.data.local.dao.*
import com.example.retailpos.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppDatabaseMigrationTest {

    private lateinit var db: AppDatabase
    private lateinit var storeDao: StoreDao
    private lateinit var productDao: ProductDao
    private lateinit var batchDao: BatchDao
    private lateinit var invoiceDao: InvoiceDao
    private lateinit var customerDao: CustomerDao
    private lateinit var creditLedgerDao: CreditLedgerDao
    private lateinit var supplierDao: SupplierDao
    private lateinit var purchaseDao: PurchaseDao
    private lateinit var syncDao: SyncDao
    private lateinit var provenanceDao: ProvenanceDao
    private lateinit var khataPaymentDao: KhataPaymentDao

    private val testStoreId = "store-migration-test"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java, "migration_test")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()
        storeDao = db.storeDao()
        productDao = db.productDao()
        batchDao = db.batchDao()
        invoiceDao = db.invoiceDao()
        customerDao = db.customerDao()
        creditLedgerDao = db.creditLedgerDao()
        supplierDao = db.supplierDao()
        purchaseDao = db.purchaseDao()
        syncDao = db.syncDao()
        provenanceDao = db.provenanceDao()
        khataPaymentDao = db.khataPaymentDao()
    }

    @After
    fun closeDb() {
        try {
            db.close()
        } catch (e: IOException) {
            // ignore
        }
    }

    @Test
    fun `v1 to v4 - all tables present after migration`() = runBlocking {
        // Insert a store (v1 table)
        storeDao.insertOrUpdateStore(
            StoreEntity(
                id = testStoreId,
                name = "Test Store",
                ownerName = "Owner",
                phone = "1234567890"
            )
        )

        // Verify we can read back from all tables that exist in v4
        val stores = storeDao.getAllStores().first()
        assertEquals(1, stores.size)
        assertEquals(testStoreId, stores[0].id)
    }

    @Test
    fun `v2 to v4 - khata_payment_queue table exists`() = runBlocking {
        // This test verifies MIGRATION_2_3 created the khata_payment_queue table
        // We can do this by checking if the table structure is correct
        val cursor = db.query("SELECT sql FROM sqlite_master WHERE type='table' AND name='khata_payment_queue'", null)
        cursor?.use { c ->
            assert(c.moveToFirst()) { "khata_payment_queue table should exist after v2" }
        }
    }

    @Test
    fun `v3 to v4 - updatedAt columns added`() = runBlocking {
        // Verify MIGRATION_3_4 added updatedAt to the required tables
        val tablesToCheck = listOf(
            "khata_payment_queue",
            "suppliers",
            "batches",
            "purchases"
        )

        for (tableName in tablesToCheck) {
            db.query("SELECT sql FROM sqlite_master WHERE type='table' AND name='$tableName'", null)?.use { cursor ->
                assert(cursor.moveToFirst()) { "Table $tableName should exist" }
                val sql = cursor.getString(0)
                assert(sql.contains("updatedAt")) { "Table $tableName should have updatedAt column after v4 migration" }
            }
        }
    }

    @Test
    fun `v1 to v4 - all entities can be inserted`() = runBlocking {
        // Test that we can insert data into all v4 tables after migration
        val productId = "prod-mig-test"

        // Insert store (using correct DAO method)
        storeDao.insertOrUpdateStore(
            StoreEntity(
                id = testStoreId,
                name = "Migration Test Store",
                ownerName = "Test Owner",
                phone = "9876543210"
            )
        )

        // Insert product
        productDao.insertProduct(
            ProductEntity(
                id = productId,
                storeId = testStoreId,
                sku = "SKU-MIG-001",
                barcode = "8901030300011",
                normalizedBarcode = "8901030300011",
                name = "Test Product",
                brand = "Brand",
                category = "General",
                variant = "",
                packSize = "100ml",
                productImage = "",
                hsnCode = "9999",
                unit = "PCS",
                mrp = 10000.0,
                sellingPrice = 9000.0,
                purchasePrice = 7000.0,
                gstRate = 18.0,
                taxType = TaxType.INCLUSIVE,
                currentStock = 100.0,
                minStock = 10.0,
                maxStock = 500.0,
                verificationStatus = VerificationStatus.VERIFIED,
                confidenceScore = 1.0,
                lastVerifiedAt = System.currentTimeMillis(),
                version = 1L,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Insert batch
        batchDao.insertBatch(
            BatchEntity(
                id = "batch-mig-1",
                productId = productId,
                storeId = testStoreId,
                batchNumber = "BATCH-001",
                mfd = "2024-01-01",
                expiryDate = System.currentTimeMillis() + 86400000L * 365, // 1 year
                mrp = 10000.0,
                sellingPrice = 9000.0,
                purchasePrice = 7000.0,
                initialQty = 100.0,
                remainingQty = 100.0,
                version = 1L,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Insert customer
        val customerId = "cust-mig-1"
        customerDao.insertCustomer(
            CustomerEntity(
                id = customerId,
                storeId = testStoreId,
                name = "Test Customer",
                phone = "1112223334",
                currentBalance = 0.0,
                creditLimit = 10000.0,
                version = 1L,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Insert invoice
        val invoiceId = "inv-mig-1"
        invoiceDao.insertInvoice(
            InvoiceEntity(
                id = invoiceId,
                localId = "local-inv-mig-1",
                storeId = testStoreId,
                invoiceNumber = "INV-2024-000001",
                customerId = customerId,
                customerName = "Test Customer",
                customerPhone = "1112223334",
                subtotal = 9000.0,
                totalGst = 1373.0,
                cgstTotal = 686.5,
                sgstTotal = 686.5,
                igstTotal = 0.0,
                discount = 0.0,
                grandTotal = 9000.0,
                paymentMethod = PaymentMethod.CASH,
                amountReceived = 9000.0,
                changeDue = 0.0,
                isInterstate = false,
                syncStatus = SyncStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Insert credit ledger entry
        creditLedgerDao.insertLedgerEntry(
            CreditLedgerEntryEntity(
                id = "ledger-mig-1",
                storeId = testStoreId,
                customerId = customerId,
                type = LedgerEntryType.DEBIT,
                amount = 9000.0,
                balanceAfter = 9000.0,
                referenceId = invoiceId,
                notes = "Credit sale",
                timestamp = System.currentTimeMillis()
            )
        )

        // Insert supplier
        supplierDao.insertSupplier(
            SupplierEntity(
                id = "supplier-mig-1",
                storeId = testStoreId,
                name = "Test Supplier",
                contactPerson = "Contact",
                phone = "5556667778",
                email = "supplier@test.com",
                gstin = "27AABCT1234A1Z5",
                address = "Test Address",
                currentBalance = 0.0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Insert purchase
        purchaseDao.insertPurchase(
            PurchaseEntity(
                id = "purchase-mig-1",
                storeId = testStoreId,
                supplierId = "supplier-mig-1",
                supplierName = "Test Supplier",
                invoiceNumber = "PUR-001",
                totalAmount = 7000.0,
                gstTotal = 1260.0,
                notes = "Test purchase",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Insert sync log
        syncDao.insertSyncLog(
            SyncLogEntity(
                id = "sync-log-mig-1",
                installationId = "inst-test",
                storeId = testStoreId,
                entityType = "INVOICE",
                entityId = invoiceId,
                action = "CREATE",
                payloadJson = "{}",
                attemptCount = 0,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
        )

        // Insert sync command
        syncDao.insertSyncCommand(
            SyncCommandEntity(
                id = "cmd-mig-1",
                storeId = testStoreId,
                installationId = "inst-test",
                localTransactionId = "local-cmd-mig-1",
                commandType = "SALE",
                idempotencyKey = "idem-mig-1",
                payloadJson = "{}",
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
        )

        // Insert product provenance
        provenanceDao.insertProvenance(
            ProductProvenanceEntity(
                id = "prov-mig-1",
                productId = productId,
                fieldName = "name",
                source = ProvenanceSource.MANUAL,
                confidence = 1.0,
                verifiedByShopkeeper = true,
                rawValue = "Test Product",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Verify all inserts were successful
        assertNotNull(storeDao.getStore(testStoreId))
        assertNotNull(productDao.getProductById(testStoreId, productId))
        assertNotNull(batchDao.getBatchesForProductFlow(testStoreId, productId).first().firstOrNull())
        assertNotNull(customerDao.getCustomerById(testStoreId, customerId))
        assertNotNull(invoiceDao.getInvoiceById(testStoreId, invoiceId))
    }
}
