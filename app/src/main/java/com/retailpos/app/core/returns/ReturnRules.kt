package com.retailpos.app.core.returns

object ReturnRules {
    fun validateQuantity(requested: Double, remaining: Double): String? = when {
        !requested.isFinite() || requested <= 0.0 -> "Return quantity must be greater than zero"
        requested > remaining + 1e-9 -> "Return quantity exceeds the remaining quantity"
        else -> null
    }

    fun validateRefundAmount(amount: Double, maximum: Double): String? = when {
        !amount.isFinite() || amount < 0.0 -> "Refund amount is invalid"
        amount > maximum + 1e-9 -> "Refund cannot exceed the refundable amount"
        else -> null
    }
}
