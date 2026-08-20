package com.retailpos.app

import com.retailpos.app.core.products.VoiceSaleCommandParser
import com.retailpos.app.core.products.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceSaleCommandTest {
    @Test
    fun parsesHindiHalfKiloRequest() {
        val command = VoiceSaleCommandParser.parse("aadha kilo shakkar")
        assertNotNull(command)
        assertEquals("shakkar", command!!.productQuery)
        assertEquals(0.5, command.quantity, 0.0001)
        assertEquals(WeightUnit.KG, command.unit)
    }

    @Test
    fun parsesNumericGramRequest() {
        val command = VoiceSaleCommandParser.parse("500 gram sugar")
        assertNotNull(command)
        assertEquals("sugar", command!!.productQuery)
        assertEquals(500.0, command.quantity, 0.0001)
        assertEquals(WeightUnit.G, command.unit)
    }

    @Test
    fun parsesLiterRequest() {
        val command = VoiceSaleCommandParser.parse("1 litre oil")
        assertNotNull(command)
        assertEquals("oil", command!!.productQuery)
        assertEquals(1.0, command.quantity, 0.0001)
        assertEquals(WeightUnit.L, command.unit)
    }

    @Test
    fun normalizesGramsToKgProduct() {
        val quantity = VoiceSaleCommandParser.toBaseQuantity(500.0, WeightUnit.G, "kg")
        assertEquals(0.5, quantity!!, 0.0001)
    }

    @Test
    fun rejectsMissingQuantity() {
        assertNull(VoiceSaleCommandParser.parse("sugar"))
    }
}
