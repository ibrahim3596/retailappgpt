package com.example.retailpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncLogEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val storeId: String,
    val entityType: String, // INVOICE, PRODUCT, CUSTOMER, STOCK_MOVEMENT
    val entityId: String,
    val action: String, // CREATE, UPDATE, DELETE
    val payloadJson: String,
    val attemptCount: Int = 0,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED, CONFLICT
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

@Entity(tableName = "sync_commands")
data class SyncCommandEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val installationId: String,
    val localTransactionId: String,
    val commandType: String, // SALE, PURCHASE, RETURN, CUSTOMER_PAYMENT, STOCK_ADJUSTMENT
    val idempotencyKey: String,
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, SUCCESS, CONFLICT, FAILED
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
)

@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val entityType: String,
    val entityId: String,
    val localDataJson: String,
    val serverDataJson: String,
    val conflictReason: String,
    val status: String = "UNRESOLVED", // UNRESOLVED, RESOLVED_LOCAL, RESOLVED_SERVER, RESOLVED_MERGE
    val createdAt: Long = System.currentTimeMillis()
)

enum class ProvenanceSource {
    MANUAL,
    LOCAL,
    GLOBAL_CATALOG,
    EXTERNAL_PROVIDER,
    OCR,
    AI,
    SHOPKEEPER
}

@Entity(tableName = "product_provenance")
data class ProductProvenanceEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val fieldName: String, // name, mrp, sellingPrice, purchasePrice, gstRate, hsnCode, brand
    val source: ProvenanceSource,
    val confidence: Double = 1.0,
    val verifiedByShopkeeper: Boolean = false,
    val rawValue: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_provenance_history")
data class ProductProvenanceHistoryEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val fieldName: String,
    val oldValue: String,
    val newValue: String,
    val source: ProvenanceSource,
    val updatedByUserId: String = "SYSTEM",
    val createdAt: Long = System.currentTimeMillis()
)
