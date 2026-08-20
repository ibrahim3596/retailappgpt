package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_identification_feedback",
    indices = [
        Index(value = ["storeId", "createdAt"]),
        Index(value = ["storeId", "candidateKey", "createdAt"]),
        Index(value = ["storeId", "barcode", "createdAt"])
    ]
)
data class ProductIdentificationFeedbackEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val barcode: String?,
    val candidateKey: String?,
    val outcome: String,
    val rankingBoost: Int,
    val explanation: String,
    val createdAt: Long
)
