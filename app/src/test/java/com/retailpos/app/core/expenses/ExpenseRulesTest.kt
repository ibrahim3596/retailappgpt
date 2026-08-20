package com.retailpos.app.core.expenses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseRulesTest {
    @Test
    fun `valid expense passes`() {
        assertNull(ExpenseRules.validate(250.0, "ELECTRICITY", "UPI", "Bill"))
    }

    @Test
    fun `invalid amount fails`() {
        assertEquals(
            "Expense amount must be greater than zero.",
            ExpenseRules.validate(0.0, "ELECTRICITY", "UPI", "")
        )
    }

    @Test
    fun `invalid category fails`() {
        assertEquals(
            "Select a valid expense category.",
            ExpenseRules.validate(100.0, "INVALID", "CASH", "")
        )
    }
}
