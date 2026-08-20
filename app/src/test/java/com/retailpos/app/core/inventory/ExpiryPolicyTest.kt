package com.retailpos.app.core.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryPolicyTest {
    @Test fun warningWindowUsesNonNegativeDays() {
        assertEquals(30L * ExpiryPolicy.DAY_MS, ExpiryPolicy.warningWindowMs())
        assertEquals(0L, ExpiryPolicy.warningWindowMs(-4))
    }

    @Test fun expiredItemsAreExcludedFromSale() {
        assertTrue(ExpiryPolicy.shouldExcludeFromSale(99L, 100L))
        assertTrue(ExpiryPolicy.shouldExcludeFromSale(100L, 100L))
        assertFalse(ExpiryPolicy.shouldExcludeFromSale(101L, 100L))
        assertFalse(ExpiryPolicy.shouldExcludeFromSale(null, 100L))
    }
}
