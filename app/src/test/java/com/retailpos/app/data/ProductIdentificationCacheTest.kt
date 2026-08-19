package com.retailpos.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductIdentificationCacheTest {
    @Test
    fun roundTripPreservesCatalogIdentity() {
        val catalog = CatalogProduct(
            name = "Sample Product",
            brand = "Sample Brand",
            category = "Beverages",
            quantity = "500 ml",
            imageUrl = "https://example.com/image.jpg"
        )
        val cached = catalog.toCacheEntity("local-store", "8901234567890", "PUBLIC_CATALOG", 98, 1234L)
        val restored = cached.toCatalogProduct()

        assertEquals(catalog, restored)
        assertEquals("local-store", cached.storeId)
        assertEquals("8901234567890", cached.barcode)
        assertEquals("PUBLIC_CATALOG", cached.source)
        assertEquals(98, cached.confidence)
        assertEquals(1234L, cached.updatedAt)
    }

    @Test
    fun nullableCatalogFieldsStayNullable() {
        val cached = CatalogProduct(null, null, null, null, null)
            .toCacheEntity("local-store", "8900000000000", "PUBLIC_CATALOG", 98, 1234L)

        assertNull(cached.name)
        assertNull(cached.brand)
        assertNull(cached.category)
        assertNull(cached.quantity)
        assertNull(cached.imageUrl)
    }
}
