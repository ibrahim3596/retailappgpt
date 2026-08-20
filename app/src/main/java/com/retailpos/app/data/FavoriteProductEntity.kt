package com.retailpos.app.data

import androidx.room.Entity

@Entity(
    tableName = "favorite_products",
    primaryKeys = ["storeId", "productId"]
)
data class FavoriteProductEntity(
    val storeId: String,
    val productId: String,
    val createdAt: Long
)
