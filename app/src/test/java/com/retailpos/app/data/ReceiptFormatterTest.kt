package com.retailpos.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFormatterTest {
    @Test
    fun cashReceiptShowsTenderAndChange() {
        val sale = SaleEntity(
            id = "sale-1",
            storeId = "store",
            customerId = null,
            subtotal = 100.0,
            discountAmount = 0.0,
            taxAmount = 0.0,
            total = 100.0,
            paymentMethod = "CASH",
            amountTendered = 150.0,
            changeAmount = 50.0,
            idempotencyKey = "key-1",
            createdAt = 0L
        )
        val receipt = ReceiptFormatter.format(sale, emptyList())
        assertTrue(receipt.contains("TOTAL: ₹100.00"))
        assertTrue(receipt.contains("CASH RECEIVED: ₹150.00"))
        assertTrue(receipt.contains("CHANGE: ₹50.00"))
    }
}
