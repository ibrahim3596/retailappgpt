package com.retailpos.app.core.products

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductListFilterTest {
    @Test
    fun allMatchesActiveProductsOnly() {
        assertTrue(ProductListFilter.ALL.matches(10.0, 5.0, archived = false))
        assertFalse(ProductListFilter.ALL.matches(10.0, 5.0, archived = true))
    }

    @Test
    fun lowStockMatchesPositiveStockAtOrBelowThreshold() {
        assertTrue(ProductListFilter.LOW_STOCK.matches(5.0, 5.0, archived = false))
        assertTrue(ProductListFilter.LOW_STOCK.matches(1.0, 5.0, archived = false))
        assertFalse(ProductListFilter.LOW_STOCK.matches(0.0, 5.0, archived = false))
        assertFalse(ProductListFilter.LOW_STOCK.matches(8.0, 5.0, archived = false))
        assertFalse(ProductListFilter.LOW_STOCK.matches(1.0, 5.0, archived = true))
    }

    @Test
    fun outOfStockMatchesZeroAndNegativeStockForActiveProducts() {
        assertTrue(ProductListFilter.OUT_OF_STOCK.matches(0.0, 5.0, archived = false))
        assertTrue(ProductListFilter.OUT_OF_STOCK.matches(-1.0, 5.0, archived = false))
        assertFalse(ProductListFilter.OUT_OF_STOCK.matches(1.0, 5.0, archived = false))
        assertFalse(ProductListFilter.OUT_OF_STOCK.matches(0.0, 5.0, archived = true))
    }

    @Test
    fun archivedMatchesOnlyArchivedProducts() {
        assertTrue(ProductListFilter.ARCHIVED.matches(0.0, 5.0, archived = true))
        assertFalse(ProductListFilter.ARCHIVED.matches(10.0, 5.0, archived = false))
    }
}
