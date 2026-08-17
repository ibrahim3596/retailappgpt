package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InventoryDao {
    @Insert
    suspend fun insertMovement(movement: InventoryMovementEntity)

    @Insert
    suspend fun insertBatch(batch: InventoryBatchEntity)

    @Query("SELECT * FROM inventory_movements WHERE storeId = :storeId ORDER BY createdAt DESC")
    suspend fun getMovements(storeId: String): List<InventoryMovementEntity>

    @Query("SELECT * FROM inventory_movements WHERE storeId = :storeId AND productId = :productId ORDER BY createdAt DESC")
    suspend fun getProductMovements(storeId: String, productId: String): List<InventoryMovementEntity>

    @Query("SELECT * FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0 ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, createdAt ASC")
    suspend fun getAvailableBatchesFefo(storeId: String, productId: String): List<InventoryBatchEntity>
}
