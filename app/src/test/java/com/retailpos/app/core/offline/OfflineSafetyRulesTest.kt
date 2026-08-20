package com.retailpos.app.core.offline

import com.retailpos.app.core.inventory.ExpiryRules
import com.retailpos.app.core.inventory.ExpiryStatus
import com.retailpos.app.core.payment.ReceiptPaymentSummary
import com.retailpos.app.core.products.ProductBarcodeDecision
import com.retailpos.app.core.products.ProductBarcodeSafety
import com.retailpos.app.core.reconciliation.DayEndRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class OfflineSafetyRulesTest {
    @Test fun barcode_rejects_payment_and_urls() {
        assertEquals(ProductBarcodeDecision.IGNORE_QR, ProductBarcodeSafety.classify("upi://pay?pa=shop@upi"))
        assertEquals(ProductBarcodeDecision.IGNORE_QR, ProductBarcodeSafety.classify("https://example.com"))
    }

    @Test fun expiry_classification_is_deterministic() {
        val now = 1_000_000L
        assertEquals(ExpiryStatus.EXPIRED, ExpiryRules.status(now - 1, now))
        assertEquals(ExpiryStatus.NEAR_EXPIRY, ExpiryRules.status(now + TimeUnit.DAYS.toMillis(10), now))
        assertEquals(ExpiryStatus.FRESH, ExpiryRules.status(now + TimeUnit.DAYS.toMillis(60), now))
    }

    @Test fun day_end_balance_uses_small_tolerance() {
        assertTrue(DayEndRules.isBalanced(1000.0, 1000.01))
        assertFalse(DayEndRules.isBalanced(1000.0, 1000.02))
    }

    @Test fun receipt_split_payment_contains_components() {
        val lines = ReceiptPaymentSummary.lines("SPLIT:CASH=300.00,UPI=200.00", null, 0.0)
        assertTrue(lines.any { it.startsWith("CASH:") })
        assertTrue(lines.any { it.startsWith("UPI:") })
    }
}
