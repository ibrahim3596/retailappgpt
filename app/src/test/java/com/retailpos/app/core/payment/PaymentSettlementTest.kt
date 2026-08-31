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

    @Test
    fun exactSplitSettlementIsAcceptedAsPaid() {
        val result = PaymentSettlementRules.settle("SPLIT:CASH=40,UPI=60", 100.0)
        assertEquals(100.0, result.amountTendered ?: 0.0, 0.0001)
        assertEquals(0.0, result.change, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedSplitSettlementCannotComplete() {
        PaymentSettlementRules.settle("SPLIT:CASH=-40,UPI=140", 100.0)
    }

    @Test
    fun zeroPayableAcceptsOnlyZeroCashTender() {
        assertEquals(0.0, PaymentSettlementRules.settle("CASH", 0.0, 0.0).change, 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroPayableRejectsPositiveCashTender() {
        PaymentSettlementRules.settle("CASH", 0.0, 0.009)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroPayableRejectsPositiveElectronicTender() {
        PaymentSettlementRules.settle("UPI", 0.0, 0.009)
    }
}
