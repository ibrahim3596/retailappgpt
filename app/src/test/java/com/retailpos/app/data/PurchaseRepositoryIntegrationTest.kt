package com.retailpos.app.data

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PurchaseRepositoryIntegrationTest {
    private lateinit var database: RetailDatabase
    private lateinit var repository: PurchaseRepository
    private val storeId = "test-store"

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        database = Room.inMemoryDatabaseBuilder(context, RetailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PurchaseRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun purchasePersistsStockMovementAndNetSupplierPayableAtomically() = runBlocking {
        seedSupplier()
        seedProduct(stock = 5.0)
        val purchase = purchase(netAmount = 180.0, paidAmount = 80.0, outstandingAmount = 0.0)
        val line = line(netCost = 180.0, effectiveCost = 60.0)

        repository.recordPurchase(purchase, listOf(line), emptyList(), now = 2_000L)

        val storedPurchase = database.purchaseDao().getById("purchase-1", storeId)
        assertNotNull(storedPurchase)
        assertEquals(100.0, storedPurchase!!.outstandingAmount, 0.000001)
        assertEquals(8.0, database.productDao().getById("p1", storeId)!!.stock, 0.000001)
        assertEquals(100.0, database.supplierLedgerDao().balance(storeId, "supplier-1"), 0.000001)

        val entries = database.supplierLedgerDao().entries(storeId, "supplier-1")
        assertEquals(2, entries.size)
        assertEquals(180.0, entries.first { it.type == "PURCHASE" }.amount, 0.000001)
        assertEquals(-80.0, entries.first { it.type == "PAYMENT" }.amount, 0.000001)

        val movements = database.inventoryDao().getProductMovements(storeId, "p1")
        assertEquals(1, movements.size)
        assertEquals(3.0, movements.single().quantityDelta, 0.000001)
        assertEquals("PURCHASE", movements.single().referenceType)
        assertEquals("purchase-1", movements.single().referenceId)
    }

    @Test
    fun inconsistentPurchaseTotalsAreRejectedWithoutMutation() = runBlocking {
        seedSupplier()
        seedProduct(stock = 5.0)
        val invalidPurchase = purchase(netAmount = 181.0, paidAmount = 80.0, outstandingAmount = 101.0)
        val line = line(netCost = 180.0, effectiveCost = 60.0)

        val failure = runCatching {
            repository.recordPurchase(invalidPurchase, listOf(line), emptyList(), now = 2_000L)
        }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(failure is IllegalArgumentException)
        assertNull(database.purchaseDao().getById("purchase-1", storeId))
        assertEquals(5.0, database.productDao().getById("p1", storeId)!!.stock, 0.000001)
        assertEquals(0.0, database.supplierLedgerDao().balance(storeId, "supplier-1"), 0.000001)
        assertTrue(database.inventoryDao().getProductMovements(storeId, "p1").isEmpty())
    }

    @Test
    fun supplierFromAnotherStoreIsRejectedWithoutMutation() = runBlocking {
        database.supplierDao().insert(
            SupplierEntity("supplier-1", "other-store", "Other Supplier", "", "", "", 1L, 1L)
        )
        seedProduct(stock = 5.0)
        val purchase = purchase(netAmount = 180.0, paidAmount = 0.0, outstandingAmount = 180.0)
        val line = line(netCost = 180.0, effectiveCost = 60.0)

        val failure = runCatching {
            repository.recordPurchase(purchase, listOf(line), emptyList(), now = 2_000L)
        }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(failure is IllegalArgumentException)
        assertNull(database.purchaseDao().getById("purchase-1", storeId))
        assertEquals(5.0, database.productDao().getById("p1", storeId)!!.stock, 0.000001)
    }

    private suspend fun seedSupplier() {
        database.supplierDao().insert(
            SupplierEntity("supplier-1", storeId, "Metro Wholesale", "9876543210", "Delhi", "", 1L, 1L)
        )
    }

    private suspend fun seedProduct(stock: Double) {
        database.productDao().upsert(
            ProductEntity(
                id = "p1",
                storeId = storeId,
                name = "Toned Milk",
                sku = "MILK-1",
                mrp = 75.0,
                sellingPrice = 70.0,
                purchasePrice = 55.0,
                stock = stock,
                unit = "pcs",
                updatedAt = 1L
            )
        )
    }

    private fun purchase(netAmount: Double, paidAmount: Double, outstandingAmount: Double) = PurchaseEntity(
        id = "purchase-1",
        storeId = storeId,
        supplierId = "supplier-1",
        invoiceNumber = "INV-101",
        grossAmount = 200.0,
        schemeDiscount = 20.0,
        netAmount = netAmount,
        paidAmount = paidAmount,
        outstandingAmount = outstandingAmount,
        createdAt = 1_000L
    )

    private fun line(netCost: Double, effectiveCost: Double) = PurchaseLineEntity(
        purchaseId = "purchase-1",
        storeId = storeId,
        productId = "p1",
        orderedQuantity = 2.0,
        freeQuantity = 1.0,
        purchaseRate = 100.0,
        schemeDiscount = 20.0,
        netCost = netCost,
        effectiveCost = effectiveCost,
        batchNumber = null,
        expiryDate = null,
        createdAt = 1_000L
    )
}
