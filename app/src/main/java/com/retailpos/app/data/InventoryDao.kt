package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.retailpos.app.core.inventory.InventoryRules
import java.util.UUID

@Dao
abstract class InventoryDao {
    @Insert abstract suspend fun insertMovement(movement: InventoryMovementEntity)
    @Insert abstract suspend fun insertBatch(batch: InventoryBatchEntity)

    @Query("SELECT * FROM inventory_movements WHERE storeId = :storeId ORDER BY createdAt DESC")
    abstract suspend fun getMovements(storeId: String): List<InventoryMovementEntity>
    @Query("SELECT * FROM inventory_movements WHERE storeId = :storeId AND productId = :productId ORDER BY createdAt DESC")
    abstract suspend fun getProductMovements(storeId: String, productId: String): List<InventoryMovementEntity>
    @Query("SELECT * FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0 ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, createdAt ASC")
    abstract suspend fun getAvailableBatchesFefo(storeId: String, productId: String): List<InventoryBatchEntity>
    @Query("UPDATE products SET stock = stock + :quantityDelta, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId AND stock + :quantityDelta >= 0")
    abstract suspend fun updateProductStock(storeId: String, productId: String, quantityDelta: Double, updatedAt: Long): Int
    @Query("UPDATE inventory_batches SET quantity = quantity + :quantityDelta WHERE id = :batchId AND storeId = :storeId AND productId = :productId AND quantity + :quantityDelta >= 0")
    abstract suspend fun updateBatchQuantity(storeId: String, productId: String, batchId: String, quantityDelta: Double): Int

    @Transaction
    open suspend fun adjustStock(storeId: String, productId: String, quantityDelta: Double, reason: InventoryMovementReason = InventoryMovementReason.ADJUSTMENT, referenceType: String? = null, referenceId: String? = null, now: Long = System.currentTimeMillis()) {
        InventoryRules.validateAdjustment(quantityDelta)?.let(::error)
        require(reason != InventoryMovementReason.SALE) { "Sale stock changes must use checkout" }
        check(updateProductStock(storeId, productId, quantityDelta, now) == 1) { "Stock cannot become negative" }
        insertMovement(InventoryMovementEntity(UUID.randomUUID().toString(), storeId, productId, null, quantityDelta, reason.name, referenceType, referenceId, now))
    }

    @Transaction
    open suspend fun adjustBatchStock(storeId: String, productId: String, batchId: String, quantityDelta: Double, reason: InventoryMovementReason = InventoryMovementReason.ADJUSTMENT, now: Long = System.currentTimeMillis()) {
        InventoryRules.validateAdjustment(quantityDelta)?.let(::error)
        require(reason != InventoryMovementReason.SALE) { "Sale stock changes must use checkout" }
        check(updateProductStock(storeId, productId, quantityDelta, now) == 1) { "Product stock cannot become negative" }
        check(updateBatchQuantity(storeId, productId, batchId, quantityDelta) == 1) { "Batch stock cannot become negative" }
        insertMovement(InventoryMovementEntity(UUID.randomUUID().toString(), storeId, productId, batchId, quantityDelta, reason.name, "BATCH_ADJUSTMENT", batchId, now))
    }

    @Transaction
    open suspend fun receiveStock(storeId: String, productId: String, quantity: Double, batchNumber: String?, expiryDate: Long?, purchasePrice: Double, now: Long = System.currentTimeMillis()) {
        InventoryRules.validateReceive(quantity, purchasePrice, expiryDate, now)?.let(::error)
        check(updateProductStock(storeId, productId, quantity, now) == 1) { "Product could not be updated" }
        insertBatch(InventoryBatchEntity(UUID.randomUUID().toString(), storeId, productId, batchNumber?.trim()?.ifBlank { null }, expiryDate, quantity, purchasePrice, now))
        insertMovement(InventoryMovementEntity(UUID.randomUUID().toString(), storeId, productId, null, quantity, InventoryMovementReason.RECEIVE.name, "RECEIVING", null, now))
    }
}
