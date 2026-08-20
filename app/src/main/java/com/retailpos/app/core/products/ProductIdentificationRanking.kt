package com.retailpos.app.core.products

import com.retailpos.app.data.IdentificationConfidence
import com.retailpos.app.data.ProductIdentificationConfidence
import com.retailpos.app.data.ProductIdentificationEvidence

data class ProductIdentificationSignals(
    val barcodeDetected: Boolean = false,
    val barcodeMatchesCatalog: Boolean = false,
    val catalogMatched: Boolean = false,
    val printedTextDetected: Boolean = false,
    val visualHintDetected: Boolean = false
)

data class ProductIdentificationScore(
    val score: Int,
    val confidence: IdentificationConfidence,
    val explanation: String
)

object ProductIdentificationRanking {
    fun score(signals: ProductIdentificationSignals): ProductIdentificationScore {
        val score = when {
            signals.barcodeDetected && signals.barcodeMatchesCatalog -> 98
            signals.barcodeDetected && signals.catalogMatched && signals.printedTextDetected -> 94
            signals.barcodeDetected && signals.catalogMatched -> 90
            signals.barcodeDetected && signals.printedTextDetected -> 85
            signals.barcodeDetected -> 80
            signals.printedTextDetected && signals.visualHintDetected -> 68
            signals.printedTextDetected -> 60
            signals.visualHintDetected -> 40
            else -> 0
        }
        val evidence = ProductIdentificationEvidence(
            barcodeDetected = signals.barcodeDetected,
            catalogMatched = signals.catalogMatched,
            printedTextDetected = signals.printedTextDetected,
            visualHintDetected = signals.visualHintDetected,
            barcodeMatchesCatalog = signals.barcodeMatchesCatalog
        )
        val confidence = ProductIdentificationConfidence.evaluate(evidence)
        val explanation = when {
            signals.barcodeDetected && signals.barcodeMatchesCatalog -> "Barcode matches the catalog candidate. Verify store-controlled price, stock and SKU."
            signals.barcodeDetected && signals.catalogMatched && signals.printedTextDetected -> "Barcode, catalog and printed text agree. Review the candidate before saving."
            signals.barcodeDetected && signals.catalogMatched -> "Barcode has a catalog candidate. Review the candidate before applying catalog fields."
            signals.barcodeDetected && signals.printedTextDetected -> "Barcode and printed text were detected, but the exact catalog identity is not confirmed."
            signals.barcodeDetected -> "A barcode was detected. No independent identity evidence is available yet."
            signals.printedTextDetected -> "Printed text was detected without a reliable barcode match. Treat it as a suggestion."
            signals.visualHintDetected -> "Only a visual category hint is available. Do not treat this as product identity."
            else -> "No reliable product identity evidence was detected."
        }
        return ProductIdentificationScore(score, confidence, explanation)
    }
}
