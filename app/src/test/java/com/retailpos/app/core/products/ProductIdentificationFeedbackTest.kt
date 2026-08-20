package com.retailpos.app.core.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductIdentificationFeedbackTest {
    @Test
    fun localAcceptanceProducesPositiveBoundedSignal() {
        val signal = ProductIdentificationFeedbackRules.toSignal(ProductIdentificationFeedback(acceptedLocalCandidate = true))
        assertEquals(3, signal.rankingBoost)
    }

    @Test
    fun localRejectionProducesNegativeSignal() {
        val signal = ProductIdentificationFeedbackRules.toSignal(ProductIdentificationFeedback(rejectedLocalCandidate = true))
        assertEquals(-3, signal.rankingBoost)
    }

    @Test
    fun conflictingLocalReviewProducesNoSignal() {
        val signal = ProductIdentificationFeedbackRules.toSignal(
            ProductIdentificationFeedback(acceptedLocalCandidate = true, rejectedLocalCandidate = true)
        )
        assertEquals(0, signal.rankingBoost)
        assertTrue(signal.explanation.contains("Conflicting"))
    }
}
