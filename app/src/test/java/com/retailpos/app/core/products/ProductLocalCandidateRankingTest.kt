package com.retailpos.app.core.products

import com.retailpos.app.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductLocalCandidateRankingTest {
    private fun product(id: String, name: String, brand: String) = ProductEntity(
        id = id,
        storeId = "s1",
        name = name,
        brand = brand,
        barcode = null,
        sku = null,
        mrp = 100.0,
        sellingPrice = 90.0,
        purchasePrice = 70.0,
        stock = 10.0,
        unit = "pcs",
        lowStockThreshold = 2.0,
        updatedAt = 1L
    )

    @Test
    fun ocrNormalizationMakesEquivalentTextComparable() {
        assertEquals("amul taaza milk", ProductCaptureParser.normalizeForMatching("  Amul/Taaza  Milk!!! "))
    }

    @Test
    fun exactNameRanksAbovePartialMatch() {
        val exact = product("1", "Amul Taaza Milk", "Amul")
        val partial = product("2", "Amul Milk", "Other")
        val ranked = ProductLocalCandidateRanking.rank("Amul Taaza Milk", "Amul", listOf(partial, exact))
        assertEquals("1", ranked.first().product.id)
        assertTrue(ranked.first().score > ranked[1].score)
    }

    @Test
    fun weakNameDoesNotBecomeCandidate() {
        val candidate = product("1", "Toothpaste", "Brand")
        val ranked = ProductLocalCandidateRanking.rank("Rice", "", listOf(candidate))
        assertTrue(ranked.isEmpty())
    }
}
