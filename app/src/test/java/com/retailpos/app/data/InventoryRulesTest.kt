package com.retailpos.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryRulesTest {
    @Test
    fun saleMovementIsNegative() {
        val movement = InventoryMovementEntity(
            id = "m1",
            storeId = "store",
            productId = "product",
            quantityDelta = -2.0,
            reason = InventoryMovementReason.SALE.name,
            referenceType = "SALE",
            referenceId = "sale",
            createdAt = 1L
        )
        assertEquals(-2.0, movement.quantityDelta, 0.0)
        assertEquals("SALE", movement.reason)
    }

    @Test
    fun fefoOrderingComparatorMatchesExpiryThenCreation() {
        val batches = listOf(
            InventoryBatchEntity("2", "s", "p", "B2", 200L, 5.0, 10.0, 20L),
            InventoryBatchEntity("1", "s", "p", "B1", 100L, 5.0, 11.0, 10L),
            InventoryBatchEntity("3", "s", "p", "B3", null, 5.0, 9.0, 5L)
        )
        val ordered = batches.sortedWith(
            compareBy<InventoryBatchEntity> { it.expiryDate == null }
                .thenBy { it.expiryDate ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }
        )
        assertEquals(listOf("1", "2", "3"), ordered.map { it.id })
    }
}
