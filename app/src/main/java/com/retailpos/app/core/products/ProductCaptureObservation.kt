package com.retailpos.app.core.products

/** Normalized evidence captured from one or more camera frames. */
data class ProductCaptureObservation(
    val barcode: String? = null,
    val printedName: String? = null,
    val printedBrand: String? = null,
    val mrp: Double? = null,
    val categoryHint: String? = null,
    val categoryConfidence: Float? = null,
    val pack: ParsedPack? = null,
    val frameCount: Int = 1
)

object ProductCaptureObservationRules {
    fun fingerprint(observation: ProductCaptureObservation): String = listOf(
        observation.barcode?.trim().orEmpty(),
        observation.printedName?.trim()?.lowercase().orEmpty(),
        observation.printedBrand?.trim()?.lowercase().orEmpty(),
        observation.mrp?.toString().orEmpty(),
        observation.categoryHint?.trim()?.lowercase().orEmpty(),
        observation.pack?.size?.toString().orEmpty(),
        observation.pack?.unit.orEmpty().lowercase()
    ).joinToString("|")

    fun hasIdentityEvidence(observation: ProductCaptureObservation): Boolean =
        !observation.barcode.isNullOrBlank() ||
            !observation.printedName.isNullOrBlank() ||
            !observation.printedBrand.isNullOrBlank()

    fun repeatedEvidence(observation: ProductCaptureObservation): Boolean = observation.frameCount >= 2
}
