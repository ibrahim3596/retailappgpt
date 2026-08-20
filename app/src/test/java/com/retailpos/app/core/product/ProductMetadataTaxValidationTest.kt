package com.retailpos.app.core.product

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMetadataTaxValidationTest {
    @Test
    fun zeroAndCommonRatesAreValid() {
        assertTrue(ProductMetadataRules.isValidTaxRatePercent(0.0))
        assertTrue(ProductMetadataRules.isValidTaxRatePercent(5.0))
        assertTrue(ProductMetadataRules.isValidTaxRatePercent(18.0))
    }

    @Test
    fun outOfRangeAndNonFiniteRatesAreInvalid() {
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(-0.1))
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(100.1))
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(Double.NaN))
        assertFalse(ProductMetadataRules.isValidTaxRatePercent(Double.POSITIVE_INFINITY))
    }
}
