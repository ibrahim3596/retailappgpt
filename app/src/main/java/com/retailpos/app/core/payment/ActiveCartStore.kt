package com.retailpos.app.core.payment

import android.content.Context
import com.retailpos.app.data.CartLine
import org.json.JSONArray
import org.json.JSONObject

/** Durable single-store active-bill snapshot used for process-death recovery. */
object ActiveCartStore {
    private const val PREFS = "retailpos_active_cart"
    private const val KEY_LINES = "lines"

    @Volatile private var prefs: android.content.SharedPreferences? = null

    fun configure(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun save(lines: List<CartLine>) {
        val json = JSONArray()
        lines.forEach { line ->
            json.put(JSONObject().apply {
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
        prefs?.edit()?.putString(KEY_LINES, json.toString())?.apply()
    }

    fun load(): List<CartLine> {
        val raw = prefs?.getString(KEY_LINES, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    val quantity = item.getDouble("quantity")
                    val unitPrice = item.getDouble("unitPrice")
                    val override = if (item.isNull("overrideUnitPrice")) null else item.getDouble("overrideUnitPrice")
                    val discount = item.optDouble("itemDiscountAmount", 0.0)
                    if (quantity.isFinite() && quantity > 0.0 && unitPrice.isFinite() && unitPrice >= 0.0 &&
                        (override == null || (override.isFinite() && override >= 0.0)) && discount.isFinite() && discount >= 0.0
                    ) {
                        add(CartLine(item.getString("productId"), item.getString("name"), if (item.isNull("sku")) null else item.getString("sku"), item.getString("unit"), unitPrice, quantity, override, discount))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clear() {
        prefs?.edit()?.remove(KEY_LINES)?.apply()
    }
}
