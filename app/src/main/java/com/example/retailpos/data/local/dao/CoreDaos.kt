package com.example.retailpos.data.local.dao

import androidx.room.*
import com.example.retailpos.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores WHERE id = :storeId LIMIT 1")
    fun getStoreFlow(storeId: String): Flow<StoreEntity?>

    @Query("SELECT * FROM stores WHERE id = :storeId LIMIT 1")
    suspend fun getStore(storeId: String): StoreEntity?

    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<StoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStore(store: StoreEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username AND storeId = :storeId LIMIT 1")
    suspend fun getUserByUsername(username: String, storeId: String): UserEntity?

    @Query("SELECT * FROM users WHERE supabaseUserId = :supabaseId LIMIT 1")
    suspend fun getUserBySupabaseId(supabaseId: String): UserEntity?

    @Query("SELECT * FROM users WHERE storeId = :storeId ORDER BY fullName ASC")
    fun getAllUsers(storeId: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE storeId = :storeId AND isActive = 1 ORDER BY name ASC")
    fun getAllProducts(storeId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE storeId = :storeId AND id = :id LIMIT 1")
    suspend fun getProductById(storeId: String, id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE storeId = :storeId AND (barcode = :barcode OR normalizedBarcode = :normalizedBarcode) LIMIT 1")
    suspend fun getProductByBarcode(storeId: String, barcode: String, normalizedBarcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE storeId = :storeId AND sku = :sku LIMIT 1")
    suspend fun getProductBySku(storeId: String, sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE storeId = :storeId AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') AND isActive = 1 LIMIT 50")
    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET currentStock = currentStock - :quantity, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId AND currentStock >= :quantity")
    suspend fun atomicDeductStock(productId: String, storeId: String, quantity: Double, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE products SET currentStock = currentStock + :quantity, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId")
    suspend fun atomicAddStock(productId: String, storeId: String, quantity: Double, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM products WHERE storeId = :storeId AND currentStock <= minStock AND isActive = 1")
    fun getLowStockProducts(storeId: String): Flow<List<ProductEntity>>
}

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE storeId = :storeId AND productId = :productId AND remainingQty > 0 ORDER BY expiryDate ASC, createdAt ASC")
    suspend fun getFefoBatchesForProduct(storeId: String, productId: String): List<BatchEntity>

    @Query("SELECT * FROM batches WHERE storeId = :storeId AND productId = :productId ORDER BY expiryDate ASC")
    fun getBatchesForProductFlow(storeId: String, productId: String): Flow<List<BatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity)

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Query("UPDATE batches SET remainingQty = remainingQty - :deductQty WHERE id = :batchId AND remainingQty >= :deductQty")
    suspend fun atomicDeductBatchQty(batchId: String, deductQty: Double): Int

    @Query("SELECT * FROM batches WHERE storeId = :storeId AND expiryDate <= :thresholdTime AND remainingQty > 0 ORDER BY expiryDate ASC")
    fun getExpiringBatches(storeId: String, thresholdTime: Long): Flow<List<BatchEntity>>
}
