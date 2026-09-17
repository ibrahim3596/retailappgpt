package com.example.retailpos.data.local.entity

import com.example.retailpos.data.local.entity.ProductEntity

/**
 * Temporary UI-level purchase item entry for building purchase orders.
 * Not a Room entity — used only during the purchase creation flow.
 */
data class PurchaseItemEntry(
    val product: ProductEntity,
    var quantity: Double,
    var purchasePrice: Double,
    var sellingPrice: Double,
    var mrp: Double,
    var batchNumber: String,
    var expiryDate: Long
)
