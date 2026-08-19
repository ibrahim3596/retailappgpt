package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMetadataRulesTest {
    @Test
    fun normalizeFieldsCollapsesWhitespace() {
        assertEquals("Snacks & Beverages", ProductMetadataRules.normalizeCategory("  Snacks   &  Beverages  "))
        assertEquals("500", ProductMetadataRules.normalizePackUnit("  500  "))
    }

    @Test
    fun packSizeAllowsMissingValueButRejectsZeroAndNegative() {
        assertTrue(ProductMetadataRules.isValidPackSize(null))
        assertTrue(ProductMetadataRules.isValidPackSize(500.0))
        assertFalse(ProductMetadataRules.isValidPackSize(0.0))
        assertFalse(ProductMetadataRules.isValidPackSize(-1.0))
    }
}
