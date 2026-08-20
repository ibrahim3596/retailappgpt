package com.retailpos.app.core.products

import com.retailpos.app.data.CartLine
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutPricingPreviewTest {
    @Test
    fun noTaxPreviewLeavesTotalUnchanged() {
        val preview = CheckoutPricingPreviewCalculator.calculate(
            cart = listOf(CartLine("p1", "Sugar", "S1", "kg", 50.0, 0.5, 25.0)),
            taxTreatment = TaxTreatment.NO_TAX,
            taxRatesByProductId = mapOf("p1" to 5.0)
        )

        assertEquals(25.0, preview.subtotal, 0.0001)
        assertEquals(0.0, preview.taxAmount, 0.0001)
        assertEquals(25.0, preview.total, 0.0001)
    }

    @Test
    fun regularGstPreviewUsesProductRate() {
        val preview = CheckoutPricingPreviewCalculator.calculate(
            cart = listOf(CartLine("p1", "Sugar", "S1", "kg", 100.0, 1.0, 100.0)),
            taxTreatment = TaxTreatment.GST_ADDED,
            taxRatesByProductId = mapOf("p1" to 5.0)
        )

        assertEquals(5.0, preview.taxAmount, 0.0001)
        assertEquals(105.0, preview.total, 0.0001)
        assertEquals(5.0, preview.lines.single().taxRatePercent, 0.0001)
    }

    @Test
    fun billDiscountIsAllocatedBeforeProductTax() {
        val preview = CheckoutPricingPreviewCalculator.calculate(
            cart = listOf(
                CartLine("p1", "Rice", "R1", "kg", 100.0, 1.0, 100.0),
                CartLine("p2", "Oil", "O1", "litre", 100.0, 1.0, 100.0)
            ),
            taxTreatment = TaxTreatment.GST_ADDED,
            taxRatesByProductId = mapOf("p1" to 5.0, "p2" to 18.0),
            billDiscountAmount = 20.0
        )

        assertEquals(200.0, preview.subtotal, 0.0001)
        assertEquals(20.0, preview.discountAmount, 0.0001)
        assertEquals(11.5, preview.taxAmount, 0.0001)
        assertEquals(191.5, preview.total, 0.0001)
    }
}
