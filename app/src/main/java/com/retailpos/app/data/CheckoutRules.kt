package com.retailpos.app.data

import com.retailpos.app.core.payment.SplitPaymentRules

object CheckoutRules {
    val paymentMethods = setOf("CASH", "UPI", "CARD", "CREDIT", "SPLIT")

    fun validatePaymentMethod(paymentMethod: String): Boolean =
        paymentMethod in paymentMethods || paymentMethod.startsWith("SPLIT:") && SplitPaymentRules.decode(paymentMethod).isNotEmpty()

    fun validateIdempotencyKey(idempotencyKey: String): Boolean = idempotencyKey.isNotBlank()

    fun validateCart(cart: List<CartLine>): Boolean =
        cart.isNotEmpty() && cart.all { it.quantity > 0.0 && it.quantity.isFinite() && it.unitPrice >= 0.0 && it.unitPrice.isFinite() }
}
