package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE storeId = :storeId LIMIT 1")
    suspend fun get(storeId: String): StoreSettingsEntity?

    @Upsert
    suspend fun upsert(settings: StoreSettingsEntity)
}
