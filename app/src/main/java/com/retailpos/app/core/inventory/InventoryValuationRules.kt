package com.retailpos.app.core.inventory

import com.retailpos.app.data.InventoryBatchEntity

/** Deterministic batch-based inventory economics; legacy unbatched stock is not invented here. */
data class InventoryValuationSummary(
    val totalBatchQuantity: Double,
    val totalCostValue: Double,
    val sellableQuantity: Double,
    val sellableCostValue: Double,
    val nearExpiryQuantity: Double,
    val nearExpiryCostValue: Double,
    val expiredQuantity: Double,
    val expiredCostValue: Double
)

object InventoryValuationRules {
    fun summarize(
        batches: List<InventoryBatchEntity>,
        now: Long,
        warningDays: Long = ExpiryPolicy.DEFAULT_WARNING_WINDOW_DAYS
    ): InventoryValuationSummary {
        val warningUntil = now + ExpiryPolicy.warningWindowMs(warningDays)
        var totalQty = 0.0
        var totalValue = 0.0
        var sellableQty = 0.0
        var sellableValue = 0.0
        var nearQty = 0.0
        var nearValue = 0.0
        var expiredQty = 0.0
        var expiredValue = 0.0

        batches.filter { it.quantity > 0.0 }.forEach { batch ->
            val value = batch.quantity * batch.purchasePrice
            totalQty += batch.quantity
            totalValue += value
            val expiry = batch.expiryDate
            when {
                expiry != null && expiry <= now -> {
                    expiredQty += batch.quantity
                    expiredValue += value
                }
                expiry != null && expiry <= warningUntil -> {
                    nearQty += batch.quantity
                    nearValue += value
                    sellableQty += batch.quantity
                    sellableValue += value
                }
                else -> {
                    sellableQty += batch.quantity
                    sellableValue += value
                }
            }
        }

        return InventoryValuationSummary(
            totalBatchQuantity = totalQty,
            totalCostValue = totalValue,
            sellableQuantity = sellableQty,
            sellableCostValue = sellableValue,
            nearExpiryQuantity = nearQty,
            nearExpiryCostValue = nearValue,
            expiredQuantity = expiredQty,
            expiredCostValue = expiredValue
        )
    }
}
