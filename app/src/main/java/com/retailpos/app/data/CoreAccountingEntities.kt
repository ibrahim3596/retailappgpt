package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sale_cost_allocations",
    indices = [Index(value = ["saleId"]), Index(value = ["saleLineId"]), Index(value = ["productId"])]
)
data class SaleCostAllocationEntity(
    @androidx.room.PrimaryKey val id: String,
    val saleId: String,
    val saleLineId: String,
    val productId: String,
    val quantity: Double,
    val unitCost: Double,
    val totalCost: Double,
    val batchId: String?,
    val createdAt: Long
)
