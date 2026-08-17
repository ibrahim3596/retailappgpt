package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_barcodes",
    indices = [
        Index(value = ["storeId", "value"]),
        Index(value = ["productId"])
    ]
)
data class ProductBarcodeEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val storeId: String,
    val value: String,
    val type: String,
    val isPrimary: Boolean = false,
    val createdAt: Long
)
