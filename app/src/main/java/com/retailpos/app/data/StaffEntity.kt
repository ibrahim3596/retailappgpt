package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff",
    indices = [Index(value = ["storeId", "username"], unique = true)]
)
data class StaffEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val username: String,
    val role: String,
    val pinHash: String,
    val active: Boolean = true,
    val failedPinAttempts: Int = 0,
    val lockedUntil: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
