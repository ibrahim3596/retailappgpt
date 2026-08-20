package com.retailpos.app.core.returns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReturnRulesTest {
    @Test
    fun rejectsMoreThanRemainingQuantity() {
        assertEquals("Return quantity exceeds the remaining quantity", ReturnRules.validateQuantity(3.0, 2.0))
    }

    @Test
    fun acceptsPartialReturnWithinRemainingQuantity() {
        assertNull(ReturnRules.validateQuantity(1.5, 2.0))
    }

    @Test
    fun rejectsRefundAboveMaximum() {
        assertEquals("Refund cannot exceed the refundable amount", ReturnRules.validateRefundAmount(101.0, 100.0))
    }

    @Test
    fun creditSaleRequiresKhataReversal() {
        assertEquals("Credit sales must be refunded through Khata reversal.", ReturnRules.validateRefundMethod("CREDIT", "CASH"))
        assertNull(ReturnRules.validateRefundMethod("CREDIT", "CREDIT_REVERSAL"))
    }

    @Test
    fun nonCreditSaleCannotUseKhataReversal() {
        assertEquals("Khata reversal is only valid for a credit sale.", ReturnRules.validateRefundMethod("CASH", "CREDIT_REVERSAL"))
        assertNull(ReturnRules.validateRefundMethod("UPI", "UPI"))
    }
}
