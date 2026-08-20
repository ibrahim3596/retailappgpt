package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CheckoutTransactionFingerprintTest {
    private val cart = listOf(
        CartLine(
            productId = "product-1",
            name = "Sugar",
            sku = "SUGAR-1",
            unit = "kg",
            unitPrice = 50.0,
            quantity = 0.5,
            overrideUnitPrice = null,
            itemDiscountAmount = 0.0
        )
    )

    @Test
    fun paymentMethodChangesTransactionFingerprint() {
        val cash = CheckoutRecoveryFingerprint.transactionOf(cart, "CASH", null, 0.0)
        val upi = CheckoutRecoveryFingerprint.transactionOf(cart, "UPI", null, 0.0)
        assertNotEquals(cash, upi)
    }

    @Test
    fun customerChangesTransactionFingerprint() {
        val walkIn = CheckoutRecoveryFingerprint.transactionOf(cart, "CREDIT", null, 0.0)
        val customer = CheckoutRecoveryFingerprint.transactionOf(cart, "CREDIT", "customer-1", 0.0)
        assertNotEquals(walkIn, customer)
    }

    @Test
    fun billDiscountChangesTransactionFingerprint() {
        val noDiscount = CheckoutRecoveryFingerprint.transactionOf(cart, "CASH", null, 0.0)
        val discount = CheckoutRecoveryFingerprint.transactionOf(cart, "CASH", null, 5.0)
        assertNotEquals(noDiscount, discount)
    }
}
