package com.retailpos.app.core.products

import com.retailpos.app.core.product.ProductMetadataRules

data class ProductMetadataFormState(
    val category: String = "",
    val subcategory: String = "",
    val packSize: String = "",
    val packUnit: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val taxRatePercent: String = "0"
) {
    fun normalized(): ProductMetadataFormState = copy(
        category = ProductMetadataRules.normalizeCategory(category),
        subcategory = ProductMetadataRules.normalizeSubcategory(subcategory),
        packSize = packSize.trim(),
        packUnit = ProductMetadataRules.normalizePackUnit(packUnit),
        description = ProductMetadataRules.normalizeDescription(description),
        taxRatePercent = taxRatePercent.trim().replace(',', '.')
    )

    fun packSizeValue(): Double? = packSize.toDoubleOrNull()
    fun taxRateValue(): Double = taxRatePercent.toDoubleOrNull() ?: Double.NaN

    fun validate(): String? {
        val packSize = packSizeValue()
        val taxRate = taxRateValue()
        if (!ProductMetadataRules.validate(category, subcategory, packSize, packUnit, description, taxRate)) {
            return when {
                !ProductMetadataRules.isValidPackSize(packSize) -> "Pack size must be greater than zero."
                !ProductMetadataRules.isValidTaxRatePercent(taxRate) -> "Tax rate must be between 0 and 100%."
                else -> "Product details are invalid."
            }
        }
        return null
    }
}
