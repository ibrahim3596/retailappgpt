package com.retailpos.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartManagerTest {
    private fun product(stock: Double = 5.0, id: String = "p1") = ProductEntity(
        id = id, storeId = "store", name = "Milk", brand = "Brand",
        barcode = "8901234567890", sku = "MILK-1", mrp = 60.0,
        sellingPrice = 55.0, purchasePrice = 50.0, stock = stock,
        unit = "pcs", lowStockThreshold = 2.0, updatedAt = 1L
    )

    @Test fun firstScanAddsOneLine() {
        val cart = CartManager()
        assertEquals(AddToCartResult.Added, cart.add(product()))
        assertEquals(1, cart.lines.size)
        assertEquals(1.0, cart.lines.single().quantity, 0.0)
    }

    @Test fun repeatedScanIncrementsExistingLine() {
        val cart = CartManager()
        cart.add(product(stock = 3.0))
        assertEquals(AddToCartResult.Added, cart.add(product(stock = 3.0)))
        assertEquals(2.0, cart.lines.single().quantity, 0.0)
    }

    @Test fun scanCannotExceedAvailableStock() {
        val cart = CartManager()
        val item = product(stock = 1.0)
        cart.add(item)
        assertEquals(AddToCartResult.InsufficientStock, cart.add(item))
        assertEquals(1.0, cart.lines.single().quantity, 0.0)
    }

    @Test fun zeroStockIsRejected() {
        val cart = CartManager()
        assertEquals(AddToCartResult.OutOfStock, cart.add(product(stock = 0.0)))
        assertTrue(cart.lines.isEmpty())
    }

    @Test fun differentProductsRemainSeparateLines() {
        val cart = CartManager()
        cart.add(product(id = "p1"))
        cart.add(product(id = "p2"))
        assertEquals(2, cart.lines.size)
    }

    @Test fun removeDeletesOnlyRequestedLine() {
        val cart = CartManager()
        val cartManager = CartManager()
        cartManager.add(product(id = "p1"))
        cartManager.add(product(id = "p2"))
        assertTrue(cartManager.remove("p1"))
        assertEquals(listOf("p2"), cartManager.lines.map { it.productId })
        assertTrue(cart.lines.isEmpty())
    }
}
