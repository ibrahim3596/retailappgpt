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
            lines = database.heldBillDao().getLines(bill.id).map {
                CartLine(
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
    }

    suspend fun take(storeId: String, id: String): HeldBillSnapshot? = database.withTransaction {
        val bill = database.heldBillDao().getAll(storeId).firstOrNull { it.id == id } ?: return@withTransaction null
        val lines = database.heldBillDao().getLines(id).map {
            CartLine(
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
        database.heldBillDao().deleteBill(id, storeId)
        HeldBillSnapshot(bill.id, bill.createdAt, lines)
    }
}
