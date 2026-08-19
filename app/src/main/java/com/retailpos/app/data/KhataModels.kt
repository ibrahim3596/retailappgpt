package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_ledger",
    indices = [
        Index(value = ["storeId", "customerId", "createdAt"]),
        Index(value = ["storeId", "referenceType", "referenceId"])
    ]
)
data class CustomerLedgerEntry(
    @PrimaryKey val id: String,
    val storeId: String,
    val customerId: String,
    /** Positive = customer owes the store; negative = customer payment. */
    val amount: Double,
    val type: String,
    val note: String,
    val referenceType: String?,
    val referenceId: String?,
    val createdAt: Long
)

data class CustomerBalance(
    val balance: Double
)
