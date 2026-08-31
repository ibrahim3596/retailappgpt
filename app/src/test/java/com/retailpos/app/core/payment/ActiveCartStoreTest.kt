package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveCartStoreTest {
    private val line = CartLine("p1", "Sugar", "P1", "kg", 70.0, 2.0)

    @Test
    fun cartLineRoundTripShapePreservesPricingFields() {
        val line = CartLine(
            productId = "p1",
            name = "Sugar",
            sku = "SUG-1",
            unit = "kg",
            unitPrice = 60.0,
            quantity = 0.5,
            overrideUnitPrice = 55.0,
            itemDiscountAmount = 2.5
        )

        assertEquals(55.0, line.effectiveUnitPrice, 0.0)
        assertEquals(25.0, line.lineTotal, 0.0)
    }

    @Test
    fun legacyArraySnapshotRemainsReadable() {
        val raw = JSONArray().put(JSONObject().apply {
            put("productId", line.productId)
            put("name", line.name)
            put("sku", line.sku)
            put("unit", line.unit)
            put("unitPrice", line.unitPrice)
            put("quantity", line.quantity)
            put("overrideUnitPrice", JSONObject.NULL)
            put("itemDiscountAmount", line.itemDiscountAmount)
        }).toString()

        assertEquals(listOf(line), ActiveCartStore.decode(raw))
    }

    @Test
    fun unknownSchemaVersionIsRejected() {
        val raw = JSONObject().apply {
            put("schemaVersion", 99)
            put("lines", JSONArray())
        }.toString()

        assertTrue(ActiveCartStore.decode(raw).isEmpty())
    }

    @Test
    fun versionedSnapshotReadsValidLines() {
        val raw = JSONObject().apply {
            put("schemaVersion", 1)
            put("lines", JSONArray().put(JSONObject().apply {
                put("productId", line.productId)
                put("name", line.name)
                put("sku", line.sku)
                put("unit", line.unit)
                put("unitPrice", line.unitPrice)
                put("quantity", line.quantity)
                put("overrideUnitPrice", JSONObject.NULL)
                put("itemDiscountAmount", line.itemDiscountAmount)
            }))
        }.toString()

        assertEquals(listOf(line), ActiveCartStore.decode(raw))
    }

    @Test
    fun invalidLineStillRejectsWholeSnapshot() {
        val raw = JSONObject().apply {
            put("schemaVersion", 1)
            put("lines", JSONArray()
                .put(JSONObject().apply {
                    put("productId", "p1")
                    put("name", "Sugar")
                    put("unit", "kg")
                    put("unitPrice", 70.0)
                    put("quantity", 1.0)
                    put("overrideUnitPrice", JSONObject.NULL)
                    put("itemDiscountAmount", 0.0)
                })
                .put(JSONObject().apply {
                    put("productId", "p2")
                    put("name", "Rice")
                    put("unit", "kg")
                    put("unitPrice", 80.0)
                    put("quantity", -1.0)
                    put("overrideUnitPrice", JSONObject.NULL)
                    put("itemDiscountAmount", 0.0)
                }))
        }.toString()

        assertTrue(ActiveCartStore.decode(raw).isEmpty())
    }
}
