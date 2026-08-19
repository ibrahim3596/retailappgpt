package com.example.retailpos.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductIdentifierValidatorTest {
    @Test
    fun validGtinsAreAccepted() {
        assertTrue(ProductIdentifierValidator.isValidGtIn("4006381333931"))
        assertTrue(ProductIdentifierValidator.isValidGtIn("036000291452"))
    }

    @Test
    fun invalidCheckDigitIsRejected() {
        assertFalse(ProductIdentifierValidator.isValidGtIn("4006381333932"))
    }

    @Test
    fun unsupportedLengthIsRejected() {
        assertFalse(ProductIdentifierValidator.isValidGtIn("123456789"))
    }

    @Test
    fun normalizationRemovesWhitespaceAndNormalizesCase() {
        assertTrue(ProductIdentifierValidator.normalize("  abc-123  ") == "ABC-123")
    }

    @Test
    fun retailPosTypesAreLimitedToConsumerGtins() {
        assertTrue(ProductIdentifierValidator.isRetailPosGtInType(BarcodeType.EAN_13))
        assertTrue(ProductIdentifierValidator.isRetailPosGtInType(BarcodeType.UPC_A))
        assertFalse(ProductIdentifierValidator.isRetailPosGtInType(BarcodeType.ITF_14))
        assertFalse(ProductIdentifierValidator.isRetailPosGtInType(BarcodeType.QR_CODE))
    }
}
