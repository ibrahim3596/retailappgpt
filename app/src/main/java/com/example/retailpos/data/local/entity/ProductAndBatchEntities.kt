package com.example.retailpos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class VerificationStatus {
    UNKNOWN,
    IDENTIFIED,
    REVIEW_REQUIRED,
    VERIFIED,
    REJECTED,
    STALE
}

enum class TaxType {
    INCLUSIVE,
    EXCLUSIVE
}

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["storeId", "sku"], unique = true),
        Index(value = ["storeId", "barcode"], unique = true),
        Index(value = ["storeId", "normalizedBarcode"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val sku: String,
    val barcode: String,
    val normalizedBarcode: String,
    val name: String,
    val brand: String = "",
    val category: String = "General",
    val variant: String = "",
    val packSize: String = "",
    val productImage: String = "",
    val hsnCode: String = "",
    val unit: String = "PCS",
    val mrp: Double,
    val sellingPrice: Double,
    val purchasePrice: Double,
    val gstRate: Double = 0.0, // 0, 5, 12, 18, 28
    val taxType: TaxType = TaxType.INCLUSIVE,
    val currentStock: Double = 0.0,
    val minStock: Double = 5.0,
    val maxStock: Double = 100.0,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val confidenceScore: Double = 1.0,
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "batches",
    indices = [
        Index(value = ["storeId", "productId"]),
        Index(value = ["expiryDate"])
    ]
)
data class BatchEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val storeId: String,
    val batchNumber: String,
    val mfd: String = "",
    val expiryDate: Long, // Epoch timestamp millis
    val mrp: Double,
    val sellingPrice: Double,
    val purchasePrice: Double,
    val initialQty: Double,
    val remainingQty: Double,
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis()
)

enum class StockMovementType {
    SALE,
    PURCHASE,
    RETURN,
    ADJUSTMENT,
    INITIAL
}

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["storeId", "productId"]),
        Index(value = ["batchId"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val productId: String,
    val batchId: String? = null,
    val type: StockMovementType,
    val quantity: Double, // Positive for addition, negative for deduction
    val balanceAfter: Double,
    val referenceId: String = "", // e.g. invoiceId or purchaseId
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val createdBy: String = "SYSTEM"
)
