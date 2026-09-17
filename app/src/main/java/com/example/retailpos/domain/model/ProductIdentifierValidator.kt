package com.example.retailpos.domain.model

/**
 * Barcode symbologies relevant to the POS. Kept as a local enum so domain rules
 * do not depend on ML Kit types.
 */
enum class BarcodeType {
    EAN_13,
    EAN_8,
    UPC_A,
    UPC_E,
    ITF_14,
    CODE_128,
    CODE_39,
    QR_CODE
}

/**
 * Validation and normalization for product identifiers (barcodes / SKUs).
 *
 * GTIN-shaped numeric barcodes (EAN-13, UPC-A, ITF-14, ...) carry a mandatory
 * check digit; a malformed check digit almost always means a scan or entry
 * error, so such identifiers are rejected before they can pollute the catalog.
 * Custom alphanumeric SKU formats have no checksum and remain allowed.
 */
object ProductIdentifierValidator {

    private val GTIN_LENGTHS = setOf(8, 12, 13, 14)

    /** Uppercases and strips surrounding whitespace and separator noise. */
    fun normalize(raw: String): String =
        raw.trim().replace(" ", "").replace("-", "").uppercase()

    /**
     * True when the value is a syntactically valid GTIN of a supported length
     * (8/12/13/14 digits) with a correct GS1 check digit.
     */
    fun isValidGtIn(value: String): Boolean {
        if (value.length !in GTIN_LENGTHS || value.any { !it.isDigit() }) return false
        return checkDigitMatches(value)
    }

    /** Symbologies a retail POS will realistically scan for products. */
    fun isRetailPosGtInType(type: BarcodeType): Boolean = when (type) {
        BarcodeType.EAN_13,
        BarcodeType.EAN_8,
        BarcodeType.UPC_A,
        BarcodeType.UPC_E -> true
        BarcodeType.ITF_14,
        BarcodeType.CODE_128,
        BarcodeType.CODE_39,
        BarcodeType.QR_CODE -> false
    }

    /**
     * Accepts a barcode for product identification: custom non-GTIN formats are
     * always allowed, but anything that is GTIN-shaped (all digits, GTIN length)
     * must have a valid check digit.
     */
    fun isValidRetailBarcode(raw: String): Boolean {
        val value = normalize(raw)
        if (value.isEmpty()) return false
        val isGtInShaped = value.length in GTIN_LENGTHS && value.all { it.isDigit() }
        return if (isGtInShaped) isValidGtIn(value) else true
    }

    private fun checkDigitMatches(value: String): Boolean {
        // GS1 check digit: weights 3,1,3,1... applied right-to-left, excluding
        // the check digit itself.
        val digits = value.map { it - '0' }
        val payload = digits.dropLast(1)
        val check = digits.last()
        var sum = 0
        payload.reversed().forEachIndexed { index, digit ->
            sum += digit * if (index % 2 == 0) 3 else 1
        }
        return (10 - (sum % 10)) % 10 == check
    }
}
