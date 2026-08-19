package com.retailpos.app.data

class CartManager {
    private val _lines = mutableListOf<CartLine>()

    val lines: List<CartLine> get() = _lines.toList()

    fun add(product: ProductEntity): AddToCartResult {
        if (product.stock <= 0.0) return AddToCartResult.OutOfStock

        val existing = _lines.firstOrNull { it.productId == product.id }
        if (existing != null) {
            if (existing.quantity + 1.0 > product.stock) return AddToCartResult.InsufficientStock
            val index = _lines.indexOf(existing)
            _lines[index] = existing.copy(quantity = existing.quantity + 1.0)
        } else {
            _lines += CartLine(
                productId = product.id,
                name = product.name,
                sku = product.sku,
                unit = product.unit,
                unitPrice = product.sellingPrice
            )
        }
        return AddToCartResult.Added
    }

    fun remove(productId: String): Boolean = _lines.removeIf { it.productId == productId }

    fun clear() = _lines.clear()
}

enum class AddToCartResult { Added, OutOfStock, InsufficientStock }
