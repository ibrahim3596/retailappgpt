package com.retailpos.app.core.pos

/** Small, deterministic rules used by the cashier quick-add surfaces. */
data class QuickAddProduct(
    val productId: String,
    val name: String,
    val brand: String,
    val unit: String,
    val sellingPrice: Double,
    val availableStock: Double
) {
    val canAdd: Boolean get() = availableStock > 0.0 && availableStock.isFinite()
}

object QuickAddRules {
    fun dedupeInOrder(products: List<QuickAddProduct>, limit: Int = 12): List<QuickAddProduct> {
        if (limit <= 0) return emptyList()
        val seen = HashSet<String>()
        return products.filter { it.productId.isNotBlank() && seen.add(it.productId) }.take(limit)
    }

    fun filterAddable(products: List<QuickAddProduct>): List<QuickAddProduct> =
        products.filter { it.canAdd }
}
