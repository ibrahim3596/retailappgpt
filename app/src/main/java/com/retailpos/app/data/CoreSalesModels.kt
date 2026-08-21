package com.retailpos.app.data

/** Owner analytics projection for the highest-volume sale products. */
data class TopProductSales(
    val productId: String,
    val name: String,
    val quantity: Double,
    val revenue: Double
)
