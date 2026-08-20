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
        sku = id,
        barcode = null,
        brand = "",
        category = "",
        subcategory = "",
        unit = "kg",
        packSize = 1.0,
        description = "",
        imagePath = null,
        purchasePrice = 50.0,
        sellingPrice = 70.0,
        mrp = 80.0,
        stock = stock,
        lowStockThreshold = 1.0,
        isArchived = archived,
        createdAt = 1L,
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
}
