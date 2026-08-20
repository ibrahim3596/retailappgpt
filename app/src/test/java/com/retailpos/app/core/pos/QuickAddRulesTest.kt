package com.retailpos.app.core.pos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddRulesTest {
    @Test
    fun dedupeKeepsNewestFirstOrderAndLimit() {
        val products = listOf(
            QuickAddProduct("a", "Sugar", "", "kg", 50.0, 10.0),
            QuickAddProduct("b", "Oil", "", "l", 120.0, 4.0),
            QuickAddProduct("a", "Sugar", "", "kg", 50.0, 10.0)
        )
        val result = QuickAddRules.dedupeInOrder(products, limit = 2)
        assertEquals(listOf("a", "b"), result.map { it.productId })
    }

    @Test
    fun zeroStockProductsCannotBeAdded() {
        val result = QuickAddRules.filterAddable(
            listOf(
                QuickAddProduct("a", "Sugar", "", "kg", 50.0, 0.0),
                QuickAddProduct("b", "Oil", "", "l", 120.0, 2.0)
            )
        )
        assertEquals(listOf("b"), result.map { it.productId })
        assertFalse(result.first().availableStock <= 0.0)
    }
}

class FavoriteProductStoreTest {
    @Test
    fun toggleAddsAndRemovesFavorite() {
        FavoriteProductStore.clear()
        assertTrue(FavoriteProductStore.toggle("p1"))
        assertTrue(FavoriteProductStore.isFavorite("p1"))
        assertFalse(FavoriteProductStore.toggle("p1"))
        assertFalse(FavoriteProductStore.isFavorite("p1"))
    }
}

class RecentProductRulesTest {
    @Test
    fun recentProductsAreDeduplicatedAcrossSales() {
        val result = RecentProductRules.fromSaleLines(
            listOf(
                listOf(QuickAddProduct("a", "Sugar", "", "kg", 50.0, 2.0)),
                listOf(
                    QuickAddProduct("b", "Oil", "", "l", 120.0, 3.0),
                    QuickAddProduct("a", "Sugar", "", "kg", 50.0, 2.0)
                )
            )
        )
        assertEquals(listOf("a", "b"), result.map { it.productId })
    }
}
