package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE storeId = :storeId ORDER BY name COLLATE NOCASE")
    fun observeProducts(storeId: String): Flow<List<ProductEntity>>

    /**
     * Product identifiers are resolved through product_barcodes. The legacy
     * ProductEntity.barcode field remains as a migration/back-compat mirror,
     * but is not treated as the authoritative searchable barcode source.
     */
    @Query("SELECT DISTINCT p.* FROM products p WHERE p.storeId = :storeId AND (p.name LIKE '%' || :query || '%' OR p.brand LIKE '%' || :query || '%' OR p.sku LIKE '%' || :query || '%' OR EXISTS (SELECT 1 FROM product_barcodes b WHERE b.productId = p.id AND b.storeId = p.storeId AND b.value LIKE '%' || :query || '%')) ORDER BY p.name COLLATE NOCASE")
    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId AND storeId = :storeId LIMIT 1")
    suspend fun getById(productId: String, storeId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE storeId = :storeId AND sku = :sku LIMIT 1")
    suspend fun getBySku(storeId: String, sku: String): ProductEntity?

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId AND storeId = :storeId")
    suspend fun delete(productId: String, storeId: String)
}
