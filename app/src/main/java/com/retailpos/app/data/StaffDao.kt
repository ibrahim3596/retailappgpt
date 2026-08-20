package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE storeId = :storeId AND username = :username LIMIT 1")
    suspend fun findByUsername(storeId: String, username: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE storeId = :storeId AND id = :staffId LIMIT 1")
    suspend fun getById(storeId: String, staffId: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE storeId = :storeId ORDER BY name COLLATE NOCASE")
    suspend fun list(storeId: String): List<StaffEntity>

    @Upsert
    suspend fun upsert(staff: StaffEntity)

    @Query("UPDATE staff SET failedPinAttempts = :failedAttempts, lockedUntil = :lockedUntil, updatedAt = :updatedAt WHERE id = :staffId AND storeId = :storeId")
    suspend fun updatePinFailures(storeId: String, staffId: String, failedAttempts: Int, lockedUntil: Long?, updatedAt: Long): Int

    @Query("UPDATE staff SET active = :active, updatedAt = :updatedAt WHERE id = :staffId AND storeId = :storeId")
    suspend fun setActive(storeId: String, staffId: String, active: Boolean, updatedAt: Long): Int
}
