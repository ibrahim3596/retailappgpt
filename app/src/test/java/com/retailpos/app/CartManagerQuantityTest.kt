package com.retailpos.app

import com.retailpos.app.data.AddToCartResult
import com.retailpos.app.data.CartManager
import com.retailpos.app.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartManagerQuantityTest {
    private fun looseProduct(stock: Double = 10.0) = ProductEntity(
        id = "sugar",
        storeId = "local-store",
        name = "Sugar",
        brand = "",
        barcode = null,
        sku = "SUGAR",
        mrp = 60.0,
        sellingPrice = 50.0,
        purchasePrice = 40.0,
        stock = stock,
        unit = "kg",
        lowStockThreshold = 1.0,
        updatedAt = 1L
    )

    @Test
    fun addsHalfKiloAndCalculatesPrice() {
        val cart = CartManager()
        assertEquals(AddToCartResult.Added, cart.addQuantity(looseProduct(), 0.5))
        assertEquals(0.5, cart.lines.single().quantity, 0.0001)
        assertEquals(25.0, cart.lines.single().lineTotal, 0.0001)
    }

    @Test
    fun rejectsQuantityAboveAvailableStock() {
        val cart = CartManager()
        assertEquals(AddToCartResult.InsufficientStock, cart.addQuantity(looseProduct(0.25), 0.5))
        assertTrue(cart.lines.isEmpty())
    }

    @Test
    fun accumulatesRepeatedFractionalQuantity() {
        val cart = CartManager()
        assertEquals(AddToCartResult.Added, cart.addQuantity(looseProduct(), 0.25))
        assertEquals(AddToCartResult.Added, cart.addQuantity(looseProduct(), 0.5))
        assertEquals(0.75, cart.lines.single().quantity, 0.0001)
        assertEquals(37.5, cart.lines.single().lineTotal, 0.0001)
    }
}
