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
        assertEquals("sugar", command!!.productQuery)
        assertEquals(0.5, command.quantity, 0.0001)
        assertEquals(WeightUnit.KG, command.unit)
    }

    @Test
    fun parsesDevanagariHalfKiloRequest() {
        val command = VoiceSaleCommandParser.parse("आधा किलो शक्कर")
        assertNotNull(command)
        assertEquals("sugar", command!!.productQuery)
        assertEquals(0.5, command.quantity, 0.0001)
        assertEquals(WeightUnit.KG, command.unit)
    }

    @Test
    fun parsesQuarterKiloRequest() {
        val command = VoiceSaleCommandParser.parse("quarter kilo sugar")
        assertNotNull(command)
        assertEquals("sugar", command!!.productQuery)
        assertEquals(0.25, command.quantity, 0.0001)
        assertEquals(WeightUnit.KG, command.unit)
    }

    @Test
    fun parsesNumericGramRequest() {
        val command = VoiceSaleCommandParser.parse("750 gram sugar")
        assertNotNull(command)
        assertEquals("sugar", command!!.productQuery)
        assertEquals(750.0, command.quantity, 0.0001)
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
    fun stripsPacketUnitForPackagedGoods() {
        val command = VoiceSaleCommandParser.parse("2 packets biscuits")
        assertNotNull(command)
        assertEquals("biscuits", command!!.productQuery)
        assertEquals(2.0, command.quantity, 0.0001)
        assertEquals(WeightUnit.PIECE, command.unit)
    }

    @Test
    fun stripsBottleUnitForCountBasedProducts() {
        val command = VoiceSaleCommandParser.parse("3 bottles water")
        assertNotNull(command)
        assertEquals("water", command!!.productQuery)
        assertEquals(3.0, command.quantity, 0.0001)
        assertEquals(WeightUnit.PIECE, command.unit)
    }

    @Test
    fun parsesTeluguSugarAlias() {
        val command = VoiceSaleCommandParser.parse("సగం కిలో పంచదార")
        assertNotNull(command)
        assertEquals("sugar", command!!.productQuery)
        assertEquals(0.5, command.quantity, 0.0001)
        assertEquals(WeightUnit.KG, command.unit)
    }

    @Test
    fun normalizesGramsToKgProduct() {
        val quantity = VoiceSaleCommandParser.toBaseQuantity(500.0, WeightUnit.G, "kg")
        assertEquals(0.5, quantity!!, 0.0001)
    }

    @Test
    fun normalizesPacketProductToPieceQuantity() {
        val quantity = VoiceSaleCommandParser.toBaseQuantity(2.0, WeightUnit.PIECE, "packet")
        assertEquals(2.0, quantity!!, 0.0001)
    }

    @Test
    fun normalizesTeluguKilogramProduct() {
        val quantity = VoiceSaleCommandParser.toBaseQuantity(500.0, WeightUnit.G, "కిలో")
        assertEquals(0.5, quantity!!, 0.0001)
    }

    @Test
    fun normalizesHindiLiterProduct() {
        val quantity = VoiceSaleCommandParser.toBaseQuantity(500.0, WeightUnit.ML, "लीटर")
        assertEquals(0.5, quantity!!, 0.0001)
    }

    @Test
    fun normalizesTamilMilliliterProduct() {
        val quantity = VoiceSaleCommandParser.toBaseQuantity(500.0, WeightUnit.ML, "மில்லிலிட்டர்")
        assertEquals(500.0, quantity!!, 0.0001)
    }

    @Test
    fun rejectsMissingQuantity() {
        assertNull(VoiceSaleCommandParser.parse("sugar"))
    }
}
