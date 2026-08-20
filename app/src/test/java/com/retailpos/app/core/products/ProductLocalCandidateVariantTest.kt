package com.retailpos.app.core.products

import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.ProductMetadataEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductLocalCandidateVariantTest {
    private fun product(id: String, name: String) = ProductEntity(
        id = id,
        storeId = "s1",
        name = name,
        brand = "Amul",
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

    private fun metadata(productId: String, size: Double) = ProductMetadataEntity(
        productId = productId,
        storeId = "s1",
        category = "Milk",
        subcategory = "",
        packSize = size,
        packUnit = "g",
        description = "",
        imageUri = null,
        taxRatePercent = 0.0,
        updatedAt = 1L
    )

    @Test
    fun exactPackVariantRanksAboveDifferentPackSize() {
        val exact = product("1", "Amul Taaza Milk")
        val other = product("2", "Amul Taaza Milk")
        val ranked = ProductLocalCandidateRanking.rank(
            queryName = "Amul Taaza Milk",
            queryBrand = "Amul",
            candidates = listOf(other, exact),
            observedPackSize = 500.0,
            observedPackUnit = "g",
            metadataByProductId = mapOf("1" to metadata("1", 500.0), "2" to metadata("2", 1000.0))
        )
        assertEquals("1", ranked.first().product.id)
        assertTrue(ranked.first().score > ranked[1].score)
    }
}
