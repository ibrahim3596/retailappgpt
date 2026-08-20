package com.retailpos.app.core.products

object ProductMetadataRules {
    fun normalizeCategory(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(100)
    fun normalizeSubcategory(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(100)
    fun normalizePackUnit(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(32)
    fun normalizeDescription(value: String): String = value.trim().take(1000)

    fun isValidPackSize(value: Double?): Boolean =
        value == null || (value.isFinite() && value > 0.0)

    fun isValidTaxRatePercent(value: Double): Boolean =
        value.isFinite() && value in 0.0..100.0
}
