package com.retailpos.app.core.payment

import com.retailpos.app.data.PaymentSummary

object PaymentSummaryRules {
    fun normalize(summaries: List<PaymentSummary>): List<PaymentSummary> {
        val totals = linkedMapOf<String, Pair<Int, Double>>()
        fun add(method: String, count: Int, amount: Double) {
            val current = totals[method]
            totals[method] = (current?.first ?: 0) + count to (current?.second ?: 0.0) + amount
        }

        summaries.forEach { summary ->
            val split = SplitPaymentRules.decode(summary.paymentMethod)
            if (split.isEmpty()) {
                if (!summary.paymentMethod.startsWith("SPLIT:")) {
                    add(summary.paymentMethod, summary.transactionCount, summary.total)
                }
            } else {
                split.forEach { part -> add(part.method, summary.transactionCount, part.amount) }
            }
        }

        return totals.entries
            .map { (method, value) -> PaymentSummary(method, value.first, value.second) }
            .sortedByDescending { it.total }
    }
}
