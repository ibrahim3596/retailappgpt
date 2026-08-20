package com.retailpos.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["storeId", "createdAt"]),
        Index(value = ["storeId", "idempotencyKey"], unique = true),
        Index(value = ["storeId", "customerId", "createdAt"])
    ]
)
data class SaleEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val customerId: String?,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
    val paymentMethod: String,
    val amountTendered: Double?,
    val changeAmount: Double,
    val idempotencyKey: String,
    val createdAt: Long
)

@Entity(
    tableName = "sale_lines",
    indices = [Index(value = ["saleId"]), Index(value = ["productId"])]
)
data class SaleLineEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val name: String,
    val sku: String?,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val taxableAmount: Double,
    val discountAmount: Double,
    val taxRatePercent: Double,
    val taxAmount: Double,
    val lineTotal: Double
)

data class CheckoutResult(
    val saleId: String,
    val total: Double,
    val changeAmount: Double
)

data class PaymentSummary(
    val paymentMethod: String,
    val transactionCount: Int,
    val total: Double
)
