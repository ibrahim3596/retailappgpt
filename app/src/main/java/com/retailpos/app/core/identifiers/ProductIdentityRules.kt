package com.retailpos.app.core.identifiers

/** Pure product-identity rules shared by product creation, scanning and search. */
object ProductIdentityRules {
    fun normalizeSku(value: String): String = value.trim().uppercase()

    fun normalizeBarcode(value: String): String = ProductIdentifierValidator.normalize(value)

    fun isValidSku(value: String): Boolean {
        val sku = normalizeSku(value)
        return sku.isNotBlank() && sku.length <= 64 && sku.none { it.isWhitespace() }
    }

    fun isValidProductName(value: String): Boolean =
        value.trim().length in 1..200

    fun identifiersConflict(first: String, second: String): Boolean =
        normalizeBarcode(first).isNotBlank() &&
            normalizeBarcode(first) == normalizeBarcode(second)
}
