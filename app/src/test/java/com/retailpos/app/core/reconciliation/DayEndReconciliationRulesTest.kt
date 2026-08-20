package com.retailpos.app.core.reconciliation

import com.retailpos.app.data.SaleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DayEndReconciliationRulesTest {
    private fun sale(id: String, method: String, total: Double) = SaleEntity(
        id = id,
        storeId = "store",
        subtotal = total,
        total = total,
        paymentMethod = method,
        createdAt = 1L,
        idempotencyKey = id,
        customerId = null,
        discountAmount = 0.0,
        taxAmount = 0.0,
        amountTendered = null,
        changeAmount = 0.0
    )

    @Test
    fun summarizeSeparatesPaymentMethods() {
        val summary = DayEndReconciliationRules.summarize(
            listOf(sale("1", "CASH", 100.0), sale("2", "UPI", 250.0), sale("3", "CREDIT", 50.0))
        )
        assertEquals(3, summary.billCount)
        assertEquals(400.0, summary.totalSales, 0.001)
        assertEquals(100.0, summary.cashSales, 0.001)
        assertEquals(250.0, summary.upiSales, 0.001)
        assertEquals(50.0, summary.creditSales, 0.001)
    }

    @Test
    fun cashDifferenceReportsShortOrExcessCash() {
        assertEquals(-25.0, DayEndReconciliationRules.cashDifference(500.0, 475.0), 0.001)
        assertEquals(20.0, DayEndReconciliationRules.cashDifference(500.0, 520.0), 0.001)
    }
}
