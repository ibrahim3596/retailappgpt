package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCaptureParserTest {
    @Test
    fun extractsNameBrandMrpAndExplicitFlavorWhileIgnoringMetadataLines() {
        val parsed = ProductCaptureParser.parse(
            "BRAND: Frooti\nProduct: Mango Drink\nFlavour: Mango\nMRP ₹ 40.00\nNet Wt 600 ml\nIngredients sugar"
        )

        assertEquals("Mango Drink", parsed.name)
        assertEquals("Frooti", parsed.brand)
        assertEquals("Mango", parsed.variant)
        assertEquals(40.0, parsed.mrp!!, 0.001)
        assertTrue(parsed.usefulLines.none { it.contains("MRP", ignoreCase = true) })
    }

    @Test
    fun malformedMrpDoesNotBecomePrice() {
        val parsed = ProductCaptureParser.parse("Sample Product\nMRP --\nRs ???")
        assertNull(parsed.mrp)
    }

    @Test
    fun doesNotGuessVariantFromUnlabeledMarketingText() {
        val parsed = ProductCaptureParser.parse(
            "BRAND: Example\nExample Cool Summer Taste\nNet Wt 500 ml\nMRP ₹ 20"
        )

        assertNull(parsed.variant)
    }
}
