package com.example.retailpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["storeId", "phone"], unique = true)
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val phone: String,
    val currentBalance: Double = 0.0, // Positive means customer owes shopkeeper
    val creditLimit: Double = 10000.0,
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class LedgerEntryType {
    DEBIT,  // Increase customer debt (Credit sale)
    CREDIT  // Decrease customer debt (Payment received)
}

@Entity(
    tableName = "credit_ledger",
    indices = [
        Index(value = ["storeId", "customerId"])
    ]
)
data class CreditLedgerEntryEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val customerId: String,
    val type: LedgerEntryType,
    val amount: Double,
    val balanceAfter: Double,
    val referenceId: String = "", // Invoice ID or Payment Receipt ID
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val address: String = "",
    val currentBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val supplierId: String,
    val supplierName: String,
    val invoiceNumber: String = "",
    val totalAmount: Double,
    val gstTotal: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val productId: String,
    val productName: String,
    val batchNumber: String,
    val expiryDate: Long,
    val quantity: Double,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val mrp: Double,
    val itemTotal: Double
)
