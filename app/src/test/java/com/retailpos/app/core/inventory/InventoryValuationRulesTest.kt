package com.retailpos.app.core.inventory

import com.retailpos.app.data.InventoryBatchEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryValuationRulesTest {
    @Test
    fun `summarizes value and expiry buckets`() {
        val now = 1_000_000L
        val near = now + 10L * ExpiryPolicy.DAY_MS
        val expired = now - 1L
        val batches = listOf(
            InventoryBatchEntity("a", "s", "p", "A", null, 10.0, 20.0, now),
            InventoryBatchEntity("b", "s", "p", "B", near, 5.0, 30.0, now),
            InventoryBatchEntity("c", "s", "p", "C", expired, 2.0, 40.0, now),
            InventoryBatchEntity("d", "s", "p", "D", now + 90L * ExpiryPolicy.DAY_MS, 3.0, 50.0, now)
        )

        val result = InventoryValuationRules.summarize(batches, now)

        assertEquals(20.0, result.totalBatchQuantity, 0.0001)
        assertEquals(580.0, result.totalCostValue, 0.0001)
        assertEquals(18.0, result.sellableQuantity, 0.0001)
        assertEquals(500.0, result.sellableCostValue, 0.0001)
        assertEquals(5.0, result.nearExpiryQuantity, 0.0001)
        assertEquals(150.0, result.nearExpiryCostValue, 0.0001)
        assertEquals(2.0, result.expiredQuantity, 0.0001)
        assertEquals(80.0, result.expiredCostValue, 0.0001)
    }
}
