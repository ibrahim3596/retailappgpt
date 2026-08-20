package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPackCompatibilityTest {
    @Test
    fun measuredPackIsConvertibleToKgSellingUnit() {
        val result = ProductPackCompatibility.classify(ParsedPack(500.0, "g", "500 g"), "kg")
        assertEquals(PackCompatibility.CONVERTIBLE_MEASURE, result.compatibility)
    }

    @Test
    fun packagedWeightIsDescriptiveForPieceSellingUnit() {
        val result = ProductPackCompatibility.classify(ParsedPack(500.0, "g", "500 g"), "pcs")
        assertEquals(PackCompatibility.DESCRIPTIVE_PACKAGE, result.compatibility)
        assertTrue(result.explanation.contains("pieces"))
    }

    @Test
    fun incompatibleUnitsRequireReview() {
        val result = ProductPackCompatibility.classify(ParsedPack(500.0, "ml", "500 ml"), "kg")
        assertEquals(PackCompatibility.MISMATCH_REQUIRES_REVIEW, result.compatibility)
    }
}
