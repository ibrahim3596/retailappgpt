package com.retailpos.app.data

import androidx.room.Room
import com.retailpos.app.core.permissions.StaffRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HeldBillRepositoryIntegrationTest {
    private lateinit var database: RetailDatabase
    private lateinit var repository: HeldBillRepository
    private val storeId = "held-test-store"

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        database = Room.inMemoryDatabaseBuilder(context, RetailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HeldBillRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun concurrentResumeAllowsExactlyOneSuccessfulClaim() = runBlocking {
        database.productDao().upsert(
            ProductEntity(
                id = "p1",
                storeId = storeId,
                name = "Milk",
                sku = "SKU-1",
                mrp = 50.0,
                sellingPrice = 50.0,
                purchasePrice = 30.0,
                stock = 2.0,
                unit = "pcs",
                updatedAt = 1L
            )
        )
        val heldBillId = repository.hold(
            storeId,
            listOf(CartLine("p1", "Milk", "SKU-1", "pcs", 50.0, 1.0))
        )

        val results = listOf(
            async(Dispatchers.IO) { repository.takeForResume(storeId, heldBillId) },
            async(Dispatchers.IO) { repository.takeForResume(storeId, heldBillId) }
        ).awaitAll()

        assertEquals(1, results.count { it != null })
        assertEquals(1, results.count { it == null })
        assertNotNull(results.firstOrNull { it != null })
        assertNull(repository.takeForResume(storeId, heldBillId))
        assertEquals(0, repository.list(storeId).size)
    }

    @Test
    fun resumeRejectsHeldQuantityAboveCurrentStockAndKeepsBill() = runBlocking {
        database.productDao().upsert(
            ProductEntity(
                id = "p2",
                storeId = storeId,
                name = "Rice",
                sku = "SKU-2",
                mrp = 80.0,
                sellingPrice = 80.0,
                purchasePrice = 60.0,
                stock = 1.0,
                unit = "kg",
                updatedAt = 1L
            )
        )
        val heldBillId = repository.hold(
            storeId,
            listOf(CartLine("p2", "Rice", "SKU-2", "kg", 80.0, 2.0))
        )

        val failure = runCatching { repository.takeForResume(storeId, heldBillId) }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(1, repository.list(storeId).size)
        assertEquals(1.0, database.productDao().getById("p2", storeId)!!.stock, 0.000001)
    }
}
