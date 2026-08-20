package com.retailpos.app.core.payments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PaymentRulesTest {
    @Test
    fun cashCalculatesChange() {
        val result = PaymentRules.validate(PaymentMethod.CASH, 87.50, 100.0)
        assertEquals(12.50, result.change, 0.0001)
        assertEquals(100.0, result.amountTendered, 0.0001)
    }

    @Test
    fun cashRejectsInsufficientTender() {
        assertThrows(IllegalArgumentException::class.java) {
            PaymentRules.validate(PaymentMethod.CASH, 100.0, 99.99)
        }
    }

    @Test
    fun electronicPaymentMustMatchTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            PaymentRules.validate(PaymentMethod.UPI, 100.0, 99.0)
        }
        val result = PaymentRules.validate(PaymentMethod.CARD, 100.0, 100.0)
        assertEquals(0.0, result.change, 0.0001)
    }

    @Test
    fun creditDoesNotRequireCashTender() {
        val result = PaymentRules.validate(PaymentMethod.CREDIT, 250.0, 0.0)
        assertEquals(0.0, result.amountTendered, 0.0001)
        assertEquals(0.0, result.change, 0.0001)
    }
}
