package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_movements",
    indices = [
        Index(value = ["storeId", "productId", "createdAt"]),
        Index(value = ["storeId", "referenceType", "referenceId"])
    ]
)
data class InventoryMovementEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val productId: String,
    val quantityDelta: Double,
    val reason: String,
    val referenceType: String?,
    val referenceId: String?,
    val createdAt: Long
)

enum class InventoryMovementReason {
    SALE,
    ADJUSTMENT,
    INITIAL_STOCK,
    RECEIVE,
    RETURN,
    DAMAGE
}

@Entity(
    tableName = "inventory_batches",
    indices = [
        Index(value = ["storeId", "productId"]),
        Index(value = ["storeId", "productId", "expiryDate"])
    ]
)
data class InventoryBatchEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val productId: String,
    val batchNumber: String?,
    val expiryDate: Long?,
    val quantity: Double,
    val purchasePrice: Double,
    val createdAt: Long
)
