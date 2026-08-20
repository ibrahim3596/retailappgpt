package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["storeId", "createdAt"]), Index(value = ["storeId", "category", "createdAt"])]
)
data class ExpenseEntity(
    @androidx.room.PrimaryKey val id: String,
    val storeId: String,
    val amount: Double,
    val category: String,
    val note: String,
    val paymentMethod: String,
    val createdAt: Long
)
