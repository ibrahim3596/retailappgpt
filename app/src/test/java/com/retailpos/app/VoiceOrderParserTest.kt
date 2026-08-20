package com.retailpos.app

import com.retailpos.app.core.products.VoiceOrderParser
import com.retailpos.app.core.products.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VoiceOrderParserTest {
    @Test
    fun parsesHindiMultiItemOrder() {
        val commands = VoiceOrderParser.parse("aadha kilo shakkar aur 1 litre tel")
        assertNotNull(commands)
        assertEquals(2, commands!!.size)
        assertEquals("sugar", commands[0].productQuery)
        assertEquals(0.5, commands[0].quantity, 0.0001)
        assertEquals(WeightUnit.KG, commands[0].unit)
        assertEquals("oil", commands[1].productQuery)
        assertEquals(1.0, commands[1].quantity, 0.0001)
        assertEquals(WeightUnit.L, commands[1].unit)
    }

    @Test
    fun parsesPackagedAndLooseItemsTogether() {
        val commands = VoiceOrderParser.parse("2 packets biscuits and 250 gram rice")
        assertNotNull(commands)
        assertEquals(2, commands!!.size)
        assertEquals("biscuits", commands[0].productQuery)
        assertEquals(2.0, commands[0].quantity, 0.0001)
        assertEquals(WeightUnit.PIECE, commands[0].unit)
        assertEquals("rice", commands[1].productQuery)
        assertEquals(250.0, commands[1].quantity, 0.0001)
        assertEquals(WeightUnit.G, commands[1].unit)
    }

    @Test
    fun parsesCommaSeparatedOrder() {
        val commands = VoiceOrderParser.parse("500 gram sugar, 250 gram rice")
        assertEquals(2, commands!!.size)
        assertEquals(500.0, commands[0].quantity, 0.0001)
        assertEquals(250.0, commands[1].quantity, 0.0001)
    }

    @Test
    fun parsesTeluguConnector() {
        val commands = VoiceOrderParser.parse("సగం కిలో చక్కెర మరియు 1 లీటర్ నూనె")
        assertEquals(2, commands!!.size)
        assertEquals("sugar", commands[0].productQuery)
        assertEquals("oil", commands[1].productQuery)
    }
}
