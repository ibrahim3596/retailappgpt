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
                HeldBillLineEntity(id, it.productId, it.name, it.sku, it.unit, it.unitPrice, it.quantity)
            }
        )
        return id
    }

    suspend fun list(storeId: String): List<HeldBillSnapshot> = database.heldBillDao().getAll(storeId).map { bill ->
        HeldBillSnapshot(
            id = bill.id,
            createdAt = bill.createdAt,
            lines = database.heldBillDao().getLines(bill.id).map {
                CartLine(it.productId, it.name, it.sku, it.unit, it.unitPrice, it.quantity)
            }
        )
    }

    suspend fun take(storeId: String, id: String): HeldBillSnapshot? = database.withTransaction {
        val bill = database.heldBillDao().getAll(storeId).firstOrNull { it.id == id } ?: return@withTransaction null
        val lines = database.heldBillDao().getLines(id).map {
            CartLine(it.productId, it.name, it.sku, it.unit, it.unitPrice, it.quantity)
        }
        database.heldBillDao().deleteBill(id, storeId)
        HeldBillSnapshot(bill.id, bill.createdAt, lines)
    }
}
