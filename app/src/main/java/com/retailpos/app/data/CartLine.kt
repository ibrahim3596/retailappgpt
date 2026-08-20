package com.retailpos.app.data

data class CartLine(
    val productId: String,
    val name: String,
    val sku: String?,
    val unit: String,
    val unitPrice: Double,
    val quantity: Double = 1.0,
    val overrideUnitPrice: Double? = null,
    val itemDiscountAmount: Double = 0.0
) {
    val effectiveUnitPrice: Double get() = overrideUnitPrice ?: unitPrice
    val grossLineTotal: Double get() = effectiveUnitPrice * quantity
    val lineTotal: Double get() = (grossLineTotal - itemDiscountAmount).coerceAtLeast(0.0)
}
