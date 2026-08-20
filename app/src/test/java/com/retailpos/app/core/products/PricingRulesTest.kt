package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PricingRulesTest {
    @Test
    fun noTaxLeavesCustomerTotalUnchangedAfterDiscount() {
        val result = PricingRules.calculate(
            PricingInput(
                subtotal = 200.0,
                discountAmount = 20.0,
                taxTreatment = TaxTreatment.NO_TAX
            )
        )

        assertEquals(180.0, result.taxableAmount, 0.0001)
        assertEquals(0.0, result.taxAmount, 0.0001)
        assertEquals(180.0, result.total, 0.0001)
    }

    @Test
    fun gstAddedCalculatesTaxAfterDiscount() {
        val result = PricingRules.calculate(
            PricingInput(
                subtotal = 200.0,
                discountAmount = 20.0,
                taxRatePercent = 5.0,
                taxTreatment = TaxTreatment.GST_ADDED
            )
        )

        assertEquals(180.0, result.taxableAmount, 0.0001)
        assertEquals(9.0, result.taxAmount, 0.0001)
        assertEquals(189.0, result.total, 0.0001)
    }

    @Test
    fun gstInclusiveExtractsTaxWithoutIncreasingCustomerTotal() {
        val result = PricingRules.calculate(
            PricingInput(
                subtotal = 118.0,
                taxRatePercent = 18.0,
                taxTreatment = TaxTreatment.GST_INCLUSIVE
            )
        )

        assertEquals(18.0, result.taxAmount, 0.0001)
        assertEquals(118.0, result.total, 0.0001)
    }

    @Test
    fun discountCannotExceedSubtotal() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingRules.calculate(PricingInput(subtotal = 100.0, discountAmount = 100.01))
        }
    }

    @Test
    fun invalidTaxRateIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingRules.calculate(
                PricingInput(
                    subtotal = 100.0,
                    taxRatePercent = 101.0,
                    taxTreatment = TaxTreatment.GST_ADDED
                )
            )
        }
    }
}
