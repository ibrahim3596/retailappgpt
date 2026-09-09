package com.retailpos.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartManagerTest2 {
    private fun product(stock: Double = 5.0, id: String = "p1", sellingPrice: Double = 55.0) = ProductEntity(
        id = id, storeId = "store", name = "Milk", brand = "Brand", barcode = "8901234567890",
        sku = "MILK-1", mrp = 60.0, sellingPrice = sellingPrice, purchasePrice = 50.0,
        stock = stock, unit = "pcs", lowStockThreshold = 2.0, updatedAt = 1L
    )
    @Test fun firstScanAddsOneLine() {
        val cart = CartManager()
        assertEquals(AddToCartResult.Added, cart.add(product()))
        assertEquals(1, cart.lines.size)
        assertEquals(1.0, cart.lines.single().quantity, 0.0)
    }
    @Test fun repeatedScanIncrementsExistingLine() {
        val cart = CartManager(); cart.add(product(stock = 3.0))
        assertEquals(AddToCartResult.Added, cart.add(product(stock = 3.0)))
        assertEquals(2.0, cart.lines.single().quantity, 0.0)
    }
    @Test fun scanCannotExceedAvailableStock() {
        val cart = CartManager(); val item = product(stock = 1.0); cart.add(item)
        assertEquals(AddToCartResult.InsufficientStock, cart.add(item))
        assertEquals(1.0, cart.lines.single().quantity, 0.0)
    }
    @Test fun zeroStockIsRejected() {
        val cart = CartManager()
        assertEquals(AddToCartResult.OutOfStock, cart.add(product(stock = 0.0)))
        assertTrue(cart.lines.isEmpty())
    }
    @Test fun nonFiniteStockIsRejected() {
        val cart = CartManager()
        assertEquals(AddToCartResult.OutOfStock, cart.add(product(stock = Double.NaN)))
        assertEquals(AddToCartResult.OutOfStock, cart.add(product(stock = Double.POSITIVE_INFINITY, id = "p2")))
        assertTrue(cart.lines.isEmpty())
    }
    @Test fun nonFiniteOrNegativeSellingPriceIsRejected() {
        val cart = CartManager()
        assertEquals(AddToCartResult.OutOfStock, cart.add(product(sellingPrice = Double.NaN)))
        assertEquals(AddToCartResult.OutOfStock, cart.add(product(sellingPrice = -1.0, id = "p2")))
        assertTrue(cart.lines.isEmpty())
    }
    @Test fun invalidAvailableStockCannotChangeQuantity() {
        val cart = CartManager(); cart.add(product())
        assertEquals(AddToCartResult.OutOfStock, cart.setQuantity("p1", 2.0, Double.NaN))
        assertEquals(1.0, cart.lines.single().quantity, 0.0)
    }
    @Test fun differentProductsRemainSeparateLines() {
        val cart = CartManager(); cart.add(product(id = "p1")); cart.add(product(id = "p2"))
        assertEquals(2, cart.lines.size)
    }
    @Test fun removeDeletesOnlyRequestedLine() {
        val cart = CartManager(); cart.add(product(id = "p1")); cart.add(product(id = "p2"))
        assertTrue(cart.remove("p1")); assertEquals(listOf("p2"), cart.lines.map { it.productId })
    }
}
