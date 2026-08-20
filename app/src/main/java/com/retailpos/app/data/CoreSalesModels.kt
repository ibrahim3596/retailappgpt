package com.retailpos.app.data

/** Room projection used by the owner dashboard payment summary query. */
data class PaymentSummary(
    val paymentMethod: String,
    val transactionCount: Int,
    val total: Double
)

/** Result returned after an atomic checkout transaction completes. */
data class CheckoutResult(
    val saleId: String,
    val total: Double,
    val changeAmount: Double
)
