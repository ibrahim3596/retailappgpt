package com.example.retailpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerName: String,
    val phone: String,
    val email: String = "",
    val gstin: String = "",
    val address: String = "",
    val stateCode: String = "27", // Default e.g. Maharashtra
    val currencySymbol: String = "₹",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val username: String,
    val fullName: String,
    val role: String = "OWNER",
    val pinHash: String = "",
    val supabaseUserId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
