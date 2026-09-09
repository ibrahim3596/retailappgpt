package com.retailpos.app.core.products

data class ProductCaptureStability(
    val stable: Boolean,
    val explanation: String
)

object ProductCaptureStabilityRules {
    fun evaluate(observation: ProductCaptureObservation): ProductCaptureStability = when {
        hasValidBarcode(observation.barcode) -> ProductCaptureStability(
            stable = true,
            explanation = "A valid product barcode is present; review can proceed immediately and other fields remain suggestions."
        )
        observation.frameCount >= 2 && hasRepeatedPrintedIdentity(observation) -> ProductCaptureStability(
            stable = true,
            explanation = "Printed product identity evidence repeated across multiple frames."
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

    private fun hasValidBarcode(barcode: String?): Boolean =
        barcode?.let { ProductBarcodeSafety.classify(it) == ProductBarcodeDecision.ACCEPT } == true

    private fun hasRepeatedPrintedIdentity(observation: ProductCaptureObservation): Boolean =
        !observation.printedName.isNullOrBlank() || !observation.printedBrand.isNullOrBlank()
}
