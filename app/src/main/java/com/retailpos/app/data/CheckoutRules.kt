package com.retailpos.app.data

import com.retailpos.app.core.payment.SplitPaymentRules

object CheckoutRules {
    val paymentMethods = setOf("CASH", "UPI", "CARD", "CREDIT", "SPLIT")

    fun validatePaymentMethod(paymentMethod: String): Boolean =
        paymentMethod in paymentMethods || paymentMethod.startsWith("SPLIT:") && SplitPaymentRules.decode(paymentMethod).isNotEmpty()

    fun validateIdempotencyKey(idempotencyKey: String): Boolean = idempotencyKey.isNotBlank()

    fun validateCart(cart: List<CartLine>): Boolean {
        if (cart.isEmpty()) return false
        val productIds = HashSet<String>(cart.size)
        return cart.all { line ->
            productIds.add(line.productId) &&
                line.productId.isNotBlank() &&
                line.quantity > 0.0 && line.quantity.isFinite() &&
                line.unitPrice >= 0.0 && line.unitPrice.isFinite() &&
                (line.overrideUnitPrice == null ||
                    (line.overrideUnitPrice >= 0.0 && line.overrideUnitPrice.isFinite())) &&
                line.itemDiscountAmount >= 0.0 && line.itemDiscountAmount.isFinite() &&
                line.effectiveUnitPrice.isFinite() &&
                line.grossLineTotal.isFinite() &&
                line.lineTotal.isFinite()
        }
    }
}
