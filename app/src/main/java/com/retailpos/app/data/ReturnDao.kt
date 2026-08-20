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
}
