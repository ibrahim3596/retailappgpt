package com.retailpos.app.data

object CheckoutRules {
    val paymentMethods = setOf("CASH", "UPI", "CARD")

    fun validatePaymentMethod(paymentMethod: String): Boolean = paymentMethod in paymentMethods

    fun validateIdempotencyKey(idempotencyKey: String): Boolean = idempotencyKey.isNotBlank()

    fun validateCart(cart: List<CartLine>): Boolean =
        cart.isNotEmpty() && cart.all { it.quantity > 0.0 && it.unitPrice >= 0.0 }
}
