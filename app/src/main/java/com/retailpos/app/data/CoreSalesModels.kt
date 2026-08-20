package com.retailpos.app.data

/** Room projection used by the owner dashboard payment summary query. */
data class PaymentSummary(
    val paymentMethod: String,
    val transactionCount: Int,
    val total: Double
)

/** Owner analytics projection for the highest-volume sale products. */
data class TopProductSales(
    val productId: String,
    val name: String,
    val quantity: Double,
    val revenue: Double
)

/** Result returned after an atomic checkout transaction completes. */
data class CheckoutResult(
    val saleId: String,
    val total: Double,
    val changeAmount: Double
)
