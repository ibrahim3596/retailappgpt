package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM customer_ledger WHERE storeId = :storeId AND customerId = :customerId")
    fun observeBalance(storeId: String, customerId: String): Flow<Double>

    @Query("SELECT * FROM customer_ledger WHERE storeId = :storeId AND customerId = :customerId ORDER BY createdAt DESC")
    fun observeEntries(storeId: String, customerId: String): Flow<List<CustomerLedgerEntry>>

    @Insert
    suspend fun insert(entry: CustomerLedgerEntry)

    @Query("SELECT * FROM customer_ledger WHERE storeId = :storeId AND referenceType = :referenceType AND referenceId = :referenceId LIMIT 1")
    suspend fun findByReference(storeId: String, referenceType: String, referenceId: String): CustomerLedgerEntry?
}
