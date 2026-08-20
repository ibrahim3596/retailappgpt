package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.core.products.PricingInput
import com.retailpos.app.core.products.PricingRules
import com.retailpos.app.core.products.StoreTaxMode
import com.retailpos.app.core.products.TaxTreatment
import com.retailpos.app.core.products.toTaxTreatment
import com.retailpos.app.core.staff.StaffSessionStore
import java.util.UUID

@Dao
abstract class SaleDao {
    @Insert abstract suspend fun insertSale(sale: SaleEntity)
    @Insert abstract suspend fun insertLines(lines: List<SaleLineEntity>)
    @Insert abstract suspend fun insertInventoryMovements(movements: List<InventoryMovementEntity>)
    @Insert abstract suspend fun insertLedgerEntry(entry: CustomerLedgerEntry)

    @Query("SELECT * FROM sales WHERE storeId = :storeId AND idempotencyKey = :idempotencyKey LIMIT 1")
    abstract suspend fun findByIdempotencyKey(storeId: String, idempotencyKey: String): SaleEntity?
    @Query("SELECT * FROM sales WHERE id = :saleId AND storeId = :storeId LIMIT 1")
    abstract suspend fun getSale(storeId: String, saleId: String): SaleEntity?
    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
    abstract suspend fun getSaleLines(saleId: String): List<SaleLineEntity>
    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end")
    abstract suspend fun getSalesTotal(storeId: String, start: Long, end: Long): Double
    @Query("SELECT COUNT(*) FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end")
    abstract suspend fun getSalesCount(storeId: String, start: Long, end: Long): Int
    @Query("SELECT COALESCE(SUM(sl.quantity), 0) FROM sale_lines sl INNER JOIN sales s ON s.id = sl.saleId WHERE s.storeId = :storeId AND s.createdAt >= :start AND s.createdAt < :end")
    abstract suspend fun getItemsSold(storeId: String, start: Long, end: Long): Double
    @Query("SELECT paymentMethod, COUNT(*) AS transactionCount, COALESCE(SUM(total), 0) AS total FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end GROUP BY paymentMethod ORDER BY total DESC")
    abstract suspend fun getPaymentSummary(storeId: String, start: Long, end: Long): List<PaymentSummary>
    @Query("SELECT * FROM sales WHERE storeId = :storeId ORDER BY createdAt DESC LIMIT :limit")
    abstract suspend fun getRecentSales(storeId: String, limit: Int): List<SaleEntity>
    @Query("SELECT * FROM product_metadata WHERE productId = :productId AND storeId = :storeId LIMIT 1")
    abstract suspend fun getProductMetadata(productId: String, storeId: String): ProductMetadataEntity?
    @Query("SELECT * FROM store_settings WHERE storeId = :storeId LIMIT 1")
    abstract suspend fun getStoreSettings(storeId: String): StoreSettingsEntity?
    @Query("UPDATE products SET stock = stock - :quantity, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId AND stock >= :quantity")
    abstract suspend fun decrementStock(productId: String, storeId: String, quantity: Double, updatedAt: Long): Int
    @Query("SELECT * FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0 AND (expiryDate IS NULL OR expiryDate > :now) ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, createdAt ASC")
    abstract suspend fun getAvailableBatchesFefo(storeId: String, productId: String, now: Long): List<InventoryBatchEntity>
    @Query("SELECT EXISTS(SELECT 1 FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0)")
    abstract suspend fun hasPositiveBatchStock(storeId: String, productId: String): Boolean
    @Query("UPDATE inventory_batches SET quantity = quantity - :quantity WHERE id = :batchId AND storeId = :storeId AND quantity >= :quantity")
    abstract suspend fun decrementBatch(batchId: String, storeId: String, quantity: Double): Int

    private suspend fun allocateFefo(storeId: String, productId: String, requiredQuantity: Double, saleId: String, now: Long): Boolean {
        var remaining = requiredQuantity
        val batches = getAvailableBatchesFefo(storeId, productId, now)
        if (batches.isEmpty()) {
            check(!hasPositiveBatchStock(storeId, productId)) { "Only expired batch stock is available" }
            return false
        }
        val movements = mutableListOf<InventoryMovementEntity>()
        for (batch in batches) {
            if (remaining <= 0.0) break
            val allocated = minOf(remaining, batch.quantity)
            check(decrementBatch(batch.id, storeId, allocated) == 1) { "Batch stock changed during checkout" }
            movements += InventoryMovementEntity(UUID.randomUUID().toString(), storeId, productId, batch.id, -allocated, InventoryMovementReason.SALE.name, "SALE", saleId, now)
            remaining -= allocated
        }
        check(remaining <= 0.0) { "Insufficient unexpired batch stock" }
        insertInventoryMovements(movements)
        return true
    }

