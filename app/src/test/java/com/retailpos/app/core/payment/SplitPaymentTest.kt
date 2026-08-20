package com.retailpos.app.core.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SplitPaymentTest {
    @Test
    fun acceptsExactSplit() {
        val parts = listOf(
            SplitPaymentPart("CASH", 40.0),
            SplitPaymentPart("UPI", 60.0)
        )
        assertNull(SplitPaymentRules.validate(100.0, parts))
    }

    @Test
    fun rejectsIncompleteSplit() {
        val parts = listOf(
            SplitPaymentPart("CASH", 40.0),
            SplitPaymentPart("UPI", 50.0)
        )
        assertNotNull(SplitPaymentRules.validate(100.0, parts))
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val original = listOf(
            SplitPaymentPart("CASH", 40.0),
            SplitPaymentPart("CARD", 60.0)
        )
        assertEquals(original, SplitPaymentRules.decode(SplitPaymentRules.encode(original)))
    }
}
