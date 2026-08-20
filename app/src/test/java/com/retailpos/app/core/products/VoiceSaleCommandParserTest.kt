package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSaleCommandParserTest {
    @Test
    fun parsesHindiLooseGoodsHalfKilo() {
        val command = VoiceSaleCommandParser.parse("aadha kilo shakkar")
        assertNotNull(command)
        assertEquals("sugar", command?.productQuery)
        assertEquals(0.5, command?.quantity ?: 0.0, 0.0001)
        assertEquals(WeightUnit.KG, command?.unit)
    }

    @Test
    fun parsesTeluguSugarAndQuantity() {
        val command = VoiceSaleCommandParser.parse("సగం కిలో చక్కెర")
        assertNotNull(command)
        assertEquals("sugar", command?.productQuery)
        assertEquals(0.5, command?.quantity ?: 0.0, 0.0001)
        assertEquals(WeightUnit.KG, command?.unit)
    }

    @Test
    fun parsesMalayalamOil() {
        val command = VoiceSaleCommandParser.parse("പകുതി ലിറ്റർ എണ്ണ")
        assertNotNull(command)
        assertEquals("oil", command?.productQuery)
        assertEquals(0.5, command?.quantity ?: 0.0, 0.0001)
        assertEquals(WeightUnit.L, command?.unit)
    }

    @Test
    fun parsesMarathiSugar() {
        val command = VoiceSaleCommandParser.parse("अर्धा किलो साखर")
        assertNotNull(command)
        assertEquals("sugar", command?.productQuery)
        assertEquals(0.5, command?.quantity ?: 0.0, 0.0001)
        assertEquals(WeightUnit.KG, command?.unit)
    }

    @Test
    fun convertsHalfKiloToGramSellingUnit() {
        val base = VoiceSaleCommandParser.toBaseQuantity(0.5, WeightUnit.KG, "g")
        assertEquals(500.0, base ?: 0.0, 0.0001)
    }

    @Test
    fun voiceLanguageCatalogHasHindiAsDefaultAndRequestedIndianLanguages() {
        assertEquals("hi-IN", VoiceLanguages.DEFAULT.tag)
        val tags = VoiceLanguages.SUPPORTED.map { it.tag }.toSet()
        assertTrue(tags.contains("te-IN"))
        assertTrue(tags.contains("ml-IN"))
        assertTrue(tags.contains("mr-IN"))
        assertTrue(tags.contains("ta-IN"))
        assertTrue(tags.contains("kn-IN"))
    }
}
