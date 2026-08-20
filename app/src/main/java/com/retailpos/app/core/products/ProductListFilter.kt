package com.retailpos.app.core.products

/** Filters used by the product master list. */
enum class ProductListFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK,
    ARCHIVED
}

fun ProductListFilter.matches(stock: Double, threshold: Double, archived: Boolean = false): Boolean = when (this) {
    ProductListFilter.ALL -> !archived
    ProductListFilter.LOW_STOCK -> !archived && stock > 0.0 && stock <= threshold
    ProductListFilter.OUT_OF_STOCK -> !archived && stock <= 0.0
    ProductListFilter.ARCHIVED -> archived
}
