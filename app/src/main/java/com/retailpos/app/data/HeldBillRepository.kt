package com.retailpos.app.data

import androidx.room.withTransaction
import java.util.UUID

class HeldBillRepository(private val database: RetailDatabase) {
    private val dao = database.heldBillDao()

    suspend fun hold(storeId: String, lines: List<CartLine>, now: Long = System.currentTimeMillis()): String {
        require(lines.isNotEmpty()) { "Cannot hold an empty bill" }
        require(lines.all { it.quantity.isFinite() && it.quantity > 0.0 }) { "Invalid held bill quantity" }
        val billId = UUID.randomUUID().toString()
        database.withTransaction {
            dao.insertBill(HeldBillEntity(billId, storeId, now, now))
            dao.insertLines(lines.map { line -> HeldBillLineEntity(billId, line.productId, line.name, line.sku, line.unit, line.unitPrice, line.quantity) })
        }
        return billId
    }

    suspend fun list(storeId: String): List<HeldBillEntity> = dao.getAll(storeId)

    suspend fun restore(storeId: String, heldBillId: String): List<CartLine> {
        val bill = dao.getAll(storeId).firstOrNull { it.id == heldBillId } ?: error("Held bill not found")
        val lines = dao.getLines(bill.id)
        require(lines.isNotEmpty()) { "Held bill is empty" }
        return lines.map { CartLine(it.productId, it.name, it.sku, it.unit, it.unitPrice, it.quantity) }
    }

    suspend fun delete(storeId: String, heldBillId: String) = dao.deleteWithLines(heldBillId, storeId)
}
