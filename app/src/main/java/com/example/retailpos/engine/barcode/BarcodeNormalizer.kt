package com.example.retailpos.engine.barcode

data class NormalizedBarcodeResult(
    val rawInput: String,
    val sanitizedInput: String,
    val symbology: String,
    val canonicalGtin: String,
    val isValidChecksum: Boolean
)

object BarcodeNormalizer {

    fun normalize(rawInput: String): NormalizedBarcodeResult {
        val sanitized = rawInput.trim().replace(Regex("[^0-9]"), "")
        if (sanitized.isEmpty()) {
            return NormalizedBarcodeResult(rawInput, "", "UNKNOWN", "", false)
        }

        var symbology = "CUSTOM"
        var gtin = sanitized
        var isValid = false

        when (sanitized.length) {
            6 -> {
                // UPC-E without number system and check digit
                val expandedUpcA = expandUpcE("0$sanitized" + calculateUpcCheckDigit("0$sanitized"))
                gtin = padToGtin13(expandedUpcA)
                symbology = "UPC-E"
                isValid = true
            }
            8 -> {
                // EAN-8 or UPC-E with lead and check
                if (sanitized.startsWith("0") || sanitized.startsWith("1")) {
                    val expandedUpcA = expandUpcE(sanitized)
                    gtin = padToGtin13(expandedUpcA)
                    symbology = "UPC-E"
                    isValid = validateModulo10(expandedUpcA)
                } else {
                    gtin = padToGtin13(sanitized)
                    symbology = "EAN-8"
                    isValid = validateModulo10(sanitized)
                }
            }
            12 -> {
                // UPC-A
                gtin = "0$sanitized"
                symbology = "UPC-A"
                isValid = validateModulo10(sanitized)
            }
            13 -> {
                // EAN-13
                gtin = sanitized
                symbology = "EAN-13"
                isValid = validateModulo10(sanitized)
            }
            14 -> {
                // ITF-14
                gtin = sanitized
                symbology = "ITF-14"
                isValid = validateModulo10(sanitized)
            }
            else -> {
                gtin = padToGtin13(sanitized)
                symbology = "CODE-128 / OTHER"
                isValid = true
            }
        }

        return NormalizedBarcodeResult(
            rawInput = rawInput,
            sanitizedInput = sanitized,
            symbology = symbology,
            canonicalGtin = gtin,
            isValidChecksum = isValid
        )
    }

    private fun padToGtin13(code: String): String {
        return code.padStart(13, '0')
    }

    /**
     * UPC-E Zero Suppression expansion algorithm.
     * Takes 8-digit UPC-E string: N d1 d2 d3 d4 d5 C
     */
    fun expandUpcE(upcE: String): String {
        val clean = upcE.padStart(8, '0')
        val numSystem = clean[0]
        val d1 = clean[1]
        val d2 = clean[2]
        val d3 = clean[3]
        val d4 = clean[4]
        val d5 = clean[5]
        val d6 = clean[6]
        val checkDigit = clean[7]

        val mfr: String
        val prod: String

        when (d6) {
            '0', '1', '2' -> {
                mfr = "$d1$d2$d6" + "00"
                prod = "00" + "$d3$d4$d5"
            }
            '3' -> {
                mfr = "$d1$d2$d3" + "00"
                prod = "000" + "$d4$d5"
            }
            '4' -> {
                mfr = "$d1$d2$d3$d4" + "0"
                prod = "0000" + "$d5"
            }
            else -> {
                mfr = "$d1$d2$d3$d4$d5"
                prod = "0000" + "$d6"
            }
        }

        val upcAWithoutCheck = "$numSystem$mfr$prod"
        val calculatedCheck = calculateUpcCheckDigit(upcAWithoutCheck)
        return "$upcAWithoutCheck$calculatedCheck"
    }

    fun validateModulo10(code: String): Boolean {
        if (code.length < 2) return false
        val digits = code.mapNotNull { it.digitToIntOrNull() }
        if (digits.size != code.length) return false

        val payload = digits.dropLast(1)
        val expectedCheck = digits.last()

        var sum = 0
        val isOddLength = payload.size % 2 != 0

        for (i in payload.indices) {
            val digit = payload[i]
            val weight = if ((i % 2 == 0) == isOddLength) 3 else 1
            sum += digit * weight
        }

        val check = (10 - (sum % 10)) % 10
        return check == expectedCheck
    }

    private fun calculateUpcCheckDigit(upcAWithoutCheck: String): String {
        val digits = upcAWithoutCheck.mapNotNull { it.digitToIntOrNull() }
        var sum = 0
        for (i in digits.indices) {
            val weight = if (i % 2 == 0) 3 else 1
            sum += digits[i] * weight
        }
        val check = (10 - (sum % 10)) % 10
        return check.toString()
    }
}
