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
        val payable = purchase.netAmount - purchase.paidAmount
        require(payable >= -1e-9) { "Paid amount cannot exceed purchase total" }

        database.withTransaction {
            database.purchaseDao().save(purchase.copy(outstandingAmount = payable.coerceAtLeast(0.0)), lines)

            batches.forEach { batch -> database.inventoryDao().insertBatch(batch) }
            lines.forEach { line ->
                val quantity = line.orderedQuantity + line.freeQuantity
                require(quantity > 0.0) { "Received quantity must be positive" }
                val updated = database.inventoryDao().updateProductStock(
                    line.storeId,
                    line.productId,
                    quantity,
                    now
                )
                check(updated == 1) { "Product stock could not be updated for ${line.productId}" }
                database.inventoryDao().insertMovement(
                    InventoryMovementEntity(
                        id = UUID.randomUUID().toString(),
                        storeId = line.storeId,
                        productId = line.productId,
                        batchId = batches.firstOrNull { it.productId == line.productId }?.id,
                        quantityDelta = quantity,
                        reason = InventoryMovementReason.RECEIVE.name,
                        referenceType = "PURCHASE",
                        referenceId = purchase.id,
                        createdAt = now
                    )
                )
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
