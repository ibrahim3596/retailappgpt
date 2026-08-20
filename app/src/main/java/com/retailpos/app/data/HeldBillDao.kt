package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class HeldBillDao {
    @Query("SELECT * FROM held_bills WHERE storeId = :storeId ORDER BY createdAt DESC")
    abstract suspend fun getAll(storeId: String): List<HeldBillEntity>

    @Query("SELECT * FROM held_bill_lines WHERE heldBillId = :heldBillId ORDER BY rowid")
    abstract suspend fun getLines(heldBillId: String): List<HeldBillLineEntity>

    @Insert
    abstract suspend fun insertBill(bill: HeldBillEntity)

    @Insert
    abstract suspend fun insertLines(lines: List<HeldBillLineEntity>)

    @Query("DELETE FROM held_bill_lines WHERE heldBillId = :heldBillId")
    abstract suspend fun deleteLines(heldBillId: String)

    @Query("DELETE FROM held_bills WHERE id = :heldBillId AND storeId = :storeId")
    abstract suspend fun deleteBill(heldBillId: String, storeId: String): Int

    @Transaction
    open suspend fun save(bill: HeldBillEntity, lines: List<HeldBillLineEntity>) {
        require(lines.isNotEmpty()) { "Cannot save an empty held bill" }
        require(lines.all { it.quantity.isFinite() && it.quantity > 0.0 }) { "Held bill contains invalid quantity" }
        insertBill(bill)
        insertLines(lines)
    }
}
