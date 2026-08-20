package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey val storeId: String,
    val gstMode: String = "NO_GST",
    val defaultTaxRatePercent: Double = 0.0,
    val currency: String = "INR",
    val updatedAt: Long
)
