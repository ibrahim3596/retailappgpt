package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveCartStoreTest {
    @Test
    fun cartLineRoundTripShapePreservesPricingFields() {
        val line = CartLine(
            productId = "p1",
            name = "Sugar",
            sku = "SUG-1",
            unit = "kg",
            unitPrice = 60.0,
            quantity = 0.5,
            overrideUnitPrice = 55.0,
            itemDiscountAmount = 2.5
        )

        assertEquals(55.0, line.effectiveUnitPrice, 0.0)
        assertEquals(25.0, line.lineTotal, 0.0)
    }
}
