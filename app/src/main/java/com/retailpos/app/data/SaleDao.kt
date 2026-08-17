package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.util.UUID

@Dao
abstract class SaleDao {
    @Insert
    abstract suspend fun insertSale(sale: SaleEntity)

    @Insert
    abstract suspend fun insertLines(lines: List<SaleLineEntity>)

    @Insert
    abstract suspend fun insertInventoryMovements(movements: List<InventoryMovementEntity>)

    @Query("SELECT * FROM sales WHERE storeId = :storeId AND idempotencyKey = :idempotencyKey LIMIT 1")
    abstract suspend fun findByIdempotencyKey(storeId: String, idempotencyKey: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE id = :saleId AND storeId = :storeId LIMIT 1")
    abstract suspend fun getSale(storeId: String, saleId: String): SaleEntity?

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
    abstract suspend fun getSaleLines(saleId: String): List<SaleLineEntity>

    @Query("UPDATE products SET stock = stock - :quantity, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId AND stock >= :quantity")
    abstract suspend fun decrementStock(
        productId: String,
        storeId: String,
        quantity: Double,
        updatedAt: Long
    ): Int

    @Query("SELECT * FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0 AND (expiryDate IS NULL OR expiryDate > :now) ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, createdAt ASC")
    abstract suspend fun getAvailableBatchesFefo(storeId: String, productId: String, now: Long): List<InventoryBatchEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0)")
    abstract suspend fun hasPositiveBatchStock(storeId: String, productId: String): Boolean

    @Query("UPDATE inventory_batches SET quantity = quantity - :quantity WHERE id = :batchId AND storeId = :storeId AND quantity >= :quantity")
    abstract suspend fun decrementBatch(batchId: String, storeId: String, quantity: Double): Int

    private suspend fun allocateFefo(
        storeId: String,
        productId: String,
        requiredQuantity: Double,
        saleId: String,
        now: Long
    ): Boolean {
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
            val updated = decrementBatch(batch.id, storeId, allocated)
            check(updated == 1) { "Batch stock changed during checkout" }
            movements += InventoryMovementEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = productId,
                batchId = batch.id,
                quantityDelta = -allocated,
                reason = InventoryMovementReason.SALE.name,
                referenceType = "SALE",
                referenceId = saleId,
                createdAt = now
            )
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
        now: Long = System.currentTimeMillis()
    ): CheckoutResult {
        require(CheckoutRules.validateCart(cart)) { "Invalid cart" }
        require(CheckoutRules.validatePaymentMethod(paymentMethod)) { "Unsupported payment method" }
        require(CheckoutRules.validateIdempotencyKey(idempotencyKey)) { "Missing checkout idempotency key" }

        findByIdempotencyKey(storeId, idempotencyKey)?.let {
            return CheckoutResult(it.id, it.total)
        }

        val subtotal = cart.sumOf { it.lineTotal }
        val saleId = UUID.randomUUID().toString()
        val sale = SaleEntity(
            id = saleId,
            storeId = storeId,
            subtotal = subtotal,
            total = subtotal,
            paymentMethod = paymentMethod,
            idempotencyKey = idempotencyKey,
            createdAt = now
        )

        val fallbackMovements = mutableListOf<InventoryMovementEntity>()
        for (line in cart) {
            val allocatedToBatch = allocateFefo(storeId, line.productId, line.quantity, saleId, now)
            if (allocatedToBatch) {
                val updated = decrementStock(line.productId, storeId, line.quantity, now)
                check(updated == 1) { "Insufficient product stock for ${line.name}" }
            } else {
                val updated = decrementStock(line.productId, storeId, line.quantity, now)
                check(updated == 1) { "Insufficient stock for ${line.name}" }
                fallbackMovements += InventoryMovementEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = storeId,
                    productId = line.productId,
                    batchId = null,
                    quantityDelta = -line.quantity,
                    reason = InventoryMovementReason.SALE.name,
                    referenceType = "SALE",
                    referenceId = saleId,
                    createdAt = now
                )
            }
        }

        if (fallbackMovements.isNotEmpty()) insertInventoryMovements(fallbackMovements)

        insertSale(sale)
        insertLines(cart.map { line ->
            SaleLineEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = line.productId,
                name = line.name,
                sku = line.sku,
                quantity = line.quantity,
                unit = line.unit,
                unitPrice = line.unitPrice,
                lineTotal = line.lineTotal
            )
        })

        return CheckoutResult(saleId, subtotal)
    }
}
