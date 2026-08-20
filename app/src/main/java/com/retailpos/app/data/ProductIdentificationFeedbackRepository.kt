package com.retailpos.app.data

import com.retailpos.app.core.products.ProductIdentificationFeedback
import com.retailpos.app.core.products.ProductIdentificationFeedbackRules
import com.retailpos.app.core.products.ProductIdentificationFeedbackSignal
import java.util.UUID

class ProductIdentificationFeedbackRepository(
    private val dao: ProductIdentificationFeedbackDao
) {
    suspend fun record(
        storeId: String,
        barcode: String?,
        candidateKey: String?,
        feedback: ProductIdentificationFeedback
    ): ProductIdentificationFeedbackSignal {
        val signal = ProductIdentificationFeedbackRules.toSignal(feedback)
        dao.insert(
            ProductIdentificationFeedbackEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                barcode = barcode,
                candidateKey = candidateKey,
                outcome = when {
                    feedback.acceptedLocalCandidate -> "ACCEPTED_LOCAL"
                    feedback.rejectedLocalCandidate -> "REJECTED_LOCAL"
                    feedback.acceptedCatalog -> "ACCEPTED_CATALOG"
                    feedback.rejectedCatalog -> "REJECTED_CATALOG"
                    feedback.retainedCameraData -> "RETAINED_CAMERA"
                    feedback.correctedName || feedback.correctedBrand || feedback.correctedPack -> "CORRECTED"
                    else -> "NO_SIGNAL"
                },
                rankingBoost = signal.rankingBoost,
                explanation = signal.explanation,
                createdAt = System.currentTimeMillis()
            )
        )
        return signal
    }

    suspend fun rankingBoost(storeId: String, candidateKey: String): Int =
        dao.rankingBoostForCandidate(storeId, candidateKey).coerceIn(-8, 8)
}
