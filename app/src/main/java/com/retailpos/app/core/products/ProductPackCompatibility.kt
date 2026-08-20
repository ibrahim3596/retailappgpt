package com.retailpos.app.core.products

/** Classifies captured package quantity without changing the configured selling unit. */
enum class PackCompatibility {
    CONVERTIBLE_MEASURE,
    DESCRIPTIVE_PACKAGE,
    MISMATCH_REQUIRES_REVIEW
}

data class PackCompatibilityResult(
    val compatibility: PackCompatibility,
    val normalizedPackUnit: String,
    val normalizedSellingUnit: String,
    val explanation: String
)

object ProductPackCompatibility {
    fun classify(pack: ParsedPack, sellingUnit: String): PackCompatibilityResult {
        val packUnit = normalize(pack.unit)
        val sellUnit = normalize(sellingUnit)

        val convertible = when {
            packUnit == sellUnit -> true
            packUnit in setOf("mg", "g", "kg") && sellUnit in setOf("mg", "g", "kg") -> true
            packUnit in setOf("ml", "l") && sellUnit in setOf("ml", "l") -> true
            else -> false
        }

        if (convertible) {
            return PackCompatibilityResult(
                PackCompatibility.CONVERTIBLE_MEASURE,
                packUnit,
                sellUnit,
                "Observed package quantity can be converted to the configured selling unit."
            )
        }

        if (sellUnit == "pcs" && packUnit in setOf("mg", "g", "kg", "ml", "l", "pack", "packet", "bottle", "box", "jar", "tin", "pouch", "sachet")) {
            return PackCompatibilityResult(
                PackCompatibility.DESCRIPTIVE_PACKAGE,
                packUnit,
                sellUnit,
                "Package quantity is descriptive; the product remains sold by pieces."
            )
        }

        return PackCompatibilityResult(
            PackCompatibility.MISMATCH_REQUIRES_REVIEW,
            packUnit,
            sellUnit,
            "Observed package unit does not match the configured selling unit. Review before saving."
        )
    }

    private fun normalize(unit: String): String = when (unit.trim().lowercase()) {
        "kgs", "kilogram", "kilograms" -> "kg"
        "grams", "gram", "gms", "gm" -> "g"
        "milligram", "milligrams" -> "mg"
        "l", "ltr", "litre", "liter", "litres", "liters" -> "l"
        "millilitre", "millilitres", "milliliter", "milliliters" -> "ml"
        "pc", "pcs", "piece", "pieces" -> "pcs"
        "packs" -> "pack"
        "packets" -> "packet"
        "bottles" -> "bottle"
        "boxes" -> "box"
        "jars" -> "jar"
        "tins" -> "tin"
        "pouches" -> "pouch"
        "sachets" -> "sachet"
        else -> unit.trim().lowercase()
    }
}
