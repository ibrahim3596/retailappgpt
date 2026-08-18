package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["storeId", "phone"], unique = true),
        Index(value = ["storeId", "name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val createdAt: Long,
    val updatedAt: Long
)
