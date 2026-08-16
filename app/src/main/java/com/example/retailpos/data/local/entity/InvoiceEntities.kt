package com.example.retailpos.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class PaymentMethod {
    CASH,
    UPI,
    CARD,
    CREDIT
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    CONFLICT
}

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["storeId", "localId"], unique = true),
        Index(value = ["storeId", "invoiceNumber"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val localId: String, // Client-generated idempotent ID
    val storeId: String,
    val invoiceNumber: String,
    val customerId: String? = null,
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val subtotal: Double,
    val totalGst: Double,
    val cgstTotal: Double,
    val sgstTotal: Double,
    val igstTotal: Double = 0.0,
    val discount: Double = 0.0,
    val grandTotal: Double,
    val paymentMethod: PaymentMethod,
    val amountReceived: Double,
    val changeDue: Double,
    val isInterstate: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
)

@Entity(
    tableName = "invoice_items",
    indices = [
        Index(value = ["invoiceId"]),
        Index(value = ["productId"])
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val productId: String,
    val productName: String,
    val batchId: String? = null,
    val barcode: String = "",
    val mrp: Double,
    val sellingPrice: Double,
    val purchasePrice: Double,
    val quantity: Double,
    val gstRate: Double,
    val hsnCode: String = "",
    val cgstAmount: Double,
    val sgstAmount: Double,
    val igstAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxType: TaxType = TaxType.INCLUSIVE,
    val itemTotal: Double
)
