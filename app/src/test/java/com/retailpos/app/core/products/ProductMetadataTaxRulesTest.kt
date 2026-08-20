package com.retailpos.app.core.products

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMetadataTaxRulesTest {
    @Test
    fun zeroTaxRateIsValid() {
        assertTrue(ProductMetadataRules.isValidTaxRatePercent(0.0))
    }

    @Test
    fun commonGstRateIsValid() {
        assertTrue(ProductMetadataRules.isValidTaxRatePercent(18.0))
    }

    @Test
    fun boundaryHundredIsValid() {
        assertTrue(ProductMetadataRules.isValidTaxRatePercent(100.0))
    }

    @Test
    fun negativeAndAboveHundredAreRejected() {
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(-0.01))
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(100.01))
    }

    @Test
    fun nonFiniteRatesAreRejected() {
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(Double.NaN))
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(Double.POSITIVE_INFINITY))
    }
}
