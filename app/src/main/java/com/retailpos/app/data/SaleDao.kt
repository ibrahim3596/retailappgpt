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

    @Query("SELECT * FROM sales WHERE storeId = :storeId AND idempotencyKey = :idempotencyKey LIMIT 1")
    abstract suspend fun findByIdempotencyKey(storeId: String, idempotencyKey: String): SaleEntity?

    @Query("UPDATE products SET stock = stock - :quantity, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId AND stock >= :quantity")
    abstract suspend fun decrementStock(
        productId: String,
        storeId: String,
        quantity: Double,
        updatedAt: Long
    ): Int

    @Transaction
    open suspend fun checkout(
        storeId: String,
        cart: List<CartLine>,
        paymentMethod: String,
        idempotencyKey: String,
        now: Long = System.currentTimeMillis()
    ): CheckoutResult {
        require(cart.isNotEmpty()) { "Cannot checkout an empty cart" }
        require(paymentMethod in setOf("CASH", "UPI", "CARD")) { "Unsupported payment method" }
        require(idempotencyKey.isNotBlank()) { "Missing checkout idempotency key" }
        require(cart.all { it.quantity > 0.0 && it.unitPrice >= 0.0 }) { "Invalid cart line" }

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

        cart.forEach { line ->
            val updated = decrementStock(line.productId, storeId, line.quantity, now)
            check(updated == 1) { "Insufficient stock for ${line.name}" }
        }

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
