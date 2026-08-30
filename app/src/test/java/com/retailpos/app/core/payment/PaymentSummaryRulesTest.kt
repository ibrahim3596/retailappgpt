package com.retailpos.app.core.payment

import com.retailpos.app.data.PaymentSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentSummaryRulesTest {
    @Test
    fun expandsSplitPaymentIntoTenderComponents() {
        val result = PaymentSummaryRules.normalize(
            listOf(
                PaymentSummary("CASH", 2, 200.0),
                PaymentSummary("SPLIT:CASH=300.00,UPI=200.00", 1, 500.0)
            )
        )
        assertEquals(listOf("CASH", "UPI"), result.map { it.paymentMethod })
        assertEquals(500.0, result.first { it.paymentMethod == "CASH" }.total, 0.001)
        assertEquals(200.0, result.first { it.paymentMethod == "UPI" }.total, 0.001)
    }

    @Test
    fun ignoresMalformedSplitPaymentInsteadOfReportingItAsPaymentMethod() {
        val result = PaymentSummaryRules.normalize(
            listOf(
                PaymentSummary("CASH", 1, 100.0),
                PaymentSummary("SPLIT:CASH=40.00,BROKEN", 1, 100.0)
            )
        )
        assertEquals(listOf("CASH"), result.map { it.paymentMethod })
        assertEquals(100.0, result.single().total, 0.001)
    }
}
