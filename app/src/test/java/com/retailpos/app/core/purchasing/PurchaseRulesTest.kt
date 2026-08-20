package com.retailpos.app.core.purchasing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseRulesTest {
    @Test
    fun freeUnitsIncreaseStockButNotPaidCost() {
        val economics = PurchaseRules.lineEconomics(
            PurchaseLineDraft("p", "Biscuits", orderedQuantity = 10.0, freeQuantity = 1.0, purchaseRate = 100.0)
        )
        assertEquals(11.0, economics.stockQuantity, 0.0001)
        assertEquals(1000.0, economics.netCost, 0.0001)
        assertEquals(90.9090909, economics.effectiveUnitCost, 0.0001)
    }

    @Test
    fun schemeDiscountReducesNetCost() {
        val economics = PurchaseRules.lineEconomics(
            PurchaseLineDraft("p", "Soap", orderedQuantity = 5.0, purchaseRate = 40.0, schemeDiscount = 20.0)
        )
        assertEquals(200.0, economics.grossCost, 0.0001)
        assertEquals(180.0, economics.netCost, 0.0001)
    }

    @Test
    fun expiryRequiresBatchNumber() {
        val errors = PurchaseRules.validateDraft(
            PurchaseDraft("supplier", lines = listOf(PurchaseLineDraft("p", "Milk", 10.0, purchaseRate = 25.0, expiryDate = 100L)))
        )
        assertTrue(errors.any { it.contains("Expiry requires a batch number") })
    }

    @Test
    fun payableTracksSupplierOutstanding() {
        assertEquals(700.0, SupplierPayableRules.balance(1000.0, 300.0), 0.0001)
        assertEquals(SupplierPayableState.SETTLED, SupplierPayableRules.state(0.0))
        assertEquals(SupplierPayableState.OUTSTANDING, SupplierPayableRules.state(700.0))
    }
}
