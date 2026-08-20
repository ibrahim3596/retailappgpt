package com.retailpos.app.core.products

import com.retailpos.app.data.CatalogProduct

/**
 * Resolves local/catalog candidates without silently choosing when strong evidence conflicts.
 */
enum class ProductResolution { LOCAL, CATALOG, CONFLICT, REVIEW_REQUIRED, NONE }

data class ProductResolutionResult(
    val resolution: ProductResolution,
    val confidence: Int,
    val explanation: String
)

object ProductCatalogConflictResolver {
    fun resolve(
        observation: ProductCaptureObservation,
        localCandidateScore: Int?,
        catalogCandidate: CatalogProduct?,
        barcodeMatchesCatalog: Boolean,
        localTextAgrees: Boolean
    ): ProductResolutionResult {
        val barcode = !observation.barcode.isNullOrBlank()
        val localStrong = localCandidateScore != null && localCandidateScore >= 80
        val catalogStrong = catalogCandidate != null && (barcodeMatchesCatalog || barcode)

        if (localStrong && catalogStrong && !localTextAgrees && barcodeMatchesCatalog) {
            return ProductResolutionResult(
                ProductResolution.CONFLICT,
                55,
                "Local product and public catalog disagree. Barcode evidence favors the catalog, while local product evidence differs. Review both before choosing."
            )
        }

        if (barcodeMatchesCatalog && catalogCandidate != null) {
            return ProductResolutionResult(
                ProductResolution.CATALOG,
                94,
                "The scanned barcode matches the public catalog candidate. Retailer-controlled price, stock and SKU still require review."
            )
        }

        if (localStrong && localTextAgrees) {
            return ProductResolutionResult(
                ProductResolution.LOCAL,
                localCandidateScore.coerceIn(0, 99),
                "The retailer's existing product matches the captured name/brand evidence. Opening the existing product avoids a duplicate."
            )
        }

        if (localCandidateScore != null || catalogCandidate != null) {
            return ProductResolutionResult(
                ProductResolution.REVIEW_REQUIRED,
                maxOf(localCandidateScore ?: 0, if (catalogCandidate != null) 60 else 0).coerceIn(0, 99),
                "Candidate evidence exists, but it is not strong enough to resolve automatically. Review the candidates before saving."
            )
        }

        return ProductResolutionResult(
            ProductResolution.NONE,
            0,
            "No reliable candidate was found. Keep the observed camera/OCR details or enter the product manually."
        )
    }
}
