package com.retailpos.app.core.products

private val PACK_PATTERN = Regex(
    "(?<![A-Za-z0-9])([0-9]+(?:[.,][0-9]+)?)\\s*(kg|kgs|kilogram(?:s)?|g|gm|gms|gram(?:s)?|mg|milligram(?:s)?|l|ltr|litre|liter|litres|liters|ml|millilitre(?:s)?|milliliter(?:s)?|pcs?|pieces?|piece|pack(?:s)?|packet(?:s)?|bottle(?:s)?|box(?:es)?|jar(?:s)?|tin(?:s)?|pouch(?:es)?|sachet(?:s)?)(?![A-Za-z])",
    RegexOption.IGNORE_CASE
)

data class ParsedPack(
    val size: Double,
    val unit: String,
    val sourceText: String
)

object ProductPackParser {
    fun parse(rawText: String): ParsedPack? {
        val matches = PACK_PATTERN.findAll(rawText.replace(',', '.')).toList()
        if (matches.isEmpty()) return null
        val match = matches.first()
        val size = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = normalizeUnit(match.groupValues[2])
        if (!size.isFinite() || size <= 0.0 || unit == null) return null
        return ParsedPack(size = size, unit = unit, sourceText = match.value.trim())
    }

    private fun normalizeUnit(value: String): String? = when (value.lowercase()) {
        "kg", "kgs", "kilogram", "kilograms" -> "kg"
        "g", "gm", "gms", "gram", "grams" -> "g"
        "mg", "milligram", "milligrams" -> "mg"
        "l", "ltr", "litre", "liter", "litres", "liters" -> "L"
        "ml", "millilitre", "millilitres", "milliliter", "milliliters" -> "ml"
        "pc", "pcs", "piece", "pieces" -> "pcs"
        "pack", "packs" -> "pack"
        "packet", "packets" -> "packet"
        "bottle", "bottles" -> "bottle"
        "box", "boxes" -> "box"
        "jar", "jars" -> "jar"
        "tin", "tins" -> "tin"
        "pouch", "pouches" -> "pouch"
        "sachet", "sachets" -> "sachet"
        else -> null
    }
}
