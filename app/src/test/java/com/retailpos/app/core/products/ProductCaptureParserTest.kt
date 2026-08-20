package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCaptureParserTest {
    @Test
    fun extractsNameBrandAndMrpWhileIgnoringMetadataLines() {
        val parsed = ProductCaptureParser.parse(
            "ACME Chocolate Bar\nACME\nMRP ₹ 49.00\nNet Wt 50 g\nIngredients sugar"
        )

        assertEquals("ACME Chocolate Bar", parsed.name)
        assertEquals("ACME", parsed.brand)
        assertEquals(49.0, parsed.mrp!!, 0.001)
        assertTrue(parsed.usefulLines.none { it.contains("MRP", ignoreCase = true) })
    }

    @Test
    fun malformedMrpDoesNotBecomePrice() {
        val parsed = ProductCaptureParser.parse("Sample Product\nMRP --\nRs ???")
        assertNull(parsed.mrp)
    }
}
