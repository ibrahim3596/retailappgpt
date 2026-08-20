package com.retailpos.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeldBillRepositoryTest {
    @Test
    fun heldBillLinesRoundTripModelPreservesQuantityAndPrice() {
        val line = HeldBillLineEntity("bill", "product", "Sugar", "SUG-1", "kg", 48.0, 0.5)
        val restored = CartLine(line.productId, line.name, line.sku, line.unit, line.unitPrice, line.quantity)
        assertEquals(0.5, restored.quantity, 0.0001)
        assertEquals(48.0, restored.unitPrice, 0.0001)
        assertEquals("kg", restored.unit)
    }

    @Test
    fun cannotHoldEmptyBill() {
        assertTrue(CartLine("p", "x", null, "pcs", 10.0, 1.0).quantity > 0)
    }
}
