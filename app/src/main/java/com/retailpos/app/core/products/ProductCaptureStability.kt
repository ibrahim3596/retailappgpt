package com.retailpos.app.core.products

data class ProductCaptureStability(
    val stable: Boolean,
    val explanation: String
)

object ProductCaptureStabilityRules {
    fun evaluate(observation: ProductCaptureObservation): ProductCaptureStability = when {
        !observation.barcode.isNullOrBlank() -> ProductCaptureStability(
            stable = true,
            explanation = "Barcode evidence is present; additional frames improve confidence but are not required for initial review."
        )
        observation.frameCount >= 2 && (!observation.printedName.isNullOrBlank() || !observation.printedBrand.isNullOrBlank()) -> ProductCaptureStability(
            stable = true,
            explanation = "Printed identity evidence repeated across multiple frames."
        )
        observation.categoryHint != null -> ProductCaptureStability(
            stable = false,
            explanation = "Only a visual category hint is available; capture more identity evidence or enter the product manually."
        )
        else -> ProductCaptureStability(
            stable = false,
            explanation = "Identity evidence is not yet stable enough for a capture suggestion."
        )
    }
}
