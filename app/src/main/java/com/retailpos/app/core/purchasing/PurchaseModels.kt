package com.retailpos.app.core.purchasing

import java.util.UUID

/** Supplier payable direction. POS never hides supplier debt inside stock value. */
data class SupplierDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = ""
)

data class PurchaseLineDraft(
    val productId: String,
    val productName: String,
    val orderedQuantity: Double,
    val freeQuantity: Double = 0.0,
    val purchaseRate: Double,
    val schemeDiscount: Double = 0.0,
    val batchNumber: String? = null,
    val expiryDate: Long? = null
)

data class PurchaseDraft(
    val supplierId: String,
    val invoiceNumber: String? = null,
    val lines: List<PurchaseLineDraft>,
    val paidAmount: Double = 0.0,
    val notes: String = ""
)

data class PurchaseLineEconomics(
    val paidQuantity: Double,
    val freeQuantity: Double,
    val stockQuantity: Double,
    val grossCost: Double,
    val schemeDiscount: Double,
    val netCost: Double,
    val effectiveUnitCost: Double
)
