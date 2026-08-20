package com.retailpos.app.core.payments

enum class PaymentMethod {
    CASH,
    UPI,
    CARD,
    CREDIT
}

data class PaymentResult(
    val method: PaymentMethod,
    val amountTendered: Double,
    val change: Double
)

object PaymentRules {
    fun validate(method: PaymentMethod, total: Double, amountTendered: Double = total): PaymentResult {
        require(total.isFinite() && total >= 0.0) { "Total must be non-negative and finite." }
        require(amountTendered.isFinite() && amountTendered >= 0.0) { "Amount tendered must be non-negative and finite." }
        return when (method) {
            PaymentMethod.CASH -> {
                require(amountTendered + 1e-9 >= total) { "Cash received is less than the payable amount." }
                PaymentResult(method, amountTendered, (amountTendered - total).coerceAtLeast(0.0))
            }
            PaymentMethod.CREDIT -> PaymentResult(method, 0.0, 0.0)
            PaymentMethod.UPI, PaymentMethod.CARD -> {
                require(kotlin.math.abs(amountTendered - total) <= 0.01) { "Electronic payment amount must match the payable amount." }
                PaymentResult(method, total, 0.0)
            }
        }
    }
}
