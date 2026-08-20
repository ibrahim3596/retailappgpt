package com.retailpos.app.core.products

/** Parsed natural-language retail request, e.g. "aadha kilo shakkar". */
data class VoiceSaleCommand(
    val productQuery: String,
    val quantity: Double,
    val unit: WeightUnit
)

enum class WeightUnit {
    PIECE,
    KG,
    G,
    L,
    ML
}

object VoiceSaleCommandParser {
    private val NUMBER = Regex("(?:\\d+(?:[.,]\\d+)?)")
    private val KG = Regex("(?:kg|kgs|kilo|kilos|kilogram|kilograms|किलो|किलोग्राम)", RegexOption.IGNORE_CASE)
    private val G = Regex("(?:g|gm|gms|gram|grams|ग्र\.?|ग्राम)", RegexOption.IGNORE_CASE)
    private val L = Regex("(?:l|lt|ltr|litre|liter|litres|liters|लीटर|लीटर)", RegexOption.IGNORE_CASE)
    private val ML = Regex("(?:ml|millilitre|milliliter|मिली|मिलीलिटर)", RegexOption.IGNORE_CASE)

    private val FRACTIONS = mapOf(
        "aadha" to 0.5, "adha" to 0.5, "half" to 0.5, "आधा" to 0.5,
        "pauna" to 0.75, "pouna" to 0.75, "पौना" to 0.75,
        "sawa" to 1.25, "sava" to 1.25, "सवा" to 1.25,
        "dedh" to 1.5, "ढाई" to 2.5, "dhai" to 2.5,
        "do" to 2.0, "ek" to 1.0, "one" to 1.0, "two" to 2.0,
        "teen" to 3.0, "three" to 3.0, "चार" to 4.0, "char" to 4.0
    )

    fun parse(spoken: String): VoiceSaleCommand? {
        var text = spoken.trim().lowercase()
        if (text.isBlank()) return null

        val explicitUnit = when {
            ML.containsMatchIn(text) -> WeightUnit.ML
            KG.containsMatchIn(text) -> WeightUnit.KG
            G.containsMatchIn(text) -> WeightUnit.G
            L.containsMatchIn(text) -> WeightUnit.L
            else -> WeightUnit.PIECE
        }

        val fractionToken = FRACTIONS.keys.firstOrNull { token -> Regex("(^|\\s)$token(?=\\s|$)").containsMatchIn(text) }
        var quantity = fractionToken?.let(FRACTIONS::get)
        if (quantity == null) {
            val number = NUMBER.find(text)?.value?.replace(',', '.')?.toDoubleOrNull()
            quantity = number
        }
        if (quantity == null) return null

        // Phrases like "aadha kilo sugar" are already expressed in the requested unit.
        // Numeric grams/liters remain in their native unit and are normalized by the caller.
        text = text
            .replace(NUMBER, " ")
            .replace(KG, " ")
            .replace(G, " ")
            .replace(L, " ")
            .replace(ML, " ")
            .let { cleanFractionTokens(it) }
            .replace(Regex("\\b(ka|ki|ke|of|mein|me|dena|do|de|please|plz)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // A fraction token can be a quantity word and not part of the product name.
        if (fractionToken != null) {
            text = Regex("(^|\\s)${Regex.escape(fractionToken)}(?=\\s|$)", RegexOption.IGNORE_CASE)
                .replace(text, " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        return text.takeIf { it.isNotBlank() }?.let { VoiceSaleCommand(it, quantity, explicitUnit) }
    }

    private fun cleanFractionTokens(text: String): String = FRACTIONS.keys.fold(text) { acc, token ->
        acc.replace(Regex("(^|\\s)${Regex.escape(token)}(?=\\s|$)"), " ")
    }

    fun toBaseQuantity(quantity: Double, unit: WeightUnit, productUnit: String): Double? {
        val target = normalizeUnit(productUnit) ?: return null
        return when (unit to target) {
            WeightUnit.KG to WeightUnit.KG -> quantity
            WeightUnit.G to WeightUnit.KG -> quantity / 1000.0
            WeightUnit.KG to WeightUnit.G -> quantity * 1000.0
            WeightUnit.G to WeightUnit.G -> quantity
            WeightUnit.L to WeightUnit.L -> quantity
            WeightUnit.ML to WeightUnit.L -> quantity / 1000.0
            WeightUnit.L to WeightUnit.ML -> quantity * 1000.0
            WeightUnit.ML to WeightUnit.ML -> quantity
            WeightUnit.PIECE to WeightUnit.PIECE -> quantity
            else -> null
        }
    }

    fun normalizeUnit(unit: String): WeightUnit? = when (unit.trim().lowercase()) {
        "kg", "kgs", "kilo", "kilogram", "kilograms", "किलो", "किलोग्राम" -> WeightUnit.KG
        "g", "gm", "gms", "gram", "grams", "ग्राम" -> WeightUnit.G
        "l", "lt", "ltr", "litre", "liter", "litres", "liters", "लीटर" -> WeightUnit.L
        "ml", "millilitre", "milliliter", "मिली", "मिलीलिटर" -> WeightUnit.ML
        "pcs", "pc", "piece", "pieces", "item", "items", "नग" -> WeightUnit.PIECE
        else -> null
    }
}
