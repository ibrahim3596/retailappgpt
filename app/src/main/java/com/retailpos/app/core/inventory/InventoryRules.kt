package com.retailpos.app.core.inventory

object InventoryRules {
    fun validateAdjustment(quantityDelta: Double): String? = when {
        !quantityDelta.isFinite() -> "Quantity must be finite"
        quantityDelta == 0.0 -> "Stock adjustment cannot be zero"
        else -> null
    }

    fun validateReceive(quantity: Double, purchasePrice: Double, expiryDate: Long?, now: Long): String? = when {
        !quantity.isFinite() || quantity <= 0.0 -> "Received quantity must be greater than zero"
        !purchasePrice.isFinite() || purchasePrice < 0.0 -> "Purchase price cannot be negative"
        expiryDate != null && expiryDate < now -> "Expiry date cannot be in the past"
        else -> null
    }

    fun stockState(stock: Double, threshold: Double): StockState = when {
        !stock.isFinite() || stock < 0.0 -> StockState.INVALID
        stock <= 0.0 -> StockState.OUT_OF_STOCK
        stock <= threshold.coerceAtLeast(0.0) -> StockState.LOW_STOCK
        else -> StockState.HEALTHY
    }

    fun expiryState(expiryDate: Long?, now: Long, warningWindowMs: Long): ExpiryState = when {
        expiryDate == null -> ExpiryState.NO_EXPIRY
        expiryDate < now -> ExpiryState.EXPIRED
        expiryDate - now <= warningWindowMs -> ExpiryState.EXPIRING_SOON
        else -> ExpiryState.OK
    }
}

enum class StockState { HEALTHY, LOW_STOCK, OUT_OF_STOCK, INVALID }

enum class ExpiryState { OK, EXPIRING_SOON, EXPIRED, NO_EXPIRY }
