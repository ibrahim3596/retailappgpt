package com.retailpos.app.core.products

import com.retailpos.app.data.CatalogProduct
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductCatalogConflictResolverTest {
    @Test
    fun barcodeCatalogMatchWinsWithReviewOfStoreFields() {
        val result = ProductCatalogConflictResolver.resolve(
            observation = ProductCaptureObservation(barcode = "8901234567890", printedName = "Milk"),
            localCandidateScore = 60,
            catalogCandidate = CatalogProduct("Milk", "Brand", null, "1 L", null),
            barcodeMatchesCatalog = true,
            localTextAgrees = true
        )
        assertEquals(ProductResolution.CATALOG, result.resolution)
    }

    @Test
    fun strongLocalAndCatalogDisagreementRequiresConflictReview() {
        val result = ProductCatalogConflictResolver.resolve(
            observation = ProductCaptureObservation(barcode = "8901234567890", printedName = "Local Milk"),
            localCandidateScore = 90,
            catalogCandidate = CatalogProduct("Different Milk", "Other", null, "1 L", null),
            barcodeMatchesCatalog = true,
            localTextAgrees = false
        )
        assertEquals(ProductResolution.CONFLICT, result.resolution)
    }

    @Test
    fun weakCandidatesNeverBecomeAutomaticResolution() {
        val result = ProductCatalogConflictResolver.resolve(
            observation = ProductCaptureObservation(printedName = "Milk"),
            localCandidateScore = 62,
            catalogCandidate = CatalogProduct("Milk", "Brand", null, "1 L", null),
            barcodeMatchesCatalog = false,
            localTextAgrees = false
        )
        assertEquals(ProductResolution.REVIEW_REQUIRED, result.resolution)
    }
}
