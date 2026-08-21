package com.retailpos.app.data

import androidx.room.withTransaction
import com.retailpos.app.core.payment.CheckoutRecoveryFingerprint
import com.retailpos.app.core.payment.PaymentSettlementRules
import com.retailpos.app.core.payment.PendingPaymentStore
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.core.pos.CartLinePricingRules
import com.retailpos.app.core.products.PricingInput
import com.retailpos.app.core.products.PricingRules
import com.retailpos.app.core.products.StoreTaxMode
import com.retailpos.app.core.products.TaxTreatment
import com.retailpos.app.core.products.toTaxTreatment
import com.retailpos.app.core.staff.StaffSessionStore
import java.util.UUID

class SaleCheckoutRepository(private val database: RetailDatabase) {
    private data class CostDraft(
        val quantity: Double,
        val unitCost: Double,
        val totalCost: Double,
        val batchId: String?
    )

    private suspend fun allocateFefo(
        dao: SaleDao,
        storeId: String,
        productId: String,
        requiredQuantity: Double,
        now: Long
    ): List<CostDraft> {
        var remaining = requiredQuantity
        val batches = dao.getAvailableBatchesFefo(storeId, productId, now)
        if (batches.isEmpty()) {
            check(!dao.hasPositiveBatchStock(storeId, productId)) { "Only expired batch stock is available" }
            val fallbackCost = dao.getCurrentPurchasePrice(productId, storeId) ?: 0.0
            return listOf(CostDraft(requiredQuantity, fallbackCost, requiredQuantity * fallbackCost, null))
        }
        val drafts = mutableListOf<CostDraft>()
        for (batch in batches) {
            if (remaining <= 0.0) break
            val allocated = minOf(remaining, batch.quantity)
            check(dao.decrementBatch(batch.id, storeId, allocated) == 1) { "Batch stock changed during checkout" }
            drafts += CostDraft(allocated, batch.purchasePrice, allocated * batch.purchasePrice, batch.id)
            remaining -= allocated
        }
        check(remaining <= 0.0) { "Insufficient unexpired batch stock" }
        return drafts
    }

