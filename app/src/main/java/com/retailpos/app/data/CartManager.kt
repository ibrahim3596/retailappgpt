package com.retailpos.app.data

class CartManager {
    private val _lines = mutableListOf<CartLine>()

    val lines: List<CartLine> get() = _lines.toList()

    fun add(product: ProductEntity): AddToCartResult = addQuantity(product, 1.0)

    fun addQuantity(product: ProductEntity, requestedQuantity: Double): AddToCartResult {
        if (requestedQuantity <= 0.0 || !requestedQuantity.isFinite()) return AddToCartResult.InvalidQuantity
        if (product.stock <= 0.0) return AddToCartResult.OutOfStock

        val existing = _lines.firstOrNull { it.productId == product.id }
        val newQuantity = (existing?.quantity ?: 0.0) + requestedQuantity
        if (newQuantity > product.stock + 1e-9) return AddToCartResult.InsufficientStock

        if (existing != null) {
            val index = _lines.indexOf(existing)
            _lines[index] = existing.copy(quantity = newQuantity)
        } else {
            _lines += CartLine(product.id, product.name, product.sku, product.unit, product.sellingPrice, requestedQuantity)
        }
        return AddToCartResult.Added
    }

    fun setQuantity(productId: String, requestedQuantity: Double, availableStock: Double): AddToCartResult {
        if (requestedQuantity <= 0.0 || !requestedQuantity.isFinite()) return AddToCartResult.InvalidQuantity
        if (availableStock <= 0.0) return AddToCartResult.OutOfStock
        if (requestedQuantity > availableStock + 1e-9) return AddToCartResult.InsufficientStock
        val index = _lines.indexOfFirst { it.productId == productId }
        if (index < 0) return AddToCartResult.InvalidQuantity
        _lines[index] = _lines[index].copy(quantity = requestedQuantity)
        return AddToCartResult.Added
    }

    fun replace(lines: List<CartLine>) {
        require(lines.all { it.quantity.isFinite() && it.quantity > 0.0 }) { "Invalid cart quantity" }
        _lines.clear()
        _lines.addAll(lines)
    }

    fun remove(productId: String): Boolean = _lines.removeIf { it.productId == productId }
    fun clear() = _lines.clear()
}

enum class AddToCartResult { Added, OutOfStock, InsufficientStock, InvalidQuantity }
