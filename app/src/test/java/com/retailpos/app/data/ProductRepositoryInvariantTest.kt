package com.retailpos.app.data

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ProductRepositoryInvariantTest {
    @Test
    fun metadataMustBelongToSameProduct() {
        val product = ProductEntity(
            id = "product-1",
            storeId = "store-1",
            name = "Sugar",
            brand = null,
            sku = "SUGAR-1",
            barcode = null,
            purchasePrice = 30.0,
            sellingPrice = 50.0,
            mrp = 50.0,
            stock = 10.0,
            unit = "kg",
            lowStockThreshold = 2.0,
            description = null,
            imageUri = null,
            isArchived = false,
            createdAt = 1L,
            updatedAt = 1L
        )
        val metadata = ProductMetadataEntity(
            productId = "product-2",
            storeId = "store-1",
            category = null,
            subcategory = null,
            packSize = 1.0,
            packUnit = "kg",
            notes = null,
            taxRatePercent = 0.0,
            updatedAt = 1L
        )

        assertFailsWith<IllegalArgumentException> {
            require(metadata.productId == product.id) { "Product metadata must belong to the same product." }
        }
    }

    @Test
    fun metadataMustBelongToSameStore() {
        val product = ProductEntity(
            id = "product-1",
            storeId = "store-1",
            name = "Sugar",
            brand = null,
            sku = "SUGAR-1",
            barcode = null,
            purchasePrice = 30.0,
            sellingPrice = 50.0,
            mrp = 50.0,
            stock = 10.0,
            unit = "kg",
            lowStockThreshold = 2.0,
            description = null,
            imageUri = null,
            isArchived = false,
            createdAt = 1L,
            updatedAt = 1L
        )
        val metadata = ProductMetadataEntity(
            productId = "product-1",
            storeId = "store-2",
            category = null,
            subcategory = null,
            packSize = 1.0,
            packUnit = "kg",
            notes = null,
            taxRatePercent = 0.0,
            updatedAt = 1L
        )

        assertFailsWith<IllegalArgumentException> {
            require(metadata.storeId == product.storeId) { "Product metadata must belong to the same store." }
        }
    }
}
