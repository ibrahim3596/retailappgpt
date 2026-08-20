package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "held_bills",
    indices = [Index(value = ["storeId", "createdAt"])]
)
data class HeldBillEntity(
    @androidx.room.PrimaryKey val id: String,
    val storeId: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "held_bill_lines",
    primaryKeys = ["heldBillId", "productId"],
    indices = [Index(value = ["heldBillId"])]
)
data class HeldBillLineEntity(
    val heldBillId: String,
    val productId: String,
    val name: String,
    val sku: String?,
    val unit: String,
    val unitPrice: Double,
    val quantity: Double
)
