package com.retailpos.app.core.offline

import com.retailpos.app.data.CartLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveCartRecoveryTest {
    private fun line(id: String, quantity: Double = 1.0): CartLine = CartLine(id, id, null, "kg", 100.0, quantity)

    @Test fun keeps_valid_lines() {
        val result = ActiveCartRecovery.evaluate(listOf(line("sugar")), mapOf("sugar" to RecoveryProduct("Sugar", "kg", 5.0, false)))
        assertEquals(1, result.validLines.size)
        assertTrue(result.issues.isEmpty())
    }

    @Test fun detects_missing_and_archived_and_stock() {
        val result = ActiveCartRecovery.evaluate(
            listOf(line("missing"), line("archived"), line("low", 4.0)),
            mapOf("archived" to RecoveryProduct("Oil", "litre", 2.0, true), "low" to RecoveryProduct("Rice", "kg", 2.0, false))
        )
        assertEquals(3, result.issues.size)
        assertEquals(setOf(CartRecoveryIssueType.MISSING_PRODUCT, CartRecoveryIssueType.ARCHIVED_PRODUCT, CartRecoveryIssueType.INSUFFICIENT_STOCK), result.issues.map { it.type }.toSet())
    }

    @Test fun rejects_non_finite_or_negative_current_stock() {
        val result = ActiveCartRecovery.evaluate(
            listOf(line("nan"), line("infinite"), line("negative")),
            mapOf(
                "nan" to RecoveryProduct("Rice", "kg", Double.NaN, false),
                "infinite" to RecoveryProduct("Oil", "litre", Double.POSITIVE_INFINITY, false),
                "negative" to RecoveryProduct("Sugar", "kg", -1.0, false)
            )
        )
        assertEquals(3, result.issues.size)
        assertTrue(result.issues.all { it.type == CartRecoveryIssueType.INVALID_PRICING })
    }
}
