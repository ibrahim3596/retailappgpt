package com.example.retailpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory {
    RENT,
    ELECTRICITY,
    TRANSPORT,
    SALARIES,
    REPAIRS,
    PACKAGING,
    SUPPLIES,
    MARKETING,
    MISC
}

enum class ExpenseSyncStatus {
    PENDING,
    SYNCED,
    CONFLICT
}

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["storeId", "date"]),
        Index(value = ["storeId", "category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val category: String, // ExpenseCategory name; string so new categories sync back-compatibly
    val amount: Double,
    val date: Long,
    val paymentMethod: String, // CASH, UPI, CARD, OTHER
    val notes: String = "",
    val syncStatus: String = "PENDING", // ExpenseSyncStatus name
    val localId: String, // client-generated idempotent ID for server sync
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
