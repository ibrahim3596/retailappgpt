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

        // Only credit sales may be received short; anything else is a till error.
        if (paymentMethod != PaymentMethod.CREDIT && amountReceived + 1e-9 < grandTotal) {
            throw IllegalArgumentException("Amount received is less than the bill total")
        }

        // Credit sales require a customer and must respect the khata limit.
        if (paymentMethod == PaymentMethod.CREDIT) {
            if (customer == null) {
                throw IllegalArgumentException("Credit sale requires a customer")
            }
            val projected = customer.currentBalance + grandTotal
            if (projected > customer.creditLimit + 1e-9) {
                throw IllegalStateException("Credit limit exceeded for ${customer.name}")
            }
        }

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

    /**
     * A unit being returned against a specific invoice item. Quantity is the
     * additional amount being returned this time, NOT the cumulative total.
     */
    data class ReturnItem(
        val invoiceItemId: String,
        val quantity: Double
    )

    /**
     * Processes a full or partial return against an existing invoice:
     *  - validates every item is from the invoice and the cumulative returned
     *    quantity never exceeds the sold quantity (so retries cannot over-return)
     *  - restores product stock (and batch stock where the item was
     *    batch-allocated) with an auditable RETURN movement
     *  - reverses khata debt for credit sales via an immutable CREDIT ledger row
     *  - marks the invoice CANCELLED only on a full return, preserving history
     *
     * @return the refund amount, or null when the return was rejected.
     */
    suspend fun processReturn(
        storeId: String,
        invoiceId: String,
        items: List<ReturnItem>,
        reason: String
    ): Double? = db.withTransaction {
        if (items.isEmpty()) return@withTransaction null

        val invoice = db.invoiceDao().getInvoiceById(storeId, invoiceId)
            ?: return@withTransaction null
        // A fully-returned invoice is already cancelled; refusing it again
        // protects against duplicate refund submissions.
        if (invoice.status == "CANCELLED") return@withTransaction null

        val invoiceItems = db.invoiceItemDao().getInvoiceItems(invoiceId)
            .associateBy { it.id }

        // Validate first, before touching any state.
        var refundTotal = 0.0
        var anyRefund = false
        for (ret in items) {
            val orig = invoiceItems[ret.invoiceItemId] ?: return@withTransaction null
            if (ret.quantity <= 0.0) return@withTransaction null

            val alreadyReturned = db.invoiceItemDao().getReturnedQty(orig.id)
            // Cumulative returns can never exceed the sold quantity, so a
            // retry or duplicate submission cannot over-return.
            if (alreadyReturned + ret.quantity > orig.quantity + 1e-9) {
                return@withTransaction null
            }

            // Refund at the price actually charged (per-unit line total).
            val pricePerUnit = if (orig.quantity > 0) orig.itemTotal / orig.quantity else 0.0
            refundTotal += pricePerUnit * ret.quantity
            anyRefund = true
        }
        if (!anyRefund) return@withTransaction null

        // Apply stock restoration + per-item return records.
        for (ret in items) {
            val orig = invoiceItems[ret.invoiceItemId] ?: continue

            // Product-level restoration (guarded so stock can't go negative).
            db.productDao().atomicAddStock(orig.productId, storeId, ret.quantity)

            // Batch-level restoration when the item was allocated from a batch.
            val product = db.productDao().getProductById(storeId, orig.productId)
            if (orig.batchId != null) {
                db.batchDao().atomicAddBatchQty(orig.batchId, ret.quantity)
            }
            val movement = StockMovementEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = orig.productId,
                batchId = orig.batchId,
                type = StockMovementType.RETURN,
                quantity = ret.quantity,
                balanceAfter = product?.currentStock ?: 0.0,
                referenceId = invoiceId,
                notes = "Return against ${invoice.invoiceNumber}: $reason"
            )
            db.stockMovementDao().insertStockMovement(movement)

            // Record the returned quantity on the item so cumulative returns
            // are validated against the sold quantity on every attempt.
            db.invoiceItemDao().incrementReturnedQty(orig.id, ret.quantity)
        }

        // Reverse khata debt for credit sales — ledger-driven so history stays immutable.
        if (invoice.paymentMethod == PaymentMethod.CREDIT && invoice.customerId != null) {
            val customer = db.customerDao().getCustomerById(storeId, invoice.customerId)
            if (customer != null) {
                val newBalance = (customer.currentBalance - refundTotal).coerceAtLeast(0.0)
                db.customerDao().updateBalance(invoice.customerId, storeId, -refundTotal)
                db.creditLedgerDao().insertLedgerEntry(
                    CreditLedgerEntryEntity(
                        id = UUID.randomUUID().toString(),
                        storeId = storeId,
                        customerId = invoice.customerId,
                        type = LedgerEntryType.CREDIT,
                        amount = refundTotal,
                        balanceAfter = newBalance,
                        referenceId = invoiceId,
                        notes = "Return refund - Invoice ${invoice.invoiceNumber}: $reason"
                    )
                )
            }
        }

        // Full return = every item fully returned → cancel the invoice,
        // keeping all rows for audit. Partial returns leave it COMPLETED.
        val fullyReturned = invoiceItems.all { orig ->
            db.invoiceItemDao().getReturnedQty(orig.id) >= orig.quantity - 1e-9
        }
        if (fullyReturned) {
            db.invoiceDao().markInvoiceCancelled(invoiceId, storeId)
        }

        refundTotal
    }
}
