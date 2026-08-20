package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProductIdentificationFeedbackDao {
    @Insert
    suspend fun insert(event: ProductIdentificationFeedbackEntity)

    @Query("SELECT * FROM product_identification_feedback WHERE storeId = :storeId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(storeId: String, limit: Int = 100): List<ProductIdentificationFeedbackEntity>

    @Query("SELECT COALESCE(SUM(rankingBoost), 0) FROM product_identification_feedback WHERE storeId = :storeId AND candidateKey = :candidateKey")
    suspend fun rankingBoostForCandidate(storeId: String, candidateKey: String): Int
}
