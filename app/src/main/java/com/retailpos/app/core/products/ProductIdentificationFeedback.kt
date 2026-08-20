package com.retailpos.app.core.products

data class ProductIdentificationFeedback(
    val acceptedCatalog: Boolean,
    val rejectedCatalog: Boolean,
    val retainedCameraData: Boolean,
    val correctedName: Boolean,
    val correctedBrand: Boolean,
    val correctedPack: Boolean
)

data class ProductIdentificationFeedbackSignal(
    val rankingBoost: Int,
    val explanation: String
)

object ProductIdentificationFeedbackRules {
    fun toSignal(feedback: ProductIdentificationFeedback): ProductIdentificationFeedbackSignal {
        if (feedback.acceptedCatalog && feedback.rejectedCatalog) {
            return ProductIdentificationFeedbackSignal(0, "Conflicting review actions; do not learn from this event.")
        }
        var boost = 0
        if (feedback.acceptedCatalog) boost += 2
        if (feedback.rejectedCatalog) boost -= 2
        if (feedback.retainedCameraData) boost += 1
        if (feedback.correctedName) boost -= 1
        if (feedback.correctedBrand) boost -= 1
        if (feedback.correctedPack) boost -= 1
        return ProductIdentificationFeedbackSignal(
            rankingBoost = boost.coerceIn(-4, 3),
            explanation = when {
                feedback.rejectedCatalog || feedback.correctedName || feedback.correctedBrand || feedback.correctedPack -> "Retailer correction indicates lower trust in the original candidate."
                feedback.acceptedCatalog -> "Retailer accepted the catalog candidate; this may be used as future ranking evidence."
                feedback.retainedCameraData -> "Retailer preferred camera/OCR data over the catalog candidate."
                else -> "No useful correction signal recorded."
            }
        )
    }
}
