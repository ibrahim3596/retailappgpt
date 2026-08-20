package com.retailpos.app.core.payment

object ReceiptPaymentSummary {
    fun lines(paymentMethod: String, amountTendered: Double?, change: Double): List<String> {
        val normalized = PaymentSummaryRules.parse(paymentMethod)
        if (normalized.isNotEmpty()) return normalized.map { "${it.method}: ${money(it.amount)}" } + listOfNotNull(
            amountTendered?.let { "CASH RECEIVED: ${money(it)}" },
            change.takeIf { it > 0.0 }?.let { "CHANGE: ${money(it)}" }
        )
        return listOfNotNull(
            "PAYMENT: $paymentMethod",
            amountTendered?.let { "CASH RECEIVED: ${money(it)}" },
            change.takeIf { it > 0.0 }?.let { "CHANGE: ${money(it)}" }
        )
    }

    private fun money(value: Double): String = String.format(java.util.Locale.US, "₹%.2f", value)
}
