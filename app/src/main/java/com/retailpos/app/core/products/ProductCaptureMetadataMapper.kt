package com.retailpos.app.core.products

import com.retailpos.app.data.ProductMetadataEntity

object ProductCaptureMetadataMapper {
    fun map(
        productId: String,
        storeId: String,
        observation: ProductCaptureObservation,
        existing: ProductMetadataEntity? = null,
        taxRatePercent: Double = existing?.taxRatePercent ?: 0.0,
        description: String = existing?.description.orEmpty(),
        imageUri: String? = existing?.imageUri
    ): ProductMetadataEntity {
        val pack = observation.pack
        return ProductMetadataEntity(
            productId = productId,
            storeId = storeId,
            category = observation.categoryHint?.trim().orEmpty().ifBlank { existing?.category.orEmpty() },
            subcategory = existing?.subcategory.orEmpty(),
            packSize = pack?.size ?: existing?.packSize,
            packUnit = pack?.unit ?: existing?.packUnit.orEmpty(),
            description = description,
            imageUri = imageUri,
            taxRatePercent = taxRatePercent,
            updatedAt = System.currentTimeMillis()
        )
    }
}
