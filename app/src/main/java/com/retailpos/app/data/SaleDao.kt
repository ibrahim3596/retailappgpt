package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.retailpos.app.core.payment.PaymentSettlementRules
import com.retailpos.app.core.payment.PendingPaymentStore
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
    @Insert abstract suspend fun insertCostAllocations(allocations: List<SaleCostAllocationEntity>)
    @Insert abstract suspend fun insertLedgerEntry(entry: CustomerLedgerEntry)

    @Query("SELECT * FROM sales WHERE storeId = :storeId AND idempotencyKey = :idempotencyKey LIMIT 1")
    abstract suspend fun findByIdempotencyKey(storeId: String, idempotencyKey: String): SaleEntity?
    @Query("SELECT * FROM sales WHERE id = :saleId AND storeId = :storeId LIMIT 1")
    abstract suspend fun getSale(storeId: String, saleId: String): SaleEntity?
    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
    abstract suspend fun getSaleLines(saleId: String): List<SaleLineEntity>
    @Query("SELECT * FROM sale_cost_allocations WHERE saleId = :saleId ORDER BY createdAt ASC")
    abstract suspend fun getSaleCostAllocations(saleId: String): List<SaleCostAllocationEntity>
    @Query("SELECT COALESCE(SUM(totalCost), 0) FROM sale_cost_allocations WHERE saleId IN (SELECT id FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end)")
    abstract suspend fun getCogsTotal(storeId: String, start: Long, end: Long): Double
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
    @Query("SELECT purchasePrice FROM products WHERE id = :productId AND storeId = :storeId LIMIT 1")
    abstract suspend fun getCurrentPurchasePrice(productId: String, storeId: String): Double?
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

    private data class CostDraft(val quantity: Double, val unitCost: Double, val totalCost: Double, val batchId: String?)

    private suspend fun allocateFefo(storeId: String, productId: String, requiredQuantity: Double, now: Long): List<CostDraft> {
        var remaining = requiredQuantity
        val batches = getAvailableBatchesFefo(storeId, productId, now)
        if (batches.isEmpty()) {
            check(!hasPositiveBatchStock(storeId, productId)) { "Only expired batch stock is available" }
            val fallbackCost = getCurrentPurchasePrice(productId, storeId) ?: 0.0
            return listOf(CostDraft(requiredQuantity, fallbackCost, requiredQuantity * fallbackCost, null))
        }
        val drafts = mutableListOf<CostDraft>()
        for (batch in batches) {
            if (remaining <= 0.0) break
            val allocated = minOf(remaining, batch.quantity)
            check(decrementBatch(batch.id, storeId, allocated) == 1) { "Batch stock changed during checkout" }
            drafts += CostDraft(allocated, batch.purchasePrice, allocated * batch.purchasePrice, batch.id)
            remaining -= allocated
        }
        check(remaining <= 0.0) { "Insufficient unexpired batch stock" }
        return drafts
    }

    @Transaction
    open suspend fun checkout(
        storeId: String,
        cart: List<CartLine>,
        paymentMethod: String,
        idempotencyKey: String,
        customerId: String? = null,
        amountTendered: Double? = null,
        now: Long = System.currentTimeMillis(),
        taxTreatment: TaxTreatment? = null,
        billDiscountAmount: Double = 0.0,
        staffRole: StaffRole = StaffSessionStore.current()?.role ?: StaffRole.CASHIER
    ): CheckoutResult {
        require(CheckoutRules.validateCart(cart)) { "Invalid cart" }
        require(CheckoutRules.validatePaymentMethod(paymentMethod)) { "Unsupported payment method" }
        require(CheckoutRules.validateIdempotencyKey(idempotencyKey)) { "Missing checkout idempotency key" }
        require(paymentMethod != "CREDIT" || !customerId.isNullOrBlank()) { "Select a customer for credit sales" }
        findByIdempotencyKey(storeId, idempotencyKey)?.let { return CheckoutResult(it.id, it.total, it.changeAmount) }

        val subtotal = cart.sumOf { it.lineTotal }
        StaffPermissionRules.validateBillDiscount(staffRole, subtotal, billDiscountAmount)?.let { error -> throw IllegalArgumentException(error) }
        val effectiveTaxTreatment = taxTreatment ?: StoreTaxMode.fromStorage(getStoreSettings(storeId)?.gstMode ?: StoreTaxMode.NO_GST.storageValue).toTaxTreatment()
        val safeDiscount = PricingRules.calculate(PricingInput(subtotal = subtotal, discountAmount = billDiscountAmount, taxTreatment = TaxTreatment.NO_TAX)).discountAmount
        val pricedLines = cart.map { line ->
            val lineDiscount = if (subtotal <= 0.0) 0.0 else safeDiscount * (line.lineTotal / subtotal)
            val productTaxRate = if (effectiveTaxTreatment == TaxTreatment.NO_TAX) 0.0 else (getProductMetadata(line.productId, storeId)?.taxRatePercent ?: 0.0)
            line to PricingRules.calculate(PricingInput(subtotal = line.lineTotal, discountAmount = lineDiscount, taxRatePercent = productTaxRate, taxTreatment = effectiveTaxTreatment))
        }
        val saleDiscount = pricedLines.sumOf { it.second.discountAmount }
        val saleTax = pricedLines.sumOf { it.second.taxAmount }
        val saleTotal = pricedLines.sumOf { it.second.total }
        val effectiveTender = amountTendered ?: PendingPaymentStore.get()
        val payment = PaymentSettlementRules.settle(paymentMethod, saleTotal, effectiveTender)
        val saleId = UUID.randomUUID().toString()
        val saleLines = pricedLines.map { (line, pricing) ->
            SaleLineEntity(UUID.randomUUID().toString(), saleId, line.productId, line.name, line.sku, line.quantity, line.unit, line.unitPrice, pricing.taxableAmount, pricing.discountAmount, if (effectiveTaxTreatment == TaxTreatment.NO_TAX) 0.0 else (getProductMetadata(line.productId, storeId)?.taxRatePercent ?: 0.0), pricing.taxAmount, pricing.total)
        }
        val sale = SaleEntity(saleId, storeId, customerId, subtotal, saleDiscount, saleTax, saleTotal, paymentMethod, payment.amountTendered, payment.change, idempotencyKey, now)
        val allCosts = mutableListOf<SaleCostAllocationEntity>()
        val fallbackMovements = mutableListOf<InventoryMovementEntity>()
        for ((index, line) in cart.withIndex()) {
            val costs = allocateFefo(storeId, line.productId, line.quantity, now)
            val batchCosts = costs.filter { it.batchId != null }
            if (batchCosts.isNotEmpty()) {
                check(decrementStock(line.productId, storeId, line.quantity, now) == 1) { "Insufficient product stock for ${line.name}" }
                costs.forEach { cost ->
                    allCosts += SaleCostAllocationEntity(UUID.randomUUID().toString(), saleId, saleLines[index].id, line.productId, cost.quantity, cost.unitCost, cost.totalCost, cost.batchId, now)
                }
                insertInventoryMovements(costs.map { cost -> InventoryMovementEntity(UUID.randomUUID().toString(), storeId, line.productId, cost.batchId, -cost.quantity, InventoryMovementReason.SALE.name, "SALE", saleId, now) })
            } else {
                check(decrementStock(line.productId, storeId, line.quantity, now) == 1) { "Insufficient stock for ${line.name}" }
                fallbackMovements += InventoryMovementEntity(UUID.randomUUID().toString(), storeId, line.productId, null, -line.quantity, InventoryMovementReason.SALE.name, "SALE", saleId, now)
                costs.forEach { cost -> allCosts += SaleCostAllocationEntity(UUID.randomUUID().toString(), saleId, saleLines[index].id, line.productId, cost.quantity, cost.unitCost, cost.totalCost, null, now) }
            }
        }
        if (fallbackMovements.isNotEmpty()) insertInventoryMovements(fallbackMovements)
        insertSale(sale)
        insertLines(saleLines)
        insertCostAllocations(allCosts)
        if (paymentMethod == "CREDIT") insertLedgerEntry(CustomerLedgerEntry(UUID.randomUUID().toString(), storeId, customerId!!, saleTotal, "CREDIT_SALE", "Sale $saleId", "SALE", saleId, now))
        PendingPaymentStore.clear()
        return CheckoutResult(saleId, saleTotal, payment.change)
    }
}
