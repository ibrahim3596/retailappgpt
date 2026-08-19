package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE storeId = :storeId ORDER BY name COLLATE NOCASE")
    fun observeAll(storeId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name COLLATE NOCASE")
    fun search(storeId: String, query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId AND storeId = :storeId LIMIT 1")
    suspend fun getById(customerId: String, storeId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND phone = :phone LIMIT 1")
    suspend fun getByPhone(storeId: String, phone: String): CustomerEntity?

    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :customerId AND storeId = :storeId")
    suspend fun delete(customerId: String, storeId: String)
}
