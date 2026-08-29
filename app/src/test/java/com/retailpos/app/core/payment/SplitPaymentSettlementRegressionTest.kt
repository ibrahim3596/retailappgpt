package com.retailpos.app.core.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SplitPaymentSettlementRegressionTest {
    @Test
    fun exactTwoTenderSplit_isAcceptedAndRoundTrips() {
        val parts = listOf(
            SplitPaymentPart("CASH", 400.0),
            SplitPaymentPart("UPI", 600.0)
        )

        assertNull(SplitPaymentRules.validate(1000.0, parts))
        val encoded = SplitPaymentRules.encode(parts)
        val decoded = SplitPaymentRules.decode(encoded)

        assertNotNull(decoded)
        assertEquals(parts, decoded)
    }

    @Test
    fun negativeTender_isRejected() {
        val parts = listOf(
            SplitPaymentPart("CASH", -1.0),
            SplitPaymentPart("UPI", 1001.0)
        )

        assertNotNull(SplitPaymentRules.validate(1000.0, parts))
    }
}
