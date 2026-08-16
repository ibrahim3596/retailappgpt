package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["storeId", "sku"], unique = true),
        Index(value = ["storeId", "barcode"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val brand: String = "",
    val barcode: String? = null,
    val sku: String? = null,
    val mrp: Double,
    val sellingPrice: Double,
    val purchasePrice: Double,
    val stock: Double,
    val unit: String = "pcs",
    val lowStockThreshold: Double = 5.0,
    val updatedAt: Long
)
