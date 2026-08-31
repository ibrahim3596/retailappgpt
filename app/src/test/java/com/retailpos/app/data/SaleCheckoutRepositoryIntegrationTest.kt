package com.retailpos.app.data

import android.content.Context
import androidx.room.Room
import com.retailpos.app.core.payment.ActiveCartStore
import com.retailpos.app.core.payment.PendingPaymentStore
import com.retailpos.app.core.permissions.StaffRole
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SaleCheckoutRepositoryIntegrationTest {
    private lateinit var database: RetailDatabase
    private lateinit var repository: SaleCheckoutRepository
    private val storeId = "test-store"

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        context.getSharedPreferences("retailpos_pending_payment", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("retailpos_active_cart", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveCartStore.configure(context)
        PendingPaymentStore.configure(context)
        PendingPaymentStore.clear()
        database = Room.inMemoryDatabaseBuilder(context, RetailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SaleCheckoutRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        PendingPaymentStore.clear()
        ActiveCartStore.clear()
    }

    @Test
    fun duplicateIdempotencyKeyDoesNotDoubleDeductStockOrCreateSecondSale() = runBlocking {
        seedProduct(stock = 5.0, sellingPrice = 50.0, purchasePrice = 30.0)
        val cart = listOf(CartLine("p1", "Milk", "SKU-1", "pcs", 50.0, 2.0))

        val first = repository.checkout(
            storeId = storeId,
            cart = cart,
            paymentMethod = "CASH",
            idempotencyKey = "idem-1",
            amountTendered = 100.0,
            now = 1_000L,
            staffRole = StaffRole.OWNER
        )
        val second = repository.checkout(
            storeId = storeId,
            cart = cart,
            paymentMethod = "CASH",
            idempotencyKey = "idem-1",
            amountTendered = 100.0,
            now = 2_000L,
            staffRole = StaffRole.OWNER
        )

        assertEquals(first.saleId, second.saleId)
        assertEquals(3.0, database.productDao().getById("p1", storeId)!!.stock, 0.000001)
        assertEquals(1, database.saleDao().getRecentSales(storeId, 10).size)
        val allocations = database.saleDao().getSaleCostAllocations(first.saleId)
        assertEquals(1, allocations.size)
        assertEquals(60.0, allocations.single().totalCost, 0.000001)
    }

    @Test
    fun insufficientStockLeavesSaleAndStockUnchanged() = runBlocking {
        seedProduct(stock = 1.0, sellingPrice = 50.0, purchasePrice = 30.0)
        val cart = listOf(CartLine("p1", "Milk", "SKU-1", "pcs", 50.0, 2.0))

        val failure = runCatching {
            repository.checkout(
                storeId = storeId,
                cart = cart,
                paymentMethod = "CASH",
                idempotencyKey = "idem-stock",
                amountTendered = 100.0,
                now = 1_000L,
                staffRole = StaffRole.OWNER
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(failure!!.message.orEmpty().contains("only 1"))
        assertEquals(1.0, database.productDao().getById("p1", storeId)!!.stock, 0.000001)
        assertTrue(database.saleDao().getRecentSales(storeId, 10).isEmpty())
    }

    @Test
    fun creditLedgerMatchesFinalPayableAfterBillDiscount() = runBlocking {
        seedProduct(stock = 3.0, sellingPrice = 100.0, purchasePrice = 60.0)
        val now = 1_000L
        database.customerDao().upsert(CustomerEntity("c1", storeId, "Asha Stores", "9999999999", createdAt = now, updatedAt = now))
        val cart = listOf(CartLine("p1", "Milk", "SKU-1", "pcs", 100.0, 1.0))

        val result = repository.checkout(
            storeId = storeId,
            cart = cart,
            paymentMethod = "CREDIT",
            idempotencyKey = "idem-credit",
            customerId = "c1",
            billDiscountAmount = 10.0,
            now = now,
            staffRole = StaffRole.OWNER
        )

        val sale = database.saleDao().getSale(storeId, result.saleId)
        val ledger = database.khataDao().findByReference(storeId, "SALE", result.saleId)
        assertNotNull(sale)
        assertNotNull(ledger)
        assertEquals(90.0, sale!!.total, 0.000001)
        assertEquals(sale.total, ledger!!.amount, 0.000001)
        assertEquals(2.0, database.productDao().getById("p1", storeId)!!.stock, 0.000001)
    }

    private suspend fun seedProduct(stock: Double, sellingPrice: Double, purchasePrice: Double) {
        database.productDao().upsert(
            ProductEntity(
                id = "p1",
                storeId = storeId,
                name = "Milk",
                sku = "SKU-1",
                mrp = sellingPrice,
                sellingPrice = sellingPrice,
                purchasePrice = purchasePrice,
                stock = stock,
                unit = "pcs",
                updatedAt = 1L
            )
        )
    }
}
