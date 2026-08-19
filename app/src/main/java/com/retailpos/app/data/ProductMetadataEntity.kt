package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index

/** Optional product-master metadata kept separate so identity and inventory stay stable. */
@Entity(
    tableName = "product_metadata",
    primaryKeys = ["productId"],
    indices = [Index(value = ["storeId", "category"])]
)
data class ProductMetadataEntity(
    val productId: String,
    val storeId: String,
    val category: String = "",
    val subcategory: String = "",
    val packSize: Double? = null,
    val packUnit: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val updatedAt: Long
)