    @Transaction
    open suspend fun checkout(
        storeId: String,
        cart: List<CartLine>,
        paymentMethod: String,
        idempotencyKey: String,
        customerId: String? = null,
        now: Long = System.currentTimeMillis(),
        taxTreatment: TaxTreatment? = null,
        billDiscountAmount: Double = 0.0,
        staffRole: StaffRole = StaffSessionStore.current()?.role ?: StaffRole.CASHIER
    ): CheckoutResult {
        require(CheckoutRules.validateCart(cart)) { "Invalid cart" }
        require(CheckoutRules.validatePaymentMethod(paymentMethod)) { "Unsupported payment method" }
        require(CheckoutRules.validateIdempotencyKey(idempotencyKey)) { "Missing checkout idempotency key" }
        require(paymentMethod != "CREDIT" || !customerId.isNullOrBlank()) { "Select a customer for credit sales" }
        findByIdempotencyKey(storeId, idempotencyKey)?.let { return CheckoutResult(it.id, it.total) }

        val subtotal = cart.sumOf { it.lineTotal }
        StaffPermissionRules.validateBillDiscount(staffRole, subtotal, billDiscountAmount)?.let { error ->
            throw IllegalArgumentException(error)
        }

        val effectiveTaxTreatment = taxTreatment ?: StoreTaxMode.fromStorage(getStoreSettings(storeId)?.gstMode ?: StoreTaxMode.NO_GST.storageValue).toTaxTreatment()
        val safeDiscount = PricingRules.calculate(
            PricingInput(subtotal = subtotal, discountAmount = billDiscountAmount, taxTreatment = TaxTreatment.NO_TAX)
        ).discountAmount

        val pricedLines = cart.map { line ->
            val lineDiscount = if (subtotal <= 0.0) 0.0 else safeDiscount * (line.lineTotal / subtotal)
            val productTaxRate = if (effectiveTaxTreatment == TaxTreatment.NO_TAX) 0.0 else (getProductMetadata(line.productId, storeId)?.taxRatePercent ?: 0.0)
            val pricing = PricingRules.calculate(
                PricingInput(
                    subtotal = line.lineTotal,
                    discountAmount = lineDiscount,
                    taxRatePercent = productTaxRate,
                    taxTreatment = effectiveTaxTreatment
                )
            )
            line to pricing
        }

        val saleDiscount = pricedLines.sumOf { it.second.discountAmount }
        val saleTax = pricedLines.sumOf { it.second.taxAmount }
        val saleTotal = pricedLines.sumOf { it.second.total }
        val saleId = UUID.randomUUID().toString()
        val sale = SaleEntity(saleId, storeId, customerId, subtotal, saleDiscount, saleTax, saleTotal, paymentMethod, idempotencyKey, now)
        val fallbackMovements = mutableListOf<InventoryMovementEntity>()
        for (line in cart) {
            val allocatedToBatch = allocateFefo(storeId, line.productId, line.quantity, saleId, now)
            if (allocatedToBatch) check(decrementStock(line.productId, storeId, line.quantity, now) == 1) { "Insufficient product stock for ${line.name}" }
            else {
                check(decrementStock(line.productId, storeId, line.quantity, now) == 1) { "Insufficient stock for ${line.name}" }
                fallbackMovements += InventoryMovementEntity(UUID.randomUUID().toString(), storeId, line.productId, null, -line.quantity, InventoryMovementReason.SALE.name, "SALE", saleId, now)
            }
        }
        if (fallbackMovements.isNotEmpty()) insertInventoryMovements(fallbackMovements)
        insertSale(sale)
        insertLines(pricedLines.map { (line, pricing) ->
            SaleLineEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = line.productId,
                name = line.name,
                sku = line.sku,
                quantity = line.quantity,
                unit = line.unit,
                unitPrice = line.unitPrice,
                taxableAmount = pricing.taxableAmount,
                discountAmount = pricing.discountAmount,
                taxRatePercent = if (effectiveTaxTreatment == TaxTreatment.NO_TAX) 0.0 else (getProductMetadata(line.productId, storeId)?.taxRatePercent ?: 0.0),
                taxAmount = pricing.taxAmount,
                lineTotal = pricing.total
            )
        })
        if (paymentMethod == "CREDIT") {
            insertLedgerEntry(CustomerLedgerEntry(UUID.randomUUID().toString(), storeId, customerId!!, saleTotal, "CREDIT_SALE", "Sale $saleId", "SALE", saleId, now))
        }
        return CheckoutResult(saleId, saleTotal)
    }
}