package com.retailpos.app.data

import com.retailpos.app.core.identifiers.ProductIdentifierValidator
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Best-effort public catalog enrichment plus persistent barcode-backed reuse. */
data class CatalogProduct(
    val name: String?,
    val brand: String?,
    val category: String?,
    val quantity: String?,
    val imageUrl: String?
)

object ProductCatalogLookup {
    private const val USER_AGENT = "RetailPOS/0.1 (product-catalog-enrichment)"
    private const val CACHE_LIMIT = 256
    private val cache = ConcurrentHashMap<String, CatalogProduct>()
    @Volatile private var persistentDao: ProductIdentificationCacheDao? = null
    @Volatile private var persistentStoreId: String? = null

    fun configurePersistentCache(storeId: String, dao: ProductIdentificationCacheDao) {
        persistentStoreId = storeId
        persistentDao = dao
    }

    suspend fun lookupByBarcode(barcode: String): CatalogProduct? {
        val clean = ProductIdentifierValidator.normalize(barcode)
        if (clean.isEmpty()) return null

        val dao = persistentDao
        val storeId = persistentStoreId
        if (dao != null && !storeId.isNullOrBlank()) {
            dao.get(storeId, clean)?.let { return it.toCatalogProduct() }
        }
        cache[clean]?.let { return it }

        return try {
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
            try {
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
                ).also {
                    remember(clean, it)
                    if (dao != null && !storeId.isNullOrBlank()) dao.upsert(it.toCacheEntity(storeId, clean, "PUBLIC_CATALOG", 98))
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun remember(barcode: String, value: CatalogProduct) {
        if (cache.size >= CACHE_LIMIT && !cache.containsKey(barcode)) cache.keys.firstOrNull()?.let(cache::remove)
        cache[barcode] = value
    }
}
