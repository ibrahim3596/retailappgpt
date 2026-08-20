package com.retailpos.app.data

import java.util.UUID

 data class HeldBillSnapshot(
    val id: String,
    val createdAt: Long,
    val lines: List<CartLine>
)

object HeldBillStore {
    private val bills = mutableListOf<HeldBillSnapshot>()

    @Synchronized
    fun hold(lines: List<CartLine>, now: Long = System.currentTimeMillis()): String {
        require(lines.isNotEmpty()) { "Cannot hold an empty bill" }
        require(lines.all { it.quantity.isFinite() && it.quantity > 0.0 }) { "Invalid held bill quantity" }
        val id = UUID.randomUUID().toString()
        bills += HeldBillSnapshot(id, now, lines.toList())
        return id
    }

    @Synchronized
    fun list(): List<HeldBillSnapshot> = bills.sortedByDescending { it.createdAt }.toList()

    @Synchronized
    fun take(id: String): HeldBillSnapshot? {
        val index = bills.indexOfFirst { it.id == id }
        if (index < 0) return null
        return bills.removeAt(index)
    }

    @Synchronized
    fun remove(id: String): Boolean = bills.removeIf { it.id == id }

    @Synchronized
    fun clear() = bills.clear()
}
