package com.retailpos.app.core.khata

import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.CustomerLedgerEntry
import org.junit.Assert.assertTrue
import org.junit.Test

class KhataStatementRulesTest {
    @Test
    fun statementContainsCustomerBalanceAndEntries() {
        val customer = CustomerEntity("c1", "store", "Ravi", "999", "", 1L, 1L)
        val entry = CustomerLedgerEntry("e1", "store", "c1", 125.0, "CREDIT_SALE", "Sale 1", "SALE", "s1", 2L)
        val statement = KhataStatementRules.format(customer, 125.0, listOf(entry))
        assertTrue(statement.contains("Ravi"))
        assertTrue(statement.contains("₹125.00"))
        assertTrue(statement.contains("CREDIT SALE"))
    }
}
