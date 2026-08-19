package com.retailpos.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutRulesTest {
    @Test
    fun acceptsSupportedPaymentMethodsOnly() {
        assertTrue(CheckoutRules.validatePaymentMethod("CASH"))
        assertTrue(CheckoutRules.validatePaymentMethod("UPI"))
        assertTrue(CheckoutRules.validatePaymentMethod("CARD"))
        assertFalse(CheckoutRules.validatePaymentMethod("CREDIT"))
    }

    @Test
    fun rejectsBlankIdempotencyKey() {
        assertTrue(CheckoutRules.validateIdempotencyKey("checkout-123"))
        assertFalse(CheckoutRules.validateIdempotencyKey(""))
        assertFalse(CheckoutRules.validateIdempotencyKey("   "))
    }

    @Test
    fun cartMustContainValidPositiveLines() {
        val valid = CartLine("p1", "Milk", "SKU-1", "pcs", 30.0, 2.0)
        val zeroQuantity = valid.copy(quantity = 0.0)
        val negativePrice = valid.copy(unitPrice = -1.0)

        assertTrue(CheckoutRules.validateCart(listOf(valid)))
        assertFalse(CheckoutRules.validateCart(emptyList()))
        assertFalse(CheckoutRules.validateCart(listOf(zeroQuantity)))
        assertFalse(CheckoutRules.validateCart(listOf(negativePrice)))
    }
}