    suspend fun checkout(
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
    ): CheckoutResult = database.withTransaction {
        val dao = database.saleDao()
        require(CheckoutRules.validateCart(cart)) { "Invalid cart" }
        require(CheckoutRules.validatePaymentMethod(paymentMethod)) { "Unsupported payment method" }
        require(CheckoutRules.validateIdempotencyKey(idempotencyKey)) { "Missing checkout idempotency key" }
        require(paymentMethod != "CREDIT" || !customerId.isNullOrBlank()) { "Select a customer for credit sales" }
        if (paymentMethod == "CREDIT") {
            check(dao.customerBelongsToStore(customerId!!, storeId)) { "Customer does not belong to this store." }
        }
        dao.findByIdempotencyKey(storeId, idempotencyKey)?.let { return@withTransaction CheckoutResult(it.id, it.total, it.changeAmount) }

        cart.forEach { line ->
            val product = dao.queryProduct(line.productId, storeId)
                ?: throw IllegalArgumentException("${line.name} is no longer in the product catalog.")
            if (product.isArchived) throw IllegalArgumentException("${product.name} is archived and cannot be sold.")
            if (line.quantity > product.stock + 1e-9) {
                throw IllegalArgumentException("${product.name} has only ${product.stock.clean()} ${product.unit} available; the saved bill requested ${line.quantity.clean()} ${line.unit}.")
            }
            CartLinePricingRules.validate(line, staffRole)?.let { throw IllegalArgumentException("${line.name}: $it") }
            require(line.lineTotal.isFinite() && line.lineTotal >= 0.0) { "${line.name} has invalid pricing in the saved bill." }
        }

        val subtotal = cart.sumOf { it.lineTotal }
        StaffPermissionRules.validateBillDiscount(staffRole, subtotal, billDiscountAmount)?.let { error -> throw IllegalArgumentException(error) }
        val effectiveTaxTreatment = taxTreatment ?: StoreTaxMode.fromStorage(dao.getStoreSettings(storeId)?.gstMode ?: StoreTaxMode.NO_GST.storageValue).toTaxTreatment()
        val safeDiscount = PricingRules.calculate(PricingInput(subtotal = subtotal, discountAmount = billDiscountAmount, taxTreatment = TaxTreatment.NO_TAX)).discountAmount
        val pricedLines = cart.map { line ->
            val lineBillDiscount = if (subtotal <= 0.0) 0.0 else safeDiscount * (line.lineTotal / subtotal)
            val combinedDiscount = line.itemDiscountAmount + lineBillDiscount
            val productTaxRate = if (effectiveTaxTreatment == TaxTreatment.NO_TAX) 0.0 else (dao.getProductMetadata(line.productId, storeId)?.taxRatePercent ?: 0.0)
            line to PricingRules.calculate(PricingInput(subtotal = line.grossLineTotal, discountAmount = combinedDiscount, taxRatePercent = productTaxRate, taxTreatment = effectiveTaxTreatment))
        }
        val saleDiscount = pricedLines.sumOf { it.second.discountAmount }
        val saleTax = pricedLines.sumOf { it.second.taxAmount }
        val saleTotal = pricedLines.sumOf { it.second.total }
        val cartFingerprint = CheckoutRecoveryFingerprint.of(cart)
        val effectiveTender = amountTendered ?: PendingPaymentStore.getAmountTenderedForCart(cartFingerprint)
        val payment = PaymentSettlementRules.settle(paymentMethod, saleTotal, effectiveTender)
        val saleId = UUID.randomUUID().toString()
        val saleLines = pricedLines.map { (line, pricing) ->
            SaleLineEntity(UUID.randomUUID().toString(), saleId, line.productId, line.name, line.sku, line.quantity, line.unit, line.effectiveUnitPrice, pricing.taxableAmount, pricing.discountAmount, if (effectiveTaxTreatment == TaxTreatment.NO_TAX) 0.0 else (dao.getProductMetadata(line.productId, storeId)?.taxRatePercent ?: 0.0), pricing.taxAmount, pricing.total)
        }
        val sale = SaleEntity(saleId, storeId, customerId, cart.sumOf { it.grossLineTotal }, saleDiscount, saleTax, saleTotal, paymentMethod, payment.amountTendered, payment.change, idempotencyKey, now)
        val allCosts = mutableListOf<SaleCostAllocationEntity>()
        val fallbackMovements = mutableListOf<InventoryMovementEntity>()
        for ((index, line) in cart.withIndex()) {
            val costs = allocateFefo(dao, storeId, line.productId, line.quantity, now)
            val batchCosts = costs.filter { it.batchId != null }
            if (batchCosts.isNotEmpty()) {
                check(dao.decrementStock(line.productId, storeId, line.quantity, now) == 1) { "Insufficient product stock for ${line.name}" }
                costs.forEach { cost ->
                    allCosts += SaleCostAllocationEntity(UUID.randomUUID().toString(), saleId, saleLines[index].id, line.productId, cost.quantity, cost.unitCost, cost.totalCost, cost.batchId, now)
                }
                dao.insertInventoryMovements(costs.map { cost -> InventoryMovementEntity(UUID.randomUUID().toString(), storeId, line.productId, cost.batchId, -cost.quantity, InventoryMovementReason.SALE.name, "SALE", saleId, now) })
            } else {
                check(dao.decrementStock(line.productId, storeId, line.quantity, now) == 1) { "Insufficient stock for ${line.name}" }
                fallbackMovements += InventoryMovementEntity(UUID.randomUUID().toString(), storeId, line.productId, null, -line.quantity, InventoryMovementReason.SALE.name, "SALE", saleId, now)
                costs.forEach { cost -> allCosts += SaleCostAllocationEntity(UUID.randomUUID().toString(), saleId, saleLines[index].id, line.productId, cost.quantity, cost.unitCost, cost.totalCost, null, now) }
            }
        }
        if (fallbackMovements.isNotEmpty()) dao.insertInventoryMovements(fallbackMovements)
        dao.insertSale(sale)
        dao.insertLines(saleLines)
        dao.insertCostAllocations(allCosts)
        if (paymentMethod == "CREDIT") dao.insertLedgerEntry(CustomerLedgerEntry(UUID.randomUUID().toString(), storeId, customerId!!, saleTotal, "CREDIT_SALE", "Sale $saleId", "SALE", saleId, now))
        PendingPaymentStore.clear()
        CheckoutResult(saleId, saleTotal, payment.change)
    }
}

private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(java.util.Locale.US, "%.2f", this)
