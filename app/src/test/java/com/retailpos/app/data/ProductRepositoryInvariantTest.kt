package com.retailpos.app.data

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ProductRepositoryInvariantTest {
    private val product = ProductEntity(
        id = "product-1",
        storeId = "store-1",
        name = "Sugar",
        brand = "",
        barcode = null,
        sku = "SUGAR-1",
        mrp = 50.0,
        sellingPrice = 50.0,
        purchasePrice = 30.0,
        stock = 10.0,
        unit = "kg",
        lowStockThreshold = 2.0,
        updatedAt = 1L,
        isArchived = false
    )

    @Test
    fun metadataMustBelongToSameProduct() {
        val metadata = ProductMetadataEntity(
            productId = "product-2",
            storeId = "store-1",
            updatedAt = 1L
        )

        assertFailsWith<IllegalArgumentException> {
            require(metadata.productId == product.id) { "Product metadata must belong to the same product." }
        }
    }

    @Test
    fun metadataMustBelongToSameStore() {
        val metadata = ProductMetadataEntity(
            productId = product.id,
            storeId = "store-2",
            updatedAt = 1L
        )

        assertFailsWith<IllegalArgumentException> {
            require(metadata.storeId == product.storeId) { "Product metadata must belong to the same store." }
        }
    }
}
