package com.example.retailpos.engine.barcode

import com.google.mlkit.vision.barcode.common.Barcode
import org.junit.Assert.*
import org.junit.Test

class ScannerFilteringTest {

    @Test
    fun `test supported product formats`() {
        assertTrue(ScannerFiltering.isSupportedProductFormat(Barcode.FORMAT_EAN_13))
        assertTrue(ScannerFiltering.isSupportedProductFormat(Barcode.FORMAT_EAN_8))
        assertTrue(ScannerFiltering.isSupportedProductFormat(Barcode.FORMAT_UPC_A))
        assertTrue(ScannerFiltering.isSupportedProductFormat(Barcode.FORMAT_UPC_E))
        assertTrue(ScannerFiltering.isSupportedProductFormat(Barcode.FORMAT_ITF))
        assertTrue(ScannerFiltering.isSupportedProductFormat(Barcode.FORMAT_CODE_128))
    }

    @Test
    fun `test unsupported formats - QR Code`() {
        assertFalse(ScannerFiltering.isSupportedProductFormat(Barcode.FORMAT_QR_CODE))
    }

    @Test
    fun `test format names`() {
        assertEquals("EAN-13", ScannerFiltering.getFormatName(Barcode.FORMAT_EAN_13))
        assertEquals("QR-CODE", ScannerFiltering.getFormatName(Barcode.FORMAT_QR_CODE))
        assertEquals("ITF", ScannerFiltering.getFormatName(Barcode.FORMAT_ITF))
        assertEquals("BARCODE", ScannerFiltering.getFormatName(99999))
    }
}
