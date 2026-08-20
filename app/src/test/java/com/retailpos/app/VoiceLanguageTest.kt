package com.retailpos.app

import com.retailpos.app.core.products.VoiceLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceLanguageTest {
    @Test
    fun hindiIsDefault() {
        assertEquals("hi-IN", VoiceLanguages.DEFAULT.tag)
        assertEquals("hi-IN", VoiceLanguages.SUPPORTED.first().tag)
    }

    @Test
    fun includesMajorIndianLanguages() {
        val tags = VoiceLanguages.SUPPORTED.map { it.tag }
        assertTrue(tags.containsAll(listOf("hi-IN", "te-IN", "ml-IN", "mr-IN", "ta-IN", "kn-IN", "bn-IN", "gu-IN", "pa-IN", "or-IN")))
    }

    @Test
    fun localeMatchingAcceptsRegionalVariant() {
        assertTrue(VoiceLanguages.matchesLocale(listOf("te-IN"), "te-IN"))
        assertTrue(VoiceLanguages.matchesLocale(listOf("te-IN"), "te"))
    }
}
