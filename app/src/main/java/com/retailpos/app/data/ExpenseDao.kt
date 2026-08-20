package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE storeId = :storeId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(storeId: String, limit: Int = 100): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end")
    suspend fun totalBetween(storeId: String, start: Long, end: Long): Double
}
