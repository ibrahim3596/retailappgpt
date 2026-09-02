package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CheckoutRecoveryFingerprintTest {
    private fun line(
        quantity: Double = 1.0,
        unitPrice: Double = 100.0,
        overridePrice: Double? = null,
        discount: Double = 0.0
    ) = CartLine(
        productId = "product-1",
        name = "Sugar",
        sku = "SUGAR-1",
        unit = "kg",
        unitPrice = unitPrice,
        quantity = quantity,
        overrideUnitPrice = overridePrice,
        itemDiscountAmount = discount
    )

    @Test
    fun sameCartProducesSameFingerprint() {
        val fingerprint1 = CheckoutRecoveryFingerprint.of(listOf(line()))
        val fingerprint2 = CheckoutRecoveryFingerprint.of(listOf(line()))
        assertEquals(fingerprint1, fingerprint2)
    }

    @Test
    fun quantityChangeProducesDifferentFingerprint() {
        val original = CheckoutRecoveryFingerprint.of(listOf(line(quantity = 1.0)))
        val changed = CheckoutRecoveryFingerprint.of(listOf(line(quantity = 0.5)))
        assertNotEquals(original, changed)
    }

    @Test
    fun priceOrDiscountChangeProducesDifferentFingerprint() {
        val original = CheckoutRecoveryFingerprint.of(listOf(line()))
        assertNotEquals(original, CheckoutRecoveryFingerprint.of(listOf(line(unitPrice = 105.0))))
        assertNotEquals(original, CheckoutRecoveryFingerprint.of(listOf(line(overridePrice = 95.0))))
        assertNotEquals(original, CheckoutRecoveryFingerprint.of(listOf(line(discount = 5.0))))
    }

    @Test
    fun lineOrderDoesNotChangeFingerprint() {
        val sugar = line()
        val rice = sugar.copy(productId = "product-2", name = "Rice", sku = "RICE-1")
        assertEquals(
            CheckoutRecoveryFingerprint.of(listOf(sugar, rice)),
            CheckoutRecoveryFingerprint.of(listOf(rice, sugar))
        )
    }

    @Test
    fun duplicateProductLinesAreCanonicalizedIndependentlyOfOrder() {
        val first = line(quantity = 1.0, unitPrice = 100.0)
        val second = line(quantity = 2.0, unitPrice = 80.0, discount = 5.0)
        assertEquals(
            CheckoutRecoveryFingerprint.of(listOf(first, second)),
            CheckoutRecoveryFingerprint.of(listOf(second, first))
        )
    }

    @Test
    fun signedZeroDoesNotChangeFingerprint() {
        val positiveZero = CheckoutRecoveryFingerprint.of(listOf(line(discount = 0.0)))
        val negativeZero = CheckoutRecoveryFingerprint.of(listOf(line(discount = -0.0)))
        assertEquals(positiveZero, negativeZero)
    }
}
