package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "returns",
    indices = [Index(value = ["storeId", "createdAt"]), Index(value = ["storeId", "originalSaleId"])]
)
data class ReturnEntity(
    @androidx.room.PrimaryKey val id: String,
    val storeId: String,
    val originalSaleId: String,
    val refundMethod: String,
    val refundAmount: Double,
    val reason: String,
    val staffRole: String,
    val createdAt: Long
)

@Entity(
    tableName = "return_lines",
    primaryKeys = ["returnId", "saleLineId"],
    indices = [Index(value = ["returnId"]), Index(value = ["saleLineId"]), Index(value = ["productId"])]
)
data class ReturnLineEntity(
    val returnId: String,
    val saleLineId: String,
    val productId: String,
    val quantity: Double,
    val refundAmount: Double,
    val restoredCost: Double
)

data class ReturnCandidateLine(
    val saleLine: SaleLineEntity,
    val alreadyReturnedQuantity: Double
) {
    val remainingQuantity: Double get() = (saleLine.quantity - alreadyReturnedQuantity).coerceAtLeast(0.0)
}
