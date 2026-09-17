package com.example.retailpos.engine.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeNormalizerTest {

    @Test
    fun `sanitizes non-numeric characters`() {
        val result = BarcodeNormalizer.normalize(" 8901-0303-0001-8 ")
        assertEquals("8901030300018", result.sanitizedInput)
        assertEquals("EAN-13", result.symbology)
    }

    @Test
    fun `EAN-13 validates checksum correctly`() {
        val result = BarcodeNormalizer.normalize("8901030300018")
        assertTrue(result.isValidChecksum)
    }

    @Test
    fun `invalid EAN-13 returns false checksum`() {
        val result = BarcodeNormalizer.normalize("8901030300019")
        assertFalse(result.isValidChecksum)
    }

    @Test
    fun `UPC-A expands to EAN-13 with leading zero`() {
        val result = BarcodeNormalizer.normalize("012345678905")
        assertEquals("0012345678905", result.canonicalGtin)
        assertEquals("UPC-A", result.symbology)
    }

    @Test
    fun `empty input returns empty canonical`() {
        val result = BarcodeNormalizer.normalize("")
        assertEquals("", result.canonicalGtin)
        assertEquals("", result.sanitizedInput)
        assertEquals("UNKNOWN", result.symbology)
    }

    @Test
    fun `alphabetic input returns empty sanitized`() {
        val result = BarcodeNormalizer.normalize("abc123")
        assertEquals("123", result.sanitizedInput)
        assertEquals("", result.canonicalGtin)
    }
}
