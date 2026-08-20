package com.retailpos.app.core.returns

object ReturnRules {
    private val nonCreditRefundMethods = setOf("CASH", "UPI", "CARD")

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

    fun validateRefundMethod(originalPaymentMethod: String, refundMethod: String): String? = when {
        originalPaymentMethod == "CREDIT" && refundMethod != "CREDIT_REVERSAL" -> "Credit sales must be refunded through Khata reversal."
        originalPaymentMethod != "CREDIT" && refundMethod == "CREDIT_REVERSAL" -> "Khata reversal is only valid for a credit sale."
        refundMethod == "CREDIT_REVERSAL" || refundMethod in nonCreditRefundMethods -> null
        else -> "Unsupported refund method."
    }
}
