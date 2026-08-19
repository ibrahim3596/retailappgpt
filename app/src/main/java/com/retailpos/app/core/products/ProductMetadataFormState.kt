package com.retailpos.app.core.products

import com.retailpos.app.core.product.ProductMetadataRules

data class ProductMetadataFormState(
    val category: String = "",
    val subcategory: String = "",
    val packSize: String = "",
    val packUnit: String = "",
    val description: String = "",
    val imageUri: String? = null
) {
    fun normalized(): ProductMetadataFormState = copy(
        category = ProductMetadataRules.normalizeCategory(category),
        subcategory = ProductMetadataRules.normalizeSubcategory(subcategory),
        packSize = packSize.trim(),
        packUnit = ProductMetadataRules.normalizePackUnit(packUnit),
        description = ProductMetadataRules.normalizeDescription(description)
    )

    fun packSizeValue(): Double? = packSize.toDoubleOrNull()

    fun validate(): String? = ProductMetadataRules.validate(
        category = category,
        subcategory = subcategory,
        packSize = packSizeValue(),
        packUnit = packUnit,
        description = description
    )
}
