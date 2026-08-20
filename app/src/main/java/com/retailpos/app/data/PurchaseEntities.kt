package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["storeId", "name"]), Index(value = ["storeId", "phone"])]
)
data class SupplierEntity(
    @androidx.room.PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val phone: String,
    val address: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["storeId", "createdAt"]),
        Index(value = ["storeId", "supplierId", "createdAt"]),
        Index(value = ["storeId", "invoiceNumber"])
    ]
)
data class PurchaseEntity(
    @androidx.room.PrimaryKey val id: String,
    val storeId: String,
    val supplierId: String,
    val invoiceNumber: String?,
    val grossAmount: Double,
    val schemeDiscount: Double,
    val netAmount: Double,
    val paidAmount: Double,
    val outstandingAmount: Double,
    val createdAt: Long
)

@Entity(
    tableName = "purchase_lines",
    primaryKeys = ["purchaseId", "productId"],
    indices = [Index(value = ["purchaseId"]), Index(value = ["storeId", "productId", "createdAt"])]
)
data class PurchaseLineEntity(
    val purchaseId: String,
    val storeId: String,
    val productId: String,
    val orderedQuantity: Double,
    val freeQuantity: Double,
    val purchaseRate: Double,
    val schemeDiscount: Double,
    val netCost: Double,
    val effectiveCost: Double,
    val batchNumber: String?,
    val expiryDate: Long?,
    val createdAt: Long
)

@Entity(
    tableName = "supplier_ledger",
    indices = [
        Index(value = ["storeId", "supplierId", "createdAt"]),
        Index(value = ["storeId", "referenceType", "referenceId"])
    ]
)
data class SupplierLedgerEntry(
    @androidx.room.PrimaryKey val id: String,
    val storeId: String,
    val supplierId: String,
    val amount: Double,
    val type: String,
    val note: String,
    val referenceType: String?,
    val referenceId: String?,
    val createdAt: Long
)
