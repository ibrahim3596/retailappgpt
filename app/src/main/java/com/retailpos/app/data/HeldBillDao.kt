package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class HeldBillDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertBill(bill: HeldBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLines(lines: List<HeldBillLineEntity>)

    @Query("SELECT * FROM held_bills WHERE storeId = :storeId ORDER BY updatedAt DESC")
    abstract suspend fun getAll(storeId: String): List<HeldBillEntity>

    @Query("SELECT * FROM held_bill_lines WHERE heldBillId = :heldBillId ORDER BY productId")
    abstract suspend fun getLines(heldBillId: String): List<HeldBillLineEntity>

    @Query("DELETE FROM held_bill_lines WHERE heldBillId = :heldBillId")
    abstract suspend fun deleteLines(heldBillId: String)

    @Query("DELETE FROM held_bills WHERE id = :heldBillId AND storeId = :storeId")
    abstract suspend fun deleteBill(heldBillId: String, storeId: String)

    @Query("SELECT COUNT(*) FROM held_bills WHERE storeId = :storeId")
    abstract suspend fun count(storeId: String): Int

    @Transaction
    open suspend fun deleteWithLines(heldBillId: String, storeId: String) {
        deleteLines(heldBillId)
        deleteBill(heldBillId, storeId)
    }
}
