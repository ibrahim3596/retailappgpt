package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteProductDao {
    @Query("SELECT productId FROM favorite_products WHERE storeId = :storeId ORDER BY createdAt DESC")
    fun observeIds(storeId: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_products WHERE storeId = :storeId AND productId = :productId)")
    suspend fun isFavorite(storeId: String, productId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entry: FavoriteProductEntity): Long

    @Query("DELETE FROM favorite_products WHERE storeId = :storeId AND productId = :productId")
    suspend fun remove(storeId: String, productId: String): Int

    @Query("SELECT productId FROM favorite_products WHERE storeId = :storeId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentIds(storeId: String, limit: Int = 12): List<String>
}
