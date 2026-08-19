package com.retailpos.app.core.products

/** Filters used by the product master list. */
enum class ProductListFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK
}

fun ProductListFilter.matches(stock: Double, threshold: Double): Boolean = when (this) {
    ProductListFilter.ALL -> true
    ProductListFilter.LOW_STOCK -> stock > 0.0 && stock <= threshold
    ProductListFilter.OUT_OF_STOCK -> stock <= 0.0
}
