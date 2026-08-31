package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import com.retailpos.app.data.ProductEntity

sealed interface ActiveCartRecoveryIssue {
    val line: CartLine

    data class Missing(override val line: CartLine) : ActiveCartRecoveryIssue
    data class Archived(override val line: CartLine) : ActiveCartRecoveryIssue
    data class InsufficientStock(override val line: CartLine, val available: Double) : ActiveCartRecoveryIssue
    data class InvalidPricing(override val line: CartLine) : ActiveCartRecoveryIssue
}

object ActiveCartRecovery {
    fun validate(lines: List<CartLine>, products: Map<String, ProductEntity>): List<ActiveCartRecoveryIssue> =
        lines.mapNotNull { line ->
            val product = products[line.productId] ?: return@mapNotNull ActiveCartRecoveryIssue.Missing(line)
            if (product.isArchived) return@mapNotNull ActiveCartRecoveryIssue.Archived(line)
            if (!line.quantity.isFinite() || line.quantity <= 0.0 ||
                !line.unitPrice.isFinite() || line.unitPrice < 0.0 ||
                (line.overrideUnitPrice != null &&
                    (!line.overrideUnitPrice.isFinite() || line.overrideUnitPrice < 0.0)) ||
                !line.itemDiscountAmount.isFinite() || line.itemDiscountAmount < 0.0 ||
                !line.lineTotal.isFinite() || line.lineTotal < 0.0 ||
                !product.stock.isFinite() || product.stock < 0.0
            ) {
                return@mapNotNull ActiveCartRecoveryIssue.InvalidPricing(line)
            }
            if (line.quantity > product.stock + 1e-9) return@mapNotNull ActiveCartRecoveryIssue.InsufficientStock(line, product.stock)
            null
        }

    fun message(issue: ActiveCartRecoveryIssue): String = when (issue) {
        is ActiveCartRecoveryIssue.Missing -> "${issue.line.name} is no longer in the catalog."
        is ActiveCartRecoveryIssue.Archived -> "${issue.line.name} is archived and cannot be sold."
        is ActiveCartRecoveryIssue.InsufficientStock -> "${issue.line.name}: only ${clean(issue.available)} ${issue.line.unit} available; saved bill needs ${clean(issue.line.quantity)}."
        is ActiveCartRecoveryIssue.InvalidPricing -> "${issue.line.name} has invalid saved pricing and needs review."
    }

    private fun clean(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.2f", value)
}
