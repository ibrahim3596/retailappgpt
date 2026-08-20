package com.retailpos.app.core.products

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCaptureStabilityTest {
    @Test
    fun barcodeEvidenceCanBeReviewedImmediately() {
        val result = ProductCaptureStabilityRules.evaluate(
            ProductCaptureObservation(barcode = "8901234567890", frameCount = 1)
        )
        assertTrue(result.stable)
    }

    @Test
    fun printedTextNeedsRepeatedEvidence() {
        val one = ProductCaptureStabilityRules.evaluate(
            ProductCaptureObservation(printedName = "Sugar", frameCount = 1)
        )
        val repeated = ProductCaptureStabilityRules.evaluate(
            ProductCaptureObservation(printedName = "Sugar", frameCount = 2)
        )
        assertFalse(one.stable)
        assertTrue(repeated.stable)
    }

    @Test
    fun visualOnlyEvidenceNeverBecomesStableIdentity() {
        val result = ProductCaptureStabilityRules.evaluate(
            ProductCaptureObservation(categoryHint = "Beverage", categoryConfidence = 0.98f, frameCount = 5)
        )
        assertFalse(result.stable)
    }
}
