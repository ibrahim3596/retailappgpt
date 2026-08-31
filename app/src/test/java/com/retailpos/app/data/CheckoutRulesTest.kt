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
        assertTrue(CheckoutRules.validatePaymentMethod("CREDIT"))
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

    @Test
    fun cartRejectsInvalidOverridesDiscountsAndTotals() {
        val valid = CartLine("p1", "Milk", "SKU-1", "pcs", 30.0, 2.0)
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(overrideUnitPrice = Double.NaN))))
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(overrideUnitPrice = -1.0))))
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(itemDiscountAmount = Double.NaN))))
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(itemDiscountAmount = -1.0))))
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(grossLineTotal = -1.0))))
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(lineTotal = -1.0))))
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(effectiveUnitPrice = -1.0))))
        assertFalse(CheckoutRules.validateCart(listOf(valid.copy(unitPrice = Double.MAX_VALUE, quantity = 2.0))))
    }

    @Test
    fun cartRejectsDuplicateOrBlankProductIds() {
        val first = CartLine("p1", "Milk", "SKU-1", "pcs", 30.0, 1.0)
        assertFalse(CheckoutRules.validateCart(listOf(first, first.copy(name = "Milk 2"))))
        assertFalse(CheckoutRules.validateCart(listOf(first.copy(productId = ""))))
        assertFalse(CheckoutRules.validateCart(listOf(first.copy(productId = "   "))))
    }
}
