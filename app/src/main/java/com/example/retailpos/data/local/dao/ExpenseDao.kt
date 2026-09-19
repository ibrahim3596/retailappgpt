package com.example.retailpos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.retailpos.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE storeId = :storeId ORDER BY date DESC")
    fun getAllExpenses(storeId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getExpensesForRange(storeId: String, startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND date >= :startTime AND date <= :endTime ORDER BY date DESC")
    suspend fun getExpensesForRangeOnce(storeId: String, startTime: Long, endTime: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE storeId = :storeId AND date >= :startTime AND date <= :endTime")
    suspend fun getTotalForRange(storeId: String, startTime: Long, endTime: Long): Double

    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND id = :id LIMIT 1")
    suspend fun getExpenseById(storeId: String, id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND localId = :localId LIMIT 1")
    suspend fun getExpenseByLocalId(storeId: String, localId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE storeId = :storeId AND syncStatus = 'PENDING' ORDER BY date ASC")
    suspend fun getPendingExpenses(storeId: String): List<ExpenseEntity>

    @Query("UPDATE expenses SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id AND storeId = :storeId")
    suspend fun updateExpenseSyncStatus(id: String, storeId: String, syncStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM expenses WHERE id = :id AND storeId = :storeId AND syncStatus != 'SYNCED'")
    suspend fun deleteUnsyncedExpense(id: String, storeId: String): Int
}
