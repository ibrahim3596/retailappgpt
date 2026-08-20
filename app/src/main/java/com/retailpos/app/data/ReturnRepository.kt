package com.retailpos.app.data

import androidx.room.withTransaction
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.core.returns.ReturnRules
import java.util.UUID

class ReturnRepository(private val database: RetailDatabase) {
    suspend fun processReturn(
        storeId: String,
        originalSale: SaleEntity,
        quantitiesBySaleLineId: Map<String, Double>,
        refundMethod: String,
        reason: String,
        staffRole: StaffRole,
        now: Long = System.currentTimeMillis()
    ): ReturnEntity {
        StaffPermissionRules.requireRefundPermission(staffRole)
        require(quantitiesBySaleLineId.isNotEmpty()) { "Select at least one item to return" }
        require(reason.isNotBlank()) { "Return reason is required" }
        val saleLines = database.saleDao().getSaleLines(originalSale.id)
        val selected = saleLines.associateBy { it.id }
        val returnLines = mutableListOf<ReturnLineEntity>()
        var refundTotal = 0.0

        database.withTransaction {
            for ((saleLineId, requestedQuantity) in quantitiesBySaleLineId) {
                val saleLine = selected[saleLineId] ?: throw IllegalArgumentException("Sale line no longer exists")
                val alreadyReturned = database.returnDao().alreadyReturnedQuantity(saleLineId)
                val remaining = (saleLine.quantity - alreadyReturned).coerceAtLeast(0.0)
                ReturnRules.validateQuantity(requestedQuantity, remaining)?.let { throw IllegalArgumentException(it) }
                val refundAmount = saleLine.lineTotal * (requestedQuantity / saleLine.quantity)
                val maxRefund = saleLine.lineTotal
                ReturnRules.validateRefundAmount(refundAmount, maxRefund)?.let { throw IllegalArgumentException(it) }
                var remainingToRestore = requestedQuantity
                val allocations = database.saleDao().getSaleCostAllocations(originalSale.id).filter { it.saleLineId == saleLineId }
                var restoredCost = 0.0
                for (allocation in allocations) {
                    if (remainingToRestore <= 0.0) break
                    val alreadyFromBatch = 0.0
                    val restore = minOf(remainingToRestore, allocation.quantity - alreadyFromBatch)
                    if (restore <= 0.0) continue
                    if (allocation.batchId != null) {
                        check(database.inventoryDao().updateBatchQuantity(storeId, saleLine.productId, allocation.batchId, restore) == 1) { "Original batch could not be restored" }
                    }
                    restoredCost += restore * allocation.unitCost
                    remainingToRestore -= restore
                }
                if (remainingToRestore > 0.0) restoredCost += remainingToRestore * ((allocations.firstOrNull()?.unitCost) ?: 0.0)
                check(database.inventoryDao().updateProductStock(storeId, saleLine.productId, requestedQuantity, now) == 1) { "Product stock could not be restored" }
                database.inventoryDao().insertMovement(InventoryMovementEntity(UUID.randomUUID().toString(), storeId, saleLine.productId, null, requestedQuantity, InventoryMovementReason.RETURN.name, "RETURN", null, now))
                returnLines += ReturnLineEntity("", saleLineId, saleLine.productId, requestedQuantity, refundAmount, restoredCost)
                refundTotal += refundAmount
            }

            val returnId = UUID.randomUUID().toString()
            val returnEntity = ReturnEntity(returnId, storeId, originalSale.id, refundMethod, refundTotal, reason.trim(), staffRole.name, now)
            insertReturn(returnEntity)
            insertReturnLines(returnLines.map { it.copy(returnId = returnId) })
            if (originalSale.paymentMethod == "CREDIT" && !originalSale.customerId.isNullOrBlank()) {
                database.khataDao().insert(CustomerLedgerEntry(UUID.randomUUID().toString(), storeId, originalSale.customerId, -refundTotal, "RETURN", "Return against sale ${originalSale.id}", "RETURN", returnId, now))
            }
            return returnEntity
        }
    }

    private suspend fun insertReturn(returnEntity: ReturnEntity) = database.returnDao().insert(returnEntity)
    private suspend fun insertReturnLines(lines: List<ReturnLineEntity>) = database.returnDao().insertLines(lines)
}
