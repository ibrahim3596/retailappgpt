package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductCaptureParserExtendedTest {
    @Test
    fun explicitBrandAndProductLabelsOverrideHeuristics() {
        val parsed = ProductCaptureParser.parse(
            "Brand: Amul\nProduct Name: Taaza Milk\nMRP: ₹ 64.00\nNet Wt 500 g"
        )
        assertEquals("Taaza Milk", parsed.name)
        assertEquals("Amul", parsed.brand)
        assertEquals(64.0, parsed.mrp!!, 0.001)
    }

    @Test
    fun suffixMrpFormatIsAccepted() {
        val parsed = ProductCaptureParser.parse("Chocolate Bar\n₹49 MRP\n50 g")
        assertEquals(49.0, parsed.mrp!!, 0.001)
    }

    @Test
    fun malformedMrpStillReturnsNull() {
        val parsed = ProductCaptureParser.parse("Sample\nMRP ???")
        assertNull(parsed.mrp)
    }
}
