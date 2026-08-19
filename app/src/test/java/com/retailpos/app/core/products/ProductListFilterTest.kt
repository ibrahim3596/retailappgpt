package com.retailpos.app.core.products

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductListFilterTest {
    @Test
    fun allMatchesEveryProduct() {
        assertTrue(ProductListFilter.ALL.matches(10.0, 5.0))
        assertTrue(ProductListFilter.ALL.matches(0.0, 5.0))
    }

    @Test
    fun lowStockMatchesPositiveStockAtOrBelowThreshold() {
        assertTrue(ProductListFilter.LOW_STOCK.matches(5.0, 5.0))
        assertTrue(ProductListFilter.LOW_STOCK.matches(1.0, 5.0))
        assertFalse(ProductListFilter.LOW_STOCK.matches(0.0, 5.0))
        assertFalse(ProductListFilter.LOW_STOCK.matches(8.0, 5.0))
    }

    @Test
    fun outOfStockMatchesZeroAndNegativeStock() {
        assertTrue(ProductListFilter.OUT_OF_STOCK.matches(0.0, 5.0))
        assertTrue(ProductListFilter.OUT_OF_STOCK.matches(-1.0, 5.0))
        assertFalse(ProductListFilter.OUT_OF_STOCK.matches(1.0, 5.0))
    }
}
