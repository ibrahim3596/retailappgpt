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
     * ProductEntity.barcode field remains a migration/back-compat mirror,
     * but is not treated as the authoritative searchable barcode source.
     */
    @Query("SELECT DISTINCT p.* FROM products p WHERE p.storeId = :storeId AND (p.name LIKE '%' || :query || '%' OR p.brand LIKE '%' || :query || '%' OR p.sku LIKE '%' || :query || '%' OR EXISTS (SELECT 1 FROM product_barcodes b WHERE b.productId = p.id AND b.storeId = p.storeId AND b.value LIKE '%' || :query || '%')) ORDER BY p.name COLLATE NOCASE")
    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>>

    @Query("SELECT DISTINCT p.* FROM products p WHERE p.storeId = :storeId AND (LOWER(p.name) LIKE '%' || LOWER(:query) || '%' OR LOWER(p.brand) LIKE '%' || LOWER(:query) || '%') ORDER BY p.name COLLATE NOCASE LIMIT :limit")
    suspend fun findLocalCandidates(storeId: String, query: String, limit: Int = 30): List<ProductEntity>

    @Query("SELECT p.* FROM products p JOIN (SELECT sl.productId AS productId, MAX(s.createdAt) AS lastSold FROM sale_lines sl JOIN sales s ON s.id = sl.saleId WHERE s.storeId = :storeId GROUP BY sl.productId ORDER BY lastSold DESC LIMIT :limit) recent ON recent.productId = p.id WHERE p.storeId = :storeId ORDER BY recent.lastSold DESC")
    fun observeRecentlySold(storeId: String, limit: Int = 12): Flow<List<ProductEntity>>

    @Query("SELECT p.* FROM products p INNER JOIN favorite_products f ON f.productId = p.id AND f.storeId = p.storeId WHERE p.storeId = :storeId ORDER BY f.createdAt DESC, p.name COLLATE NOCASE")
    fun observeFavoriteProducts(storeId: String): Flow<List<ProductEntity>>

    @Upsert
    suspend fun addFavorite(favorite: FavoriteProductEntity)

    @Query("DELETE FROM favorite_products WHERE storeId = :storeId AND productId = :productId")
    suspend fun removeFavorite(storeId: String, productId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_products WHERE storeId = :storeId AND productId = :productId)")
    suspend fun isFavorite(storeId: String, productId: String): Boolean

    @Query("SELECT * FROM products WHERE id = :productId AND storeId = :storeId LIMIT 1")
    suspend fun getById(productId: String, storeId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE storeId = :storeId AND sku = :sku LIMIT 1")
    suspend fun getBySku(storeId: String, sku: String): ProductEntity?

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId AND storeId = :storeId")
    suspend fun delete(productId: String, storeId: String)
}
