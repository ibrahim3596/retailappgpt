package com.retailpos.app.core.products

import com.retailpos.app.data.IdentificationConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductIdentificationRankingTest {
    @Test
    fun barcodeCatalogAgreementIsHighestConfidence() {
        val result = ProductIdentificationRanking.score(
            ProductIdentificationSignals(
                barcodeDetected = true,
                barcodeMatchesCatalog = true,
                catalogMatched = true,
                printedTextDetected = true
            )
        )

        assertEquals(98, result.score)
        assertEquals(IdentificationConfidence.HIGH, result.confidence)
    }

    @Test
    fun visualOnlyEvidenceCannotBecomeHighConfidence() {
        val result = ProductIdentificationRanking.score(
            ProductIdentificationSignals(visualHintDetected = true)
        )

        assertTrue(result.score < 60)
        assertEquals(IdentificationConfidence.LOW, result.confidence)
    }
}
