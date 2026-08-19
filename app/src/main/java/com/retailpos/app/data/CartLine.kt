package com.retailpos.app.data

data class CartLine(
    val productId: String,
    val name: String,
    val sku: String?,
    val unit: String,
    val unitPrice: Double,
    val quantity: Double = 1.0
) {
    val lineTotal: Double get() = unitPrice * quantity
}
