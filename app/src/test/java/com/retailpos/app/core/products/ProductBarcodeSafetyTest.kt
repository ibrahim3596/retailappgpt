package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductBarcodeSafetyTest {
    @Test
    fun acceptsCommonNumericProductCodes() {
        assertEquals(ProductBarcodeDecision.ACCEPT, ProductBarcodeSafety.classify("8901234567890"))
        assertEquals(ProductBarcodeDecision.ACCEPT, ProductBarcodeSafety.classify("12345678"))
    }

    @Test
    fun ignoresQrPayloads() {
        assertEquals(ProductBarcodeDecision.IGNORE_QR, ProductBarcodeSafety.classify("upi://pay?pa=shop@upi"))
        assertEquals(ProductBarcodeDecision.IGNORE_QR, ProductBarcodeSafety.classify("BEGIN:VCARD\nFN:Shop"))
    }

    @Test
    fun rejectsShortOrNonProductContent() {
        assertEquals(ProductBarcodeDecision.REJECT_INVALID, ProductBarcodeSafety.classify("abc"))
        assertEquals(ProductBarcodeDecision.REJECT_INVALID, ProductBarcodeSafety.classify("12345678901234567890"))
    }
}
