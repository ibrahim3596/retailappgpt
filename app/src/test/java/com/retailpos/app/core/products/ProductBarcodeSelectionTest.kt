package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductBarcodeSelectionTest {
    @Test
    fun prefersCentralLargeCandidateOverTinyEdgeCandidate() {
        val chosen = ProductBarcodeSelection.choose(
            candidates = listOf(
                ProductBarcodeCandidate("8901111111111", 1, 60f, 60f, 0.01f),
                ProductBarcodeCandidate("8902222222222", 1, 500f, 400f, 0.16f)
            ),
            frameWidth = 600,
            frameHeight = 500
        )

        assertEquals("8902222222222", chosen?.rawValue)
    }

    @Test
    fun centralityCanBeatAOnlySlightlyLargerEdgeCandidate() {
        val chosen = ProductBarcodeSelection.choose(
            candidates = listOf(
                ProductBarcodeCandidate("edge", 1, 590f, 490f, 0.30f),
                ProductBarcodeCandidate("center", 1, 300f, 250f, 0.20f)
            ),
            frameWidth = 600,
            frameHeight = 500
        )

        assertEquals("center", chosen?.rawValue)
    }

    @Test
    fun invalidFrameDimensionsYieldNoSelection() {
        assertNull(
            ProductBarcodeSelection.choose(
                candidates = listOf(ProductBarcodeCandidate("8901111111111", 1, 1f, 1f, 0.2f)),
                frameWidth = 0,
                frameHeight = 500
            )
        )
    }
}
