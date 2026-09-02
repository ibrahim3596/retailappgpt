package com.retailpos.app.core.payment

import com.retailpos.app.data.CartLine
import com.retailpos.app.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveCartRecoveryTest {
    private fun product(id: String, stock: Double, archived: Boolean = false) = ProductEntity(
        id = id,
        storeId = "local-store",
        name = "Product $id",
        brand = "",
        barcode = null,
        sku = id,
        mrp = 80.0,
        sellingPrice = 70.0,
        purchasePrice = 50.0,
        stock = stock,
        unit = "kg",
        lowStockThreshold = 1.0,
        isArchived = archived,
        updatedAt = 1L
    )

    @Test
    fun missingProductIsReported() {
        val line = CartLine("missing", "Missing Sugar", "M1", "kg", 70.0, 1.0)
        val issues = ActiveCartRecovery.validate(listOf(line), emptyMap())
        assertEquals(1, issues.size)
        assertTrue(issues.single() is ActiveCartRecoveryIssue.Missing)
    }

    @Test
    fun archivedProductIsReported() {
        val line = CartLine("p1", "Sugar", "P1", "kg", 70.0, 1.0)
        val issues = ActiveCartRecovery.validate(listOf(line), mapOf("p1" to product("p1", 10.0, archived = true)))
        assertTrue(issues.single() is ActiveCartRecoveryIssue.Archived)
    }

    @Test
    fun insufficientStockIsReported() {
        val line = CartLine("p1", "Sugar", "P1", "kg", 70.0, 2.0)
        val issues = ActiveCartRecovery.validate(listOf(line), mapOf("p1" to product("p1", 1.0)))
        val issue = issues.single()
        assertTrue(issue is ActiveCartRecoveryIssue.InsufficientStock)
        assertEquals(1.0, (issue as ActiveCartRecoveryIssue.InsufficientStock).available, 0.0001)
    }

    @Test
    fun duplicateProductLinesAreReportedAsInvalidPricing() {
        val lines = listOf(
            CartLine("p1", "Sugar", "P1", "kg", 70.0, 1.0),
            CartLine("p1", "Sugar", "P1", "kg", 70.0, 1.0)
        )
        val issues = ActiveCartRecovery.validate(lines, mapOf("p1" to product("p1", 10.0)))
        assertEquals(2, issues.size)
        assertTrue(issues.all { it is ActiveCartRecoveryIssue.InvalidPricing })
    }

    @Test
    fun nonPositiveQuantityIsReportedEvenWhenDiscountClampsTotal() {
        val line = CartLine(
            "p1", "Sugar", "P1", "kg", 70.0, quantity = -1.0, itemDiscountAmount = 100.0
        )
        val issues = ActiveCartRecovery.validate(listOf(line), mapOf("p1" to product("p1", 10.0)))
        assertTrue(issues.single() is ActiveCartRecoveryIssue.InvalidPricing)
    }

    @Test
    fun nonFiniteProductStockIsReportedAsInvalidPricing() {
        val line = CartLine("p1", "Sugar", "P1", "kg", 70.0, 1.0)
        val issues = ActiveCartRecovery.validate(listOf(line), mapOf("p1" to product("p1", Double.NaN)))
        assertTrue(issues.single() is ActiveCartRecoveryIssue.InvalidPricing)
    }

    @Test
    fun negativeProductStockIsReportedAsInvalidPricing() {
        val line = CartLine("p1", "Sugar", "P1", "kg", 70.0, 1.0)
        val issues = ActiveCartRecovery.validate(listOf(line), mapOf("p1" to product("p1", -1.0)))
        assertTrue(issues.single() is ActiveCartRecoveryIssue.InvalidPricing)
    }
}
