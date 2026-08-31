package com.retailpos.app.core.payment

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPaymentStoreFingerprintTest {
    @Test
    fun matchingCartFingerprintRestoresPendingAmount() {
        assertTrue(PendingPaymentStore.matchesCartFingerprint("cart-a", "cart-a"))
    }

    @Test
    fun changedOrMissingCartFingerprintDoesNotRestorePendingAmount() {
        assertFalse(PendingPaymentStore.matchesCartFingerprint("cart-a", "cart-b"))
        assertFalse(PendingPaymentStore.matchesCartFingerprint("cart-a", null))
        assertFalse(PendingPaymentStore.matchesCartFingerprint(null, "cart-a"))
        assertFalse(PendingPaymentStore.matchesCartFingerprint("", "cart-a"))
    }
}
