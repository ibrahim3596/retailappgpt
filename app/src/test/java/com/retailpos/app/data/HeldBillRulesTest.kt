package com.retailpos.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HeldBillRulesTest {
    @Test
    fun cartLineSnapshotPreservesDecimalQuantity() {
        val line = CartLine("p", "Sugar", "SUGAR", "kg", 52.0, 0.5)
        val snapshot = HeldBillLineEntity("bill", line.productId, line.name, line.sku, line.unit, line.unitPrice, line.quantity)
        val restored = CartLine(snapshot.productId, snapshot.name, snapshot.sku, snapshot.unit, snapshot.unitPrice, snapshot.quantity)
        assertEquals(0.5, restored.quantity, 0.0001)
    }
}
