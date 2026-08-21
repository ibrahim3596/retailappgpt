package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntelligentCaptureRulesTest {
    @Test
    fun parsesExplicitPackQuantityWithoutUsingBareNumbers() {
        val parsed = ProductPackParser.parse("Net weight 500 g | MRP 120")
        assertEquals(500.0, parsed?.size ?: -1.0, 0.0)
        assertEquals("g", parsed?.unit)
        assertTrue(ProductPackParser.parse("MRP 120") == null)
    }

    @Test
    fun packageCanDescribePieceSoldProduct() {
        val pack = ParsedPack(500.0, "g", "500 g")
        val result = ProductPackCompatibility.classify(pack, "pcs")
        assertEquals(PackCompatibility.DESCRIPTIVE_PACKAGE, result.compatibility)
    }

    @Test
    fun incompatibleLiquidAndWeightRequireReview() {
        val pack = ParsedPack(500.0, "ml", "500 ml")
        val result = ProductPackCompatibility.classify(pack, "kg")
        assertEquals(PackCompatibility.MISMATCH_REQUIRES_REVIEW, result.compatibility)
    }

    @Test
    fun repeatedEvidenceAndTextAgreementIncreaseRankingWithoutConfirmation() {
        val base = ProductIdentificationRanking.score(
            ProductIdentificationSignals(barcodeDetected = true, catalogMatched = true)
        )
        val stronger = ProductIdentificationRanking.score(
            ProductIdentificationSignals(
                barcodeDetected = true,
                catalogMatched = true,
                textAgreesWithCandidate = true,
                multipleFrameAgreement = true,
                packCompatibleWithSellingUnit = true
            )
        )
        assertTrue(stronger.score > base.score)
        assertTrue(stronger.score < 100)
    }

    @Test
    fun conflictingFeedbackNeverProducesLearningBoost() {
        val result = ProductIdentificationFeedbackRules.toSignal(
            ProductIdentificationFeedback(
                acceptedCatalog = true,
                rejectedCatalog = true,
                retainedCameraData = false,
                correctedName = false,
                correctedBrand = false,
                correctedPack = false
            )
        )
        assertEquals(0, result.rankingBoost)
    }
}
