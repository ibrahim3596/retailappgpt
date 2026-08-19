package com.retailpos.app.data

enum class IdentificationConfidence {
    HIGH,
    GOOD,
    MEDIUM,
    LOW,
    NONE
}

data class ProductIdentificationEvidence(
    val barcodeDetected: Boolean = false,
    val catalogMatched: Boolean = false,
    val printedTextDetected: Boolean = false,
    val visualHintDetected: Boolean = false,
    val barcodeMatchesCatalog: Boolean = false
)

object ProductIdentificationConfidence {
    fun evaluate(evidence: ProductIdentificationEvidence): IdentificationConfidence = when {
        evidence.barcodeDetected && evidence.catalogMatched && evidence.barcodeMatchesCatalog -> IdentificationConfidence.HIGH
        evidence.barcodeDetected && (evidence.printedTextDetected || evidence.catalogMatched) -> IdentificationConfidence.GOOD
        evidence.printedTextDetected -> IdentificationConfidence.MEDIUM
        evidence.visualHintDetected -> IdentificationConfidence.LOW
        else -> IdentificationConfidence.NONE
    }
}
