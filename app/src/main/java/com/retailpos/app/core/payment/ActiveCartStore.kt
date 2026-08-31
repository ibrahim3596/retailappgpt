package com.retailpos.app.core.payment

import android.content.Context
import com.retailpos.app.data.CartLine
import org.json.JSONArray
import org.json.JSONObject

/** Durable single-store active-bill snapshot used for process-death recovery. */
object ActiveCartStore {
    private const val PREFS = "retailpos_active_cart"
    private const val KEY_LINES = "lines"
    private const val KEY_VERSION = "schemaVersion"
    private const val SCHEMA_VERSION = 1

    @Volatile private var prefs: android.content.SharedPreferences? = null

    fun configure(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun save(lines: List<CartLine>) {
        val targetPrefs = prefs ?: return
        val json = JSONObject().apply {
            put(KEY_VERSION, SCHEMA_VERSION)
            put(KEY_LINES, JSONArray().apply {
                lines.forEach { line ->
                    put(JSONObject().apply {
                        put("productId", line.productId)
                        put("name", line.name)
                        put("sku", line.sku ?: JSONObject.NULL)
                        put("unit", line.unit)
                        put("unitPrice", line.unitPrice)
                        put("quantity", line.quantity)
                        put("overrideUnitPrice", line.overrideUnitPrice ?: JSONObject.NULL)
                        put("itemDiscountAmount", line.itemDiscountAmount)
                    })
                }
            })
        }
        targetPrefs.edit().putString(KEY_LINES, json.toString()).apply()
    }

    fun load(): List<CartLine> {
        val raw = prefs?.getString(KEY_LINES, null) ?: return emptyList()
        return decode(raw)
    }

    internal fun decode(raw: String): List<CartLine> = runCatching {
        val root = JSONObject(raw)
        if (root.optInt(KEY_VERSION, -1) != SCHEMA_VERSION) return@runCatching emptyList()
        decodeLines(root.getJSONArray(KEY_LINES))
    }.recoverCatching {
        // Pre-schema snapshots were stored as a bare JSON array. Keep them readable so
        // upgrading the app does not discard an otherwise valid in-progress bill.
        decodeLines(JSONArray(raw))
    }.getOrDefault(emptyList())

    private fun decodeLines(json: JSONArray): List<CartLine> {
        val lines = mutableListOf<CartLine>()
        for (index in 0 until json.length()) {
            val item = json.getJSONObject(index)
            val productId = item.getString("productId").takeIf { it.isNotBlank() } ?: return emptyList()
            val name = item.getString("name")
            val unit = item.getString("unit").takeIf { it.isNotBlank() } ?: return emptyList()
            val quantity = item.getDouble("quantity")
            val unitPrice = item.getDouble("unitPrice")
            val override = if (item.isNull("overrideUnitPrice")) null else item.getDouble("overrideUnitPrice")
            val discount = item.optDouble("itemDiscountAmount", 0.0)
            if (!quantity.isFinite() || quantity <= 0.0 ||
                !unitPrice.isFinite() || unitPrice < 0.0 ||
                (override != null && (!override.isFinite() || override < 0.0)) ||
                !discount.isFinite() || discount < 0.0
            ) {
                return emptyList()
            }
            lines += CartLine(
                productId,
                name,
                if (item.isNull("sku")) null else item.getString("sku"),
                unit,
                unitPrice,
                quantity,
                override,
                discount
            )
        }
        return lines
    }

    fun clear() {
        prefs?.edit()?.remove(KEY_LINES)?.apply()
    }
}
