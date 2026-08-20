package com.retailpos.app.core.payment

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentSettlementTest {
    @Test
    fun cashCalculatesChange() {
        val result = PaymentSettlementRules.settle("CASH", 137.50, 200.0)
        assertEquals(62.50, result.change, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun cashRejectsInsufficientTender() {
        PaymentSettlementRules.settle("CASH", 137.50, 100.0)
    }

    @Test
    fun electronicPaymentUsesExactPayable() {
        assertEquals(0.0, PaymentSettlementRules.settle("UPI", 250.0).change, 0.0001)
        assertEquals(0.0, PaymentSettlementRules.settle("CARD", 250.0, 250.0).change, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun electronicPaymentRejectsMismatch() {
        PaymentSettlementRules.settle("UPI", 250.0, 200.0)
    }
}
