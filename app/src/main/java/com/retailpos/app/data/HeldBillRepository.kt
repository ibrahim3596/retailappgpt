package com.retailpos.app.data

import androidx.room.withTransaction
import java.util.UUID

class HeldBillRepository(private val database: RetailDatabase) {
    suspend fun hold(storeId: String, lines: List<CartLine>, now: Long = System.currentTimeMillis()): String {
        require(lines.isNotEmpty()) { "Cannot hold an empty bill" }
        val id = UUID.randomUUID().toString()
        database.heldBillDao().save(
            HeldBillEntity(id, storeId, now, now),
            lines.map {
                HeldBillLineEntity(
                    heldBillId = id,
                    productId = it.productId,
                    name = it.name,
                    sku = it.sku,
                    unit = it.unit,
                    unitPrice = it.unitPrice,
                    quantity = it.quantity,
                    overrideUnitPrice = it.overrideUnitPrice,
                    itemDiscountAmount = it.itemDiscountAmount
                )
            }
        )
        return id
    }

    suspend fun list(storeId: String): List<HeldBillSnapshot> = database.heldBillDao().getAll(storeId).map { bill ->
        HeldBillSnapshot(
            id = bill.id,
            createdAt = bill.createdAt,
            lines = database.heldBillDao().getLines(bill.id).map(::toCartLine)
        )
    }

    /**
     * Atomically claims a held bill for resume after validating that every
     * referenced product still exists and has enough current stock.
     * The held bill is deleted only after validation succeeds, so two
     * concurrent resume attempts cannot both claim the same bill.
     */
    suspend fun takeForResume(storeId: String, id: String): HeldBillSnapshot? = database.withTransaction {
        val bill = database.heldBillDao().getAll(storeId).firstOrNull { it.id == id }
            ?: return@withTransaction null
        val lines = database.heldBillDao().getLines(id).map(::toCartLine)
        require(lines.isNotEmpty()) { "Held bill is empty" }

        lines.forEach { line ->
            val product = database.productDao().getById(line.productId, storeId)
                ?: throw IllegalArgumentException("${line.name} no longer exists in the product catalog.")
            require(line.quantity > 0.0 && line.quantity.isFinite()) { "${line.name} has an invalid held quantity." }
            require(line.quantity <= product.stock + 1e-9) {
                "${line.name} has only ${product.stock.clean()} ${product.unit} available"
            }
        }

        database.heldBillDao().deleteBill(id, storeId)
        HeldBillSnapshot(bill.id, bill.createdAt, lines)
    }

    suspend fun take(storeId: String, id: String): HeldBillSnapshot? = database.withTransaction {
        val bill = database.heldBillDao().getAll(storeId).firstOrNull { it.id == id }
            ?: return@withTransaction null
        val lines = database.heldBillDao().getLines(id).map(::toCartLine)
        database.heldBillDao().deleteBill(id, storeId)
        HeldBillSnapshot(bill.id, bill.createdAt, lines)
    }

    private fun toCartLine(line: HeldBillLineEntity): CartLine = CartLine(
        productId = line.productId,
        name = line.name,
        sku = line.sku,
        unit = line.unit,
        unitPrice = line.unitPrice,
        quantity = line.quantity,
        overrideUnitPrice = line.overrideUnitPrice,
        itemDiscountAmount = line.itemDiscountAmount
    )
}

private fun Double.clean(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format(java.util.Locale.US, "%.2f", this)
