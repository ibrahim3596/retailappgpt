package com.retailpos.app.core.payment

private const val EPSILON = 0.01

/** A single tender used to settle part of a bill. */
data class SplitPaymentPart(
    val method: String,
    val amount: Double
)

object SplitPaymentRules {
    private val methods = setOf("CASH", "UPI", "CARD")

    fun validate(total: Double, parts: List<SplitPaymentPart>): String? {
        if (!total.isFinite() || total < 0.0) return "Invalid payable amount."
        if (parts.size < 2) return "Split payment needs at least two payment parts."
        if (parts.any { it.method !in methods }) return "Unsupported split-payment method."
        if (parts.any { !it.amount.isFinite() || it.amount <= 0.0 }) return "Every split-payment amount must be positive."
        val sum = parts.sumOf { it.amount }
        if (kotlin.math.abs(sum - total) > EPSILON) return "Split payment must equal the payable amount."
        return null
    }

    fun encode(parts: List<SplitPaymentPart>): String =
        "SPLIT:" + parts.joinToString(",") { "${it.method}=${"%.2f".format(java.util.Locale.US, it.amount)}" }

    fun decode(value: String): List<SplitPaymentPart> {
        if (!value.startsWith("SPLIT:")) return emptyList()
        val decoded = value.removePrefix("SPLIT:").split(',').mapNotNull { token ->
            val pair = token.split('=', limit = 2)
            if (pair.size != 2) return@mapNotNull null
            val method = pair[0]
            val amount = pair[1].toDoubleOrNull() ?: return@mapNotNull null
            if (method !in methods || !amount.isFinite() || amount <= 0.0) return@mapNotNull null
            SplitPaymentPart(method, amount)
        }
        return if (decoded.size >= 2) decoded else emptyList()
    }
}
