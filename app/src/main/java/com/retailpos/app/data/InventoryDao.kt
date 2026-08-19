package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.util.UUID

@Dao
abstract class InventoryDao {
    @Insert
    abstract suspend fun insertMovement(movement: InventoryMovementEntity)

    @Insert
    abstract suspend fun insertBatch(batch: InventoryBatchEntity)

    @Query("SELECT * FROM inventory_movements WHERE storeId = :storeId ORDER BY createdAt DESC")
    abstract suspend fun getMovements(storeId: String): List<InventoryMovementEntity>

    @Query("SELECT * FROM inventory_movements WHERE storeId = :storeId AND productId = :productId ORDER BY createdAt DESC")
    abstract suspend fun getProductMovements(storeId: String, productId: String): List<InventoryMovementEntity>

    @Query("SELECT * FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0 ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, createdAt ASC")
    abstract suspend fun getAvailableBatchesFefo(storeId: String, productId: String): List<InventoryBatchEntity>

    @Query("UPDATE products SET stock = stock + :quantityDelta, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId AND stock + :quantityDelta >= 0")
    abstract suspend fun updateProductStock(
        storeId: String,
        productId: String,
        quantityDelta: Double,
        updatedAt: Long
    ): Int

    @Query("UPDATE inventory_batches SET quantity = quantity + :quantityDelta WHERE id = :batchId AND storeId = :storeId AND productId = :productId AND quantity + :quantityDelta >= 0")
    abstract suspend fun updateBatchQuantity(
        storeId: String,
        productId: String,
        batchId: String,
        quantityDelta: Double
    ): Int

    @Transaction
    open suspend fun adjustStock(
        storeId: String,
        productId: String,
        quantityDelta: Double,
        reason: InventoryMovementReason = InventoryMovementReason.ADJUSTMENT,
        referenceType: String? = null,
        referenceId: String? = null,
        now: Long = System.currentTimeMillis()
    ) {
        require(quantityDelta != 0.0) { "Stock adjustment cannot be zero" }
        require(reason != InventoryMovementReason.SALE) { "Sale stock changes must use checkout" }
        val updated = updateProductStock(storeId, productId, quantityDelta, now)
        check(updated == 1) { "Stock cannot become negative" }
        insertMovement(
            InventoryMovementEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = productId,
                quantityDelta = quantityDelta,
                reason = reason.name,
                referenceType = referenceType,
                referenceId = referenceId,
                createdAt = now
            )
        )
    }

    @Transaction
    open suspend fun adjustBatchStock(
        storeId: String,
        productId: String,
        batchId: String,
        quantityDelta: Double,
        reason: InventoryMovementReason = InventoryMovementReason.ADJUSTMENT,
        now: Long = System.currentTimeMillis()
    ) {
        require(quantityDelta != 0.0) { "Stock adjustment cannot be zero" }
        require(reason != InventoryMovementReason.SALE) { "Sale stock changes must use checkout" }
        val productUpdated = updateProductStock(storeId, productId, quantityDelta, now)
        check(productUpdated == 1) { "Product stock cannot become negative" }
        val batchUpdated = updateBatchQuantity(storeId, productId, batchId, quantityDelta)
        check(batchUpdated == 1) { "Batch stock cannot become negative" }
        insertMovement(
            InventoryMovementEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = productId,
                batchId = batchId,
                quantityDelta = quantityDelta,
                reason = reason.name,
                referenceType = "BATCH_ADJUSTMENT",
                referenceId = batchId,
                createdAt = now
            )
        )
    }

    @Transaction
    open suspend fun receiveStock(
        storeId: String,
        productId: String,
        quantity: Double,
        batchNumber: String?,
        expiryDate: Long?,
        purchasePrice: Double,
        now: Long = System.currentTimeMillis()
    ) {
        require(quantity > 0) { "Received quantity must be greater than zero" }
        require(purchasePrice >= 0) { "Purchase price cannot be negative" }
        require(expiryDate == null || expiryDate >= now) { "Expiry date cannot be in the past" }

        val updated = updateProductStock(storeId, productId, quantity, now)
        check(updated == 1) { "Product could not be updated" }

        insertBatch(
            InventoryBatchEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = productId,
                batchNumber = batchNumber?.trim()?.ifBlank { null },
                expiryDate = expiryDate,
                quantity = quantity,
                purchasePrice = purchasePrice,
                createdAt = now
            )
        )

        insertMovement(
            InventoryMovementEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = productId,
                quantityDelta = quantity,
                reason = InventoryMovementReason.RECEIVE.name,
                referenceType = "RECEIVING",
                referenceId = null,
                createdAt = now
            )
        )
    }
}
