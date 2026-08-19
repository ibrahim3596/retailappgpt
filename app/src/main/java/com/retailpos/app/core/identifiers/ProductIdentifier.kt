package com.retailpos.app.core.identifiers

/**
 * An externally scannable identifier attached to a product.
 * A retailer SKU is intentionally separate because it is store-controlled.
 */
data class ProductIdentifier(
    val value: String,
    val type: BarcodeType,
    val isPrimary: Boolean = false
)

object ProductIdentifierValidator {
    fun normalize(value: String): String = value.trim().uppercase()

    fun isValidGtIn(value: String): Boolean {
        val digits = normalize(value)
        if (digits.length !in setOf(8, 12, 13, 14) || digits.any { !it.isDigit() }) return false
        val check = digits.last().digitToInt()
        var sum = 0
        var multiplier = 3
        for (i in digits.length - 2 downTo 0) {
            sum += digits[i].digitToInt() * multiplier
            multiplier = if (multiplier == 3) 1 else 3
        }
        return (10 - (sum % 10)) % 10 == check
    }

    /**
     * Numeric retail identifiers with GTIN-sized lengths must pass the
     * GTIN check digit. Other barcode formats may contain non-GTIN values.
     */
    fun isValidRetailBarcode(value: String): Boolean {
        val normalized = normalize(value)
        if (normalized.isBlank()) return true
        val numeric = normalized.all { it.isDigit() }
        val looksLikeGtIn = numeric && normalized.length in setOf(8, 12, 13, 14)
        return !looksLikeGtIn || isValidGtIn(normalized)
    }

    fun isProductIdentifierType(type: BarcodeType): Boolean = type != BarcodeType.QR_CODE

    fun isRetailPosGtInType(type: BarcodeType): Boolean = when (type) {
        BarcodeType.EAN_8,
        BarcodeType.EAN_13,
        BarcodeType.UPC_A,
        BarcodeType.UPC_E -> true
        else -> false
    }
}
