package com.retailpos.app.core.customer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KhataRulesTest {
    @Test
    fun partialPaymentLeavesRemainingBalance() {
        assertEquals(300.0, KhataRules.balanceAfterPayment(500.0, 200.0)!!, 0.000001)
    }

    @Test
    fun fullPaymentSettlesBalance() {
        assertEquals(0.0, KhataRules.balanceAfterPayment(500.0, 500.0)!!, 0.000001)
    }

    @Test
    fun overpaymentRejected() {
        assertEquals("Payment cannot exceed the outstanding amount", KhataRules.validatePayment(500.0, 500.01))
        assertNull(KhataRules.validatePayment(500.0, 500.0))
    }

    @Test
    fun displayStatesAreDeterministic() {
        assertEquals(KhataState.DUE, KhataRules.displayState(10.0))
        assertEquals(KhataState.CREDIT, KhataRules.displayState(-10.0))
        assertEquals(KhataState.SETTLED, KhataRules.displayState(0.0))
    }
}
