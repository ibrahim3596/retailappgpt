package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductPackParserTest {
    @Test
    fun parsesGrams() {
        val result = ProductPackParser.parse("Net Weight 500 g")!!
        assertEquals(500.0, result.size, 0.0)
        assertEquals("g", result.unit)
    }

    @Test
    fun parsesLitresWithoutSpace() {
        val result = ProductPackParser.parse("1L")!!
        assertEquals(1.0, result.size, 0.0)
        assertEquals("L", result.unit)
    }

    @Test
    fun normalizesIndianUnitSpellings() {
        val result = ProductPackParser.parse("Net Wt. 750 gms and 1 litre")!!
        assertEquals(750.0, result.size, 0.0)
        assertEquals("g", result.unit)
    }

    @Test
    fun ignoresBareNumbers() {
        assertNull(ProductPackParser.parse("MRP ₹99 • Batch 12345"))
    }

    @Test
    fun parsesPackagedCountUnits() {
        val result = ProductPackParser.parse("Pack of 6 pieces")!!
        assertEquals(6.0, result.size, 0.0)
        assertEquals("pcs", result.unit)
    }
}
