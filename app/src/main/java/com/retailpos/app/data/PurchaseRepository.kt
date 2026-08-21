package com.retailpos.app.data

import androidx.room.withTransaction
import java.util.UUID

class PurchaseRepository(private val database: RetailDatabase) {
    suspend fun recordPurchase(
        purchase: PurchaseEntity,
        lines: List<PurchaseLineEntity>,
        batches: List<InventoryBatchEntity>,
        now: Long = System.currentTimeMillis()
    ) {
        require(lines.isNotEmpty()) { "Purchase must contain lines" }
        require(purchase.storeId.isNotBlank()) { "Purchase store is required" }
        require(purchase.supplierId.isNotBlank()) { "Supplier is required" }
        require(purchase.grossAmount.isFinite() && purchase.grossAmount >= 0.0) { "Purchase gross amount is invalid" }
        require(purchase.schemeDiscount.isFinite() && purchase.schemeDiscount >= 0.0) { "Purchase discount is invalid" }
        require(purchase.netAmount.isFinite() && purchase.netAmount >= 0.0) { "Purchase total is invalid" }
        require(purchase.paidAmount.isFinite() && purchase.paidAmount >= 0.0) { "Paid amount is invalid" }
        require(purchase.paidAmount <= purchase.netAmount + 1e-9) { "Paid amount cannot exceed purchase total" }

        val supplier = database.supplierDao().getById(purchase.supplierId, purchase.storeId)
            ?: throw IllegalArgumentException("Supplier does not belong to this store.")
        check(supplier.id == purchase.supplierId) { "Invalid supplier reference" }

        val lineProducts = lines.associateBy { it.productId }
        require(lineProducts.size == lines.size) { "Purchase contains duplicate product lines" }
        lines.forEach { line ->
            require(line.purchaseId == purchase.id) { "Purchase line references a different purchase" }
            require(line.storeId == purchase.storeId) { "Purchase line does not belong to the purchase store" }
            require(line.orderedQuantity.isFinite() && line.orderedQuantity > 0.0) { "Ordered quantity must be greater than zero" }
            require(line.freeQuantity.isFinite() && line.freeQuantity >= 0.0) { "Free quantity cannot be negative" }
            require(line.purchaseRate.isFinite() && line.purchaseRate >= 0.0) { "Purchase rate cannot be negative" }
            require(line.schemeDiscount.isFinite() && line.schemeDiscount >= 0.0) { "Scheme discount cannot be negative" }
            require(line.schemeDiscount <= line.orderedQuantity * line.purchaseRate + 1e-9) { "Scheme discount cannot exceed gross cost" }
            require(line.expiryDate == null || !line.batchNumber.isNullOrBlank()) { "Expiry requires a batch number" }
            val product = database.productDao().getById(line.productId, purchase.storeId)
            require(product != null) { "Product ${line.productId} does not belong to this store." }

            val gross = line.orderedQuantity * line.purchaseRate
            val net = gross - line.schemeDiscount
            val stock = line.orderedQuantity + line.freeQuantity
            val effective = if (stock > 0.0) net / stock else 0.0
            require(kotlin.math.abs(line.netCost - net) <= 1e-9) { "Purchase line net cost is inconsistent" }
            require(kotlin.math.abs(line.effectiveCost - effective) <= 1e-9) { "Purchase line effective cost is inconsistent" }
        }

        val expectedGross = lines.sumOf { it.orderedQuantity * it.purchaseRate }
        val expectedScheme = lines.sumOf { it.schemeDiscount }
        val expectedNet = expectedGross - expectedScheme
        require(kotlin.math.abs(purchase.grossAmount - expectedGross) <= 1e-9) { "Purchase gross amount does not match its lines" }
        require(kotlin.math.abs(purchase.schemeDiscount - expectedScheme) <= 1e-9) { "Purchase discount does not match its lines" }
        require(kotlin.math.abs(purchase.netAmount - expectedNet) <= 1e-9) { "Purchase total does not match its lines" }
        val payable = purchase.netAmount - purchase.paidAmount

        val batchesByProduct = batches.groupBy { it.productId }
        batches.forEach { batch ->
            require(batch.storeId == purchase.storeId) { "Batch does not belong to the purchase store" }
            require(lineProducts.containsKey(batch.productId)) { "Batch references a product not present in the purchase" }
            require(batch.quantity.isFinite() && batch.quantity > 0.0) { "Batch quantity must be positive" }
            require(batch.purchasePrice.isFinite() && batch.purchasePrice >= 0.0) { "Batch purchase price is invalid" }
            require(batch.expiryDate == null || batch.expiryDate > now) { "Batch expiry must be in the future" }
        }

        lines.forEach { line ->
            val expectedStock = line.orderedQuantity + line.freeQuantity
            val productBatches = batchesByProduct[line.productId].orEmpty()
            if (productBatches.isNotEmpty()) {
                val batchedQuantity = productBatches.sumOf { it.quantity }
                require(kotlin.math.abs(batchedQuantity - expectedStock) <= 1e-9) {
                    "Batch quantity for ${line.productId} must equal received stock quantity"
                }
            }
        }

        database.withTransaction {
            database.purchaseDao().save(purchase.copy(outstandingAmount = payable.coerceAtLeast(0.0)), lines)

            batches.forEach { batch -> database.inventoryDao().insertBatch(batch) }
            lines.forEach { line ->
                val quantity = line.orderedQuantity + line.freeQuantity
                val updated = database.inventoryDao().updateProductStock(
                    line.storeId,
                    line.productId,
                    quantity,
                    now
                )
                check(updated == 1) { "Product stock could not be updated for ${line.productId}" }

                val productBatches = batchesByProduct[line.productId].orEmpty()
                if (productBatches.isEmpty()) {
                    database.inventoryDao().insertMovement(
                        InventoryMovementEntity(
                            id = UUID.randomUUID().toString(),
                            storeId = line.storeId,
                            productId = line.productId,
                            batchId = null,
                            quantityDelta = quantity,
                            reason = InventoryMovementReason.RECEIVE.name,
                            referenceType = "PURCHASE",
                            referenceId = purchase.id,
                            createdAt = now
                        )
                    )
                } else {
                    productBatches.forEach { batch ->
                        database.inventoryDao().insertMovement(
                            InventoryMovementEntity(
                                id = UUID.randomUUID().toString(),
                                storeId = line.storeId,
                                productId = line.productId,
                                batchId = batch.id,
                                quantityDelta = batch.quantity,
                                reason = InventoryMovementReason.RECEIVE.name,
                                referenceType = "PURCHASE",
                                referenceId = purchase.id,
                                createdAt = now
                            )
                        )
                    }
                }
            }

            if (purchase.netAmount > 0.0) {
                database.supplierLedgerDao().insert(
                    SupplierLedgerEntry(
                        id = UUID.randomUUID().toString(),
                        storeId = purchase.storeId,
                        supplierId = purchase.supplierId,
                        amount = purchase.netAmount,
                        type = "PURCHASE",
                        note = purchase.invoiceNumber?.let { "Purchase invoice $it" } ?: "Purchase ${purchase.id}",
                        referenceType = "PURCHASE",
                        referenceId = purchase.id,
                        createdAt = now
                    )
                )
            }

            if (purchase.paidAmount > 0.0) {
                database.supplierLedgerDao().insert(
                    SupplierLedgerEntry(
                        id = UUID.randomUUID().toString(),
                        storeId = purchase.storeId,
                        supplierId = purchase.supplierId,
                        amount = -purchase.paidAmount,
                        type = "PAYMENT",
                        note = "Payment against purchase ${purchase.id}",
                        referenceType = "PURCHASE_PAYMENT",
                        referenceId = purchase.id,
                        createdAt = now
                    )
                )
            }
        }
    }
}
