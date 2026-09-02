package com.retailpos.app.core.payment

import org.junit.Test

class PaymentSettlementEdgeCasesTest {
    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFinitePayableAmount() {
        PaymentSettlementRules.settle("CASH", Double.NaN, 100.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedPaymentMethod() {
        PaymentSettlementRules.settle("WALLET", 100.0, 100.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteCashTender() {
        PaymentSettlementRules.settle("CASH", 100.0, Double.POSITIVE_INFINITY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteElectronicTender() {
        PaymentSettlementRules.settle("CARD", 100.0, Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeCashTenderEvenWithinTolerance() {
        PaymentSettlementRules.settle("CASH", 0.0, -0.0000005)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeElectronicTenderForZeroTotal() {
        PaymentSettlementRules.settle("CARD", 0.0, -0.005)
    }
}
