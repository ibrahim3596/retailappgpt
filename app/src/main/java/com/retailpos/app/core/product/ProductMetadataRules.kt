package com.retailpos.app.core.product

/** Validation and normalization rules for optional product-master metadata. */
object ProductMetadataRules {
    fun normalizeCategory(value: String): String = value.trim()
    fun normalizeSubcategory(value: String): String = value.trim()
    fun normalizePackUnit(value: String): String = value.trim().lowercase()
    fun normalizeDescription(value: String): String = value.trim()

    fun isValidCategory(value: String): Boolean = normalizeCategory(value).length <= 100
    fun isValidSubcategory(value: String): Boolean = normalizeSubcategory(value).length <= 100
    fun isValidPackSize(value: Double?): Boolean = value == null || (value.isFinite() && value > 0.0)
    fun isValidPackUnit(value: String): Boolean = normalizePackUnit(value).length <= 32
    fun isValidDescription(value: String): Boolean = normalizeDescription(value).length <= 2000
    fun isValidTaxRatePercent(value: Double): Boolean = value.isFinite() && value in 0.0..100.0

    fun validate(
        category: String,
        subcategory: String,
        packSize: Double?,
        packUnit: String,
        description: String,
        taxRatePercent: Double = 0.0
    ): Boolean =
        isValidCategory(category) &&
            isValidSubcategory(subcategory) &&
            isValidPackSize(packSize) &&
            isValidPackUnit(packUnit) &&
            isValidDescription(description) &&
            isValidTaxRatePercent(taxRatePercent)
}
