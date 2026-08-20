package com.retailpos.app.core.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryRulesTest {
    @Test fun invalidAdjustmentRejected() {
        assertEquals("Stock adjustment cannot be zero", InventoryRules.validateAdjustment(0.0))
        assertEquals("Quantity must be finite", InventoryRules.validateAdjustment(Double.NaN))
    }

    @Test fun receiveValidationRejectsPastExpiry() {
        assertEquals("Expiry date cannot be in the past", InventoryRules.validateReceive(1.0, 10.0, 99L, 100L))
        assertNull(InventoryRules.validateReceive(1.0, 10.0, 101L, 100L))
    }

    @Test fun stockStateIsDeterministic() {
        assertEquals(StockState.OUT_OF_STOCK, InventoryRules.stockState(0.0, 5.0))
        assertEquals(StockState.LOW_STOCK, InventoryRules.stockState(3.0, 5.0))
        assertEquals(StockState.HEALTHY, InventoryRules.stockState(8.0, 5.0))
    }

    @Test fun expiryStateIsDeterministic() {
        assertEquals(ExpiryState.NO_EXPIRY, InventoryRules.expiryState(null, 100L, 10L))
        assertEquals(ExpiryState.EXPIRED, InventoryRules.expiryState(99L, 100L, 10L))
        assertEquals(ExpiryState.EXPIRING_SOON, InventoryRules.expiryState(105L, 100L, 10L))
        assertEquals(ExpiryState.OK, InventoryRules.expiryState(200L, 100L, 10L))
    }
}
