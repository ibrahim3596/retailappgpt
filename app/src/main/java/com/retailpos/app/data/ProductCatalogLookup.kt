package com.retailpos.app.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Best-effort public catalog enrichment. Store-specific fields are never supplied by this source. */
data class CatalogProduct(
    val name: String?,
    val brand: String?,
    val category: String?,
    val quantity: String?,
    val imageUrl: String?
)

object ProductCatalogLookup {
    private const val USER_AGENT = "RetailPOS/0.1 (product-catalog-enrichment)"

    fun lookupByBarcode(barcode: String): CatalogProduct? {
        val clean = barcode.trim()
        if (clean.isEmpty()) return null
        val encoded = URLEncoder.encode(clean, Charsets.UTF_8.name())
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$encoded" +
                "?product_type=all&lc=en&cc=in&fields=product_name,brands,categories,quantity,image_front_url"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4_000
            readTimeout = 5_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            if (root.optInt("status", 0) != 1) return null
            val product = root.optJSONObject("product") ?: return null
            CatalogProduct(
                name = product.optString("product_name", "").trim().ifBlank { null },
                brand = product.optString("brands", "").substringBefore(',').trim().ifBlank { null },
                category = product.optString("categories", "").split(',').firstOrNull()?.trim().orEmpty().ifBlank { null },
                quantity = product.optString("quantity", "").trim().ifBlank { null },
                imageUrl = product.optString("image_front_url", "").trim().ifBlank { null }
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
