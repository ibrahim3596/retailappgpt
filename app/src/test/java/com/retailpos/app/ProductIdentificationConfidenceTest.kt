package com.retailpos.app

import com.retailpos.app.data.IdentificationConfidence
import com.retailpos.app.data.ProductIdentificationConfidence
import com.retailpos.app.data.ProductIdentificationEvidence
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductIdentificationConfidenceTest {
    @Test
    fun catalogBarcodeMatchIsHigh() {
        val evidence = ProductIdentificationEvidence(
            barcodeDetected = true,
            catalogMatched = true,
            barcodeMatchesCatalog = true
        )
        assertEquals(IdentificationConfidence.HIGH, ProductIdentificationConfidence.evaluate(evidence))
    }

    @Test
    fun barcodeWithPrintedTextIsGood() {
        val evidence = ProductIdentificationEvidence(
            barcodeDetected = true,
            printedTextDetected = true
        )
        assertEquals(IdentificationConfidence.GOOD, ProductIdentificationConfidence.evaluate(evidence))
    }

    @Test
    fun printedTextOnlyIsMedium() {
        val evidence = ProductIdentificationEvidence(printedTextDetected = true)
        assertEquals(IdentificationConfidence.MEDIUM, ProductIdentificationConfidence.evaluate(evidence))
    }

    @Test
    fun visualHintOnlyIsLow() {
        val evidence = ProductIdentificationEvidence(visualHintDetected = true)
        assertEquals(IdentificationConfidence.LOW, ProductIdentificationConfidence.evaluate(evidence))
    }

    @Test
    fun noEvidenceIsNone() {
        assertEquals(
            IdentificationConfidence.NONE,
            ProductIdentificationConfidence.evaluate(ProductIdentificationEvidence())
        )
    }
}
