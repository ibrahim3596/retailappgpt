package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE storeId = :storeId ORDER BY name COLLATE NOCASE")
    fun observeProducts(storeId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE storeId = :storeId AND (name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%') ORDER BY name COLLATE NOCASE")
    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>>

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId AND storeId = :storeId")
    suspend fun delete(productId: String, storeId: String)
}
