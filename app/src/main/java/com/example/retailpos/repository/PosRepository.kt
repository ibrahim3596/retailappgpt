package com.example.retailpos.repository

import androidx.room.withTransaction
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.*
import com.example.retailpos.engine.fefo.FefoAllocationEngine
import com.example.retailpos.engine.gst.GstBreakdown
import com.example.retailpos.engine.gst.GstCalculator
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class CartItem(
    val product: ProductEntity,
    val selectedBatch: BatchEntity? = null,
    val quantity: Double,
    val discountAmount: Double = 0.0,
    val overridePrice: Double? = null
) {
    val effectivePrice: Double get() = overridePrice ?: product.sellingPrice

    fun calculateGst(isInterstate: Boolean): GstBreakdown {
        return GstCalculator.calculateItemGst(
            sellingPrice = effectivePrice,
            quantity = quantity,
            gstRate = product.gstRate,
            isTaxInclusive = product.taxType == TaxType.INCLUSIVE,
            discountAmount = discountAmount,
            isInterstate = isInterstate
        )
    }
}

class PosRepository(private val db: AppDatabase) {

    private val fefoEngine = FefoAllocationEngine(
        productDao = db.productDao(),
        batchDao = db.batchDao(),
        stockMovementDao = db.stockMovementDao()
    )

    fun getProducts(storeId: String): Flow<List<ProductEntity>> = db.productDao().getAllProducts(storeId)

    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>> =
        db.productDao().searchProducts(storeId, query)

    suspend fun getProductByBarcode(storeId: String, barcode: String): ProductEntity? {
        return db.productDao().getProductByBarcode(storeId, barcode, barcode)
    }

    /**
     * Transactional Offline/Online Billing execution using atomic FEFO and GST
     */
    suspend fun createInvoice(
        storeId: String,
        customer: CustomerEntity?,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        amountReceived: Double,
        overallDiscount: Double = 0.0,
        isInterstate: Boolean = false
    ): InvoiceEntity = db.withTransaction {
        if (cartItems.isEmpty()) throw IllegalArgumentException("Cart is empty")

        val invoiceId = UUID.randomUUID().toString()
        val localId = "INV-LOC-" + UUID.randomUUID().toString().take(10)
        val invoiceCount = db.invoiceDao().getInvoiceCount(storeId)
        val invoiceNumber = "INV-${(invoiceCount + 1).toString().padStart(5, '0')}"

        var subtotal = 0.0
        var totalGst = 0.0
        var cgstTotal = 0.0
        var sgstTotal = 0.0
        var igstTotal = 0.0

        val invoiceItems = mutableListOf<InvoiceItemEntity>()

        // Process each cart item with FEFO allocation
        for (item in cartItems) {
            val gst = item.calculateGst(isInterstate)

            subtotal += gst.assessableValue
            totalGst += gst.totalGst
            cgstTotal += gst.cgstAmount
            sgstTotal += gst.sgstAmount
            igstTotal += gst.igstAmount

            // Allocate and deduct stock using FEFO
            val deductions = fefoEngine.allocateAndDeductFefo(
                storeId = storeId,
                productId = item.product.id,
                requestedQty = item.quantity,
                referenceId = invoiceId,
                createdBy = "POS"
            )

            val primaryBatchId = deductions.firstOrNull()?.batchId ?: item.selectedBatch?.id

            val invoiceItem = InvoiceItemEntity(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                productId = item.product.id,
                productName = item.product.name,
                batchId = primaryBatchId,
                barcode = item.product.barcode,
                mrp = item.product.mrp,
                sellingPrice = item.effectivePrice,
                purchasePrice = item.product.purchasePrice,
                quantity = item.quantity,
                gstRate = item.product.gstRate,
                hsnCode = item.product.hsnCode,
                cgstAmount = gst.cgstAmount,
                sgstAmount = gst.sgstAmount,
                igstAmount = gst.igstAmount,
                discountAmount = item.discountAmount,
                taxType = item.product.taxType,
                itemTotal = gst.finalLineTotal
            )
            invoiceItems.add(invoiceItem)
        }

        val grandTotal = (subtotal + totalGst - overallDiscount).coerceAtLeast(0.0)
        val changeDue = (amountReceived - grandTotal).coerceAtLeast(0.0)

        val invoice = InvoiceEntity(
            id = invoiceId,
            localId = localId,
            storeId = storeId,
            invoiceNumber = invoiceNumber,
            customerId = customer?.id,
            customerName = customer?.name ?: "Walk-in Customer",
            customerPhone = customer?.phone ?: "",
            subtotal = subtotal,
            totalGst = totalGst,
            cgstTotal = cgstTotal,
            sgstTotal = sgstTotal,
            igstTotal = igstTotal,
            discount = overallDiscount,
            grandTotal = grandTotal,
            paymentMethod = paymentMethod,
            amountReceived = amountReceived,
            changeDue = changeDue,
            isInterstate = isInterstate,
            syncStatus = SyncStatus.PENDING
        )

        // Save Invoice and Invoice Items
        db.invoiceDao().insertInvoice(invoice)
        db.invoiceItemDao().insertInvoiceItems(invoiceItems)

        // If payment method is CREDIT, record customer khata debt
        if (paymentMethod == PaymentMethod.CREDIT && customer != null) {
            val newBalance = customer.currentBalance + grandTotal
            db.customerDao().updateBalance(customer.id, storeId, grandTotal)

            val ledgerEntry = CreditLedgerEntryEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                customerId = customer.id,
                type = LedgerEntryType.DEBIT,
                amount = grandTotal,
                balanceAfter = newBalance,
                referenceId = invoiceId,
                notes = "Credit Sale - Invoice $invoiceNumber"
            )
            db.creditLedgerDao().insertLedgerEntry(ledgerEntry)
        }

        invoice
    }
}
