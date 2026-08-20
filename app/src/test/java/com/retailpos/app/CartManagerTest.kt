package com.retailpos.app

import com.retailpos.app.data.AddToCartResult
import com.retailpos.app.data.CartManager
import com.retailpos.app.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartManagerTest {
    private fun product(stock: Double = 10.0) = ProductEntity(
        id = "sugar-1",
        storeId = "local-store",
        name = "Sugar",
        brand = "",
        sku = "SUG-001",
        barcode = null,
        category = "Grocery",
        subcategory = null,
        packSize = 1.0,
        unit = "kg",
        description = null,
        imageUri = null,
        purchasePrice = 35.0,
        sellingPrice = 50.0,
        mrp = 55.0,
        stock = stock,
        lowStockThreshold = 1.0,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun setQuantitySupportsFractionalLooseGoods() {
        val manager = CartManager()
        val p = product()
        assertEquals(AddToCartResult.Added, manager.addQuantity(p, 1.0))
        assertEquals(AddToCartResult.Added, manager.setQuantity(p.id, 0.25, p.stock))
        assertEquals(0.25, manager.lines.single().quantity, 0.0001)
    }

    @Test
    fun setQuantityRejectsAboveStock() {
        val manager = CartManager()
        val p = product(stock = 2.0)
        manager.addQuantity(p, 1.0)
        assertEquals(AddToCartResult.InsufficientStock, manager.setQuantity(p.id, 2.5, p.stock))
        assertEquals(1.0, manager.lines.single().quantity, 0.0001)
    }

    @Test
    fun setQuantityRejectsZeroAndNegative() {
        val manager = CartManager()
        val p = product()
        manager.addQuantity(p, 1.0)
        assertEquals(AddToCartResult.InvalidQuantity, manager.setQuantity(p.id, 0.0, p.stock))
        assertEquals(AddToCartResult.InvalidQuantity, manager.setQuantity(p.id, -1.0, p.stock))
        assertEquals(1.0, manager.lines.single().quantity, 0.0001)
    }

    @Test
    fun quantityEditKeepsSameLineAndPricingBasis() {
        val manager = CartManager()
        val p = product()
        manager.addQuantity(p, 0.5)
        manager.setQuantity(p.id, 0.75, p.stock)
        val line = manager.lines.single()
        assertEquals("kg", line.unit)
        assertEquals(50.0, line.unitPrice, 0.0001)
        assertEquals(37.5, line.lineTotal, 0.0001)
        assertTrue(line.productId == p.id)
    }
}
