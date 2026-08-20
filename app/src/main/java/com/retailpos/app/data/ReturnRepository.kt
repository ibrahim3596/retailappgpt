package com.retailpos.app.data

import androidx.room.withTransaction
import com.retailpos.app.core.permissions.StaffPermission
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
        require(StaffPermissionRules.hasPermission(staffRole, StaffPermission.PROCESS_RETURN)) {
            "This staff role cannot process returns."
        }
        require(quantitiesBySaleLineId.isNotEmpty()) { "Select at least one item to return" }
        require(reason.isNotBlank()) { "Return reason is required" }
        ReturnRules.validateRefundMethod(originalSale.paymentMethod, refundMethod)?.let { throw IllegalArgumentException(it) }

        val saleLines = database.saleDao().getSaleLines(originalSale.id)
        val selected = saleLines.associateBy { it.id }

        return database.withTransaction {
            val returnId = UUID.randomUUID().toString()
            val returnLines = mutableListOf<ReturnLineEntity>()
            var refundTotal = 0.0

            for ((saleLineId, requestedQuantity) in quantitiesBySaleLineId) {
                val saleLine = selected[saleLineId] ?: throw IllegalArgumentException("Sale line no longer exists")
                val alreadyReturned = database.returnDao().alreadyReturnedQuantity(saleLineId)
                val remaining = (saleLine.quantity - alreadyReturned).coerceAtLeast(0.0)
                ReturnRules.validateQuantity(requestedQuantity, remaining)?.let { throw IllegalArgumentException(it) }

                val refundAmount = saleLine.lineTotal * (requestedQuantity / saleLine.quantity)
                ReturnRules.validateRefundAmount(refundAmount, saleLine.lineTotal)?.let { throw IllegalArgumentException(it) }

                val allocations = database.saleDao().getSaleCostAllocations(originalSale.id).filter { it.saleLineId == saleLineId }
                var restoredCost = 0.0
                if (allocations.isNotEmpty()) {
                    val totalAllocatedQuantity = allocations.sumOf { it.quantity }.coerceAtLeast(0.0)
                    if (totalAllocatedQuantity > 0.0) {
                        allocations.forEach { allocation ->
                            val restoreQuantity = requestedQuantity * (allocation.quantity / totalAllocatedQuantity)
                            if (restoreQuantity > 0.0 && allocation.batchId != null) {
                                check(database.inventoryDao().updateBatchQuantity(storeId, saleLine.productId, allocation.batchId, restoreQuantity) == 1) {
                                    "Original batch could not be restored"
                                }
                            }
                            restoredCost += restoreQuantity * allocation.unitCost
                        }
                    }
                }

                check(database.inventoryDao().updateProductStock(storeId, saleLine.productId, requestedQuantity, now) == 1) {
                    "Product stock could not be restored"
                }
                database.inventoryDao().insertMovement(
                    InventoryMovementEntity(UUID.randomUUID().toString(), storeId, saleLine.productId, null, requestedQuantity, InventoryMovementReason.RETURN.name, "RETURN", returnId, now)
                )

                returnLines += ReturnLineEntity(returnId, saleLineId, saleLine.productId, requestedQuantity, refundAmount, restoredCost)
                refundTotal += refundAmount
            }

            ReturnRules.validateRefundAmount(refundTotal, originalSale.total)?.let { throw IllegalArgumentException(it) }
            val returnEntity = ReturnEntity(returnId, storeId, originalSale.id, refundMethod, refundTotal, reason.trim(), staffRole.name, now)
            database.returnDao().insert(returnEntity)
            database.returnDao().insertLines(returnLines)

            if (originalSale.paymentMethod == "CREDIT" && !originalSale.customerId.isNullOrBlank()) {
                database.khataDao().insert(
                    CustomerLedgerEntry(
                        UUID.randomUUID().toString(), storeId, originalSale.customerId,
                        -refundTotal, "RETURN", "Return against sale ${originalSale.id}",
                        "RETURN", returnId, now
                    )
                )
            }
            returnEntity
        }
    }
}
