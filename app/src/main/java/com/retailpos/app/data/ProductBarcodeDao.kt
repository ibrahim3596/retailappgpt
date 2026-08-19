package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductBarcodeDao {
    @Query("SELECT * FROM product_barcodes WHERE productId = :productId AND storeId = :storeId ORDER BY isPrimary DESC, createdAt ASC")
    fun observeForProduct(productId: String, storeId: String): Flow<List<ProductBarcodeEntity>>

    @Query("SELECT * FROM product_barcodes WHERE storeId = :storeId AND value = :value LIMIT 1")
    suspend fun getByValue(storeId: String, value: String): ProductBarcodeEntity?

    @Query("SELECT p.* FROM products p INNER JOIN product_barcodes b ON b.productId = p.id AND b.storeId = p.storeId WHERE b.storeId = :storeId AND b.value = :value LIMIT 1")
    suspend fun getProductByBarcode(storeId: String, value: String): ProductEntity?

    @Query("SELECT * FROM product_barcodes WHERE productId = :productId AND storeId = :storeId AND isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(productId: String, storeId: String): ProductBarcodeEntity?

    @Upsert
    suspend fun upsert(barcode: ProductBarcodeEntity)

    @Query("DELETE FROM product_barcodes WHERE productId = :productId AND storeId = :storeId AND isPrimary = 1")
    suspend fun deletePrimary(productId: String, storeId: String)

    @Query("DELETE FROM product_barcodes WHERE productId = :productId AND storeId = :storeId AND isPrimary = 0")
    suspend fun deleteSecondaryForProduct(productId: String, storeId: String)

    @Query("DELETE FROM product_barcodes WHERE productId = :productId AND storeId = :storeId")
    suspend fun deleteForProduct(productId: String, storeId: String)

    @Query("DELETE FROM product_barcodes WHERE id = :barcodeId AND storeId = :storeId")
    suspend fun delete(barcodeId: String, storeId: String)
}
