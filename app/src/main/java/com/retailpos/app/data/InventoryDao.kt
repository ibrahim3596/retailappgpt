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
}
