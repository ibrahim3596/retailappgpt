package com.retailpos.app.core.offline

import com.retailpos.app.data.CartLine

enum class CartRecoveryIssueType { MISSING_PRODUCT, ARCHIVED_PRODUCT, INSUFFICIENT_STOCK, INVALID_PRICING }

data class CartRecoveryIssue(
    val productId: String,
    val productName: String,
    val type: CartRecoveryIssueType,
    val detail: String
)

data class CartRecoveryResult(
    val validLines: List<CartLine>,
    val issues: List<CartRecoveryIssue>
)

object ActiveCartRecovery {
    fun evaluate(cart: List<CartLine>, currentProducts: Map<String, RecoveryProduct>): CartRecoveryResult {
        val valid = mutableListOf<CartLine>()
        val issues = mutableListOf<CartRecoveryIssue>()
        cart.forEach { line ->
            val product = currentProducts[line.productId]
            if (product == null) {
                issues += CartRecoveryIssue(line.productId, line.name, CartRecoveryIssueType.MISSING_PRODUCT, "Product is no longer in the catalog.")
                return@forEach
            }
            if (product.archived) {
                issues += CartRecoveryIssue(line.productId, product.name, CartRecoveryIssueType.ARCHIVED_PRODUCT, "Product is archived and cannot be sold.")
                return@forEach
            }
            if (!line.quantity.isFinite() || line.quantity <= 0.0 || !line.lineTotal.isFinite() || line.lineTotal < 0.0) {
                issues += CartRecoveryIssue(line.productId, product.name, CartRecoveryIssueType.INVALID_PRICING, "Saved quantity or pricing is invalid.")
                return@forEach
            }
            if (!product.stock.isFinite() || product.stock < 0.0) {
                issues += CartRecoveryIssue(line.productId, product.name, CartRecoveryIssueType.INVALID_PRICING, "Current inventory quantity is invalid.")
                return@forEach
            }
            if (line.quantity > product.stock + 1e-9) {
                issues += CartRecoveryIssue(line.productId, product.name, CartRecoveryIssueType.INSUFFICIENT_STOCK, "Only ${product.stock} ${product.unit} is available.")
                return@forEach
            }
            valid += line
        }
        return CartRecoveryResult(valid, issues)
    }
}

data class RecoveryProduct(val name: String, val unit: String, val stock: Double, val archived: Boolean)
