package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductMetadataDao {
    @Query("SELECT * FROM product_metadata WHERE productId = :productId AND storeId = :storeId LIMIT 1")
    suspend fun get(productId: String, storeId: String): ProductMetadataEntity?

    @Query("SELECT * FROM product_metadata WHERE productId = :productId AND storeId = :storeId LIMIT 1")
    fun observe(productId: String, storeId: String): Flow<ProductMetadataEntity?>

    @Query("SELECT DISTINCT category FROM product_metadata WHERE storeId = :storeId AND category <> '' ORDER BY category COLLATE NOCASE")
    fun observeCategories(storeId: String): Flow<List<String>>

    @Upsert
    suspend fun upsert(metadata: ProductMetadataEntity)

    @Query("DELETE FROM product_metadata WHERE productId = :productId AND storeId = :storeId")
    suspend fun delete(productId: String, storeId: String)
}
