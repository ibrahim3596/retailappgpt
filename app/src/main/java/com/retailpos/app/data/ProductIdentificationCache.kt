package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(
    tableName = "product_identification_cache",
    primaryKeys = ["storeId", "barcode"],
    indices = [Index(value = ["storeId", "updatedAt"])]
)
data class ProductIdentificationCacheEntity(
    val storeId: String,
    val barcode: String,
    val name: String?,
    val brand: String?,
    val category: String?,
    val quantity: String?,
    val imageUrl: String?,
    val source: String,
    val confidence: Int,
    val updatedAt: Long
)

@Dao
interface ProductIdentificationCacheDao {
    @Query("SELECT * FROM product_identification_cache WHERE storeId = :storeId AND barcode = :barcode LIMIT 1")
    suspend fun get(storeId: String, barcode: String): ProductIdentificationCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ProductIdentificationCacheEntity)
}

fun CatalogProduct.toCacheEntity(storeId: String, barcode: String, source: String, confidence: Int, now: Long = System.currentTimeMillis()): ProductIdentificationCacheEntity =
    ProductIdentificationCacheEntity(storeId, barcode, name, brand, category, quantity, imageUrl, source, confidence, now)

fun ProductIdentificationCacheEntity.toCatalogProduct(): CatalogProduct =
    CatalogProduct(name = name, brand = brand, category = category, quantity = quantity, imageUrl = imageUrl)
