package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CheckoutRecoveryFingerprintCollisionTest {
    private fun line(name: String, sku: String = "SUGAR-1") = CartLine("product-1", name, sku, "kg", 100.0, 1.0, null, 0.0)

    @Test
    fun delimiterCharactersDoNotCreateFingerprintCollisions() {
        val first = CheckoutRecoveryFingerprint.of(listOf(line("A|B")))
        val second = CheckoutRecoveryFingerprint.of(listOf(line("A", "B")))
        assertNotEquals(first, second)
    }
}
