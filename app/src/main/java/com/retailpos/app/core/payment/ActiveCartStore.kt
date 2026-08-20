package com.retailpos.app.core.payment

import android.content.Context
import com.retailpos.app.data.CartLine
import org.json.JSONArray
import org.json.JSONObject

/** Durable single-store active-bill snapshot used for process-death recovery. */
object ActiveCartStore {
    private const val PREFS = "retailpos_active_cart"
    private const val KEY_LINES = "lines"

    fun save(context: Context, lines: List<CartLine>) {
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
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LINES, json.toString())
            .apply()
    }

    fun load(context: Context): List<CartLine> {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LINES, null)
            ?: return emptyList()
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
                        add(
                            CartLine(
                                productId = item.getString("productId"),
                                name = item.getString("name"),
                                sku = if (item.isNull("sku")) null else item.getString("sku"),
                                unit = item.getString("unit"),
                                unitPrice = unitPrice,
                                quantity = quantity,
                                overrideUnitPrice = override,
                                itemDiscountAmount = discount
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LINES)
            .apply()
    }
}
