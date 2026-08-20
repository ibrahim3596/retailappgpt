package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "held_bills",
    indices = [Index(value = ["storeId", "createdAt"])]
)
data class HeldBillEntity(
    @PrimaryKey val id: String,
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

data class HeldBillWithLines(
    val bill: HeldBillEntity,
    val lines: List<HeldBillLineEntity>
)
