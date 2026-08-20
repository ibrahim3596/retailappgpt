package com.retailpos.app.core.returns

import org.junit.Assert.assertEquals
import org.junit.Test

class ReturnRulesTest {
    @Test
    fun rejectsMoreThanRemainingQuantity() {
        assertEquals("Return quantity exceeds the remaining quantity", ReturnRules.validateQuantity(3.0, 2.0))
    }

    @Test
    fun acceptsPartialReturnWithinRemainingQuantity() {
        assertEquals(null, ReturnRules.validateQuantity(1.5, 2.0))
    }

    @Test
    fun rejectsRefundAboveMaximum() {
        assertEquals("Refund cannot exceed the refundable amount", ReturnRules.validateRefundAmount(101.0, 100.0))
    }
}
