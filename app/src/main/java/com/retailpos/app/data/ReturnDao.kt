package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
abstract class ReturnDao {
    @Insert abstract suspend fun insert(returnEntity: ReturnEntity)
    @Insert abstract suspend fun insertLines(lines: List<ReturnLineEntity>)

    @Query("SELECT COALESCE(SUM(quantity), 0.0) FROM return_lines WHERE saleLineId = :saleLineId")
    abstract suspend fun alreadyReturnedQuantity(saleLineId: String): Double

    @Query("SELECT * FROM returns WHERE storeId = :storeId ORDER BY createdAt DESC LIMIT :limit")
    abstract suspend fun recent(storeId: String, limit: Int): List<ReturnEntity>

    @Query("SELECT * FROM return_lines WHERE returnId = :returnId")
    abstract suspend fun getLines(returnId: String): List<ReturnLineEntity>

    @Query("SELECT COALESCE(SUM(refundTotal), 0.0) FROM returns WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end")
    abstract suspend fun getRefundTotal(storeId: String, start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(rl.quantity), 0.0) FROM return_lines rl INNER JOIN returns r ON r.id = rl.returnId WHERE r.storeId = :storeId AND r.createdAt >= :start AND r.createdAt < :end")
    abstract suspend fun getReturnedItemsTotal(storeId: String, start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(rl.restoredCost), 0.0) FROM return_lines rl INNER JOIN returns r ON r.id = rl.returnId WHERE r.storeId = :storeId AND r.createdAt >= :start AND r.createdAt < :end")
    abstract suspend fun getRestoredCostTotal(storeId: String, start: Long, end: Long): Double

    @Query("SELECT refundMethod AS refundMethod, COALESCE(SUM(refundTotal), 0.0) AS total FROM returns WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end GROUP BY refundMethod")
    abstract suspend fun getRefundSummary(storeId: String, start: Long, end: Long): List<RefundSummary>

    @Query("SELECT rl.productId AS productId, COALESCE(SUM(rl.quantity), 0.0) AS quantity, COALESCE(SUM(rl.refundAmount), 0.0) AS revenue FROM return_lines rl INNER JOIN returns r ON r.id = rl.returnId WHERE r.storeId = :storeId AND r.createdAt >= :start AND r.createdAt < :end GROUP BY rl.productId")
    abstract suspend fun getReturnedProducts(storeId: String, start: Long, end: Long): List<ReturnedProductSummary>
}

data class RefundSummary(
    val refundMethod: String,
    val total: Double
)

data class ReturnedProductSummary(
    val productId: String,
    val quantity: Double,
    val revenue: Double
)
