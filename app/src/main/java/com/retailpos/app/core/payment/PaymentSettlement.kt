package com.retailpos.app.core.payment

private const val EPSILON = 0.000001

data class PaymentSettlement(
    val method: String,
    val total: Double,
    val amountTendered: Double?,
    val change: Double
)

object PaymentSettlementRules {
    private val supportedMethods = setOf("CASH", "UPI", "CARD", "CREDIT")

    fun settle(method: String, total: Double, amountTendered: Double? = null): PaymentSettlement {
        require(method in supportedMethods) { "Unsupported payment method" }
        require(total.isFinite() && total >= 0.0) { "Invalid payable amount" }

        return when (method) {
            "CASH" -> {
                val tendered = amountTendered ?: 0.0
                require(tendered.isFinite() && tendered >= total - EPSILON) { "Cash received must cover the payable amount" }
                PaymentSettlement(method, total, tendered, (tendered - total).coerceAtLeast(0.0))
            }
            "UPI", "CARD" -> {
                val paid = amountTendered ?: total
                require(paid.isFinite() && kotlin.math.abs(paid - total) <= 0.01) { "Electronic payment must match the payable amount" }
                PaymentSettlement(method, total, paid, 0.0)
            }
            "CREDIT" -> PaymentSettlement(method, total, null, 0.0)
            else -> error("Unsupported payment method")
        }
    }
}
