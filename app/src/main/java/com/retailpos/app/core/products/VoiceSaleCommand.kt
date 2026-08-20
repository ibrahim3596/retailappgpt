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
    private val KG = Regex(
        "(?:kg|kgs|kilo|kilos|kilogram|kilograms|किलो|किलोग्राम|కిలో|కిలోలు|కిలోగ్రాము|കിലോ|കിലോഗ്രാം|किलो|किलोग्रॅम|கிலோ|கிலோகிராம்|ಕಿಲೋ|ಕಿಲೋಗ್ರಾಂ|কেজি|কিলো|কিলোগ্রাম|કિલો|કિલોગ્રામ|ਕਿਲੋ|ਕਿਲੋਗ੍ਰਾਮ|କିଲୋ|କିଲୋଗ୍ରାମ)",
        RegexOption.IGNORE_CASE
    )
    private val G = Regex(
        "(?:g|gm|gms|gram|grams|ग्र\\.?|ग्राम|గ్రా|గ్రాము|గ్రాములు|ഗ്രാം|গ্রাম|ग्रॅम|கிராம்|கிரா|ಗ್ರಾಂ|ಗ್ರಾಮ|ગ્રામ|ગ્?રામ|ਗ੍ਰਾਮ|ଗ୍ରାମ)",
        RegexOption.IGNORE_CASE
    )
    private val L = Regex(
        "(?:l|lt|ltr|litre|liter|litres|liters|लीटर|లీటర్|లీటర్లు|ലിറ്റർ|लिटर|लीटर|லிட்டர்|லிட்டர்கள்|ಲೀಟರ್|ಲೀಟರು|লিটার|লিটার|લિટર|લિટરો|ਲੀਟਰ|ਲੀਟਰਾਂ|ଲିଟର|ଲିଟର)",
        RegexOption.IGNORE_CASE
    )
    private val ML = Regex(
        "(?:ml|millilitre|milliliter|मिली|मिलीलिटर|మిల్లీ|మిల్లీలీటర్|മില്ലി|മില്ലിലിറ്റർ|मिली|मिलिलिटर|மில்லி|மில்லிலிட்டர்|ಮಿಲಿ|ಮಿಲಿಲೀಟರ್|মিলি|মিলিলিটার|મિલી|મિલિલિટર|ਮਿਲੀ|ਮਿਲੀਲੀਟਰ|ମିଲି|ମିଲିଲିଟର)",
        RegexOption.IGNORE_CASE
    )

    private val FRACTIONS = mapOf(
        "aadha" to 0.5, "adha" to 0.5, "half" to 0.5, "आधा" to 0.5,
        "pauna" to 0.75, "pouna" to 0.75, "पौना" to 0.75,
        "sawa" to 1.25, "sava" to 1.25, "सवा" to 1.25,
        "dedh" to 1.5, "डेढ़" to 1.5, "ढाई" to 2.5, "dhai" to 2.5,
        "do" to 2.0, "दो" to 2.0, "ek" to 1.0, "एक" to 1.0,
        "one" to 1.0, "two" to 2.0, "teen" to 3.0, "तीन" to 3.0,
        "three" to 3.0, "चार" to 4.0, "char" to 4.0,
        "five" to 5.0, "पांच" to 5.0,
        "సగం" to 0.5, "പകുതി" to 0.5, "अर्धा" to 0.5,
        "அரை" to 0.5, "ಅರ್ಧ" to 0.5, "অর্ধেক" to 0.5, "અડધું" to 0.5, "ਅੱਧਾ" to 0.5, "ଅଧା" to 0.5
    )

    private val ALIASES = mapOf(
        // Hindi / Hinglish
        "shakkar" to "sugar", "शक्कर" to "sugar", "cheeni" to "sugar", "चीनी" to "sugar",
        "chawal" to "rice", "चावल" to "rice", "atta" to "wheat flour", "आटा" to "wheat flour",
        "maida" to "refined flour", "मैदा" to "refined flour", "tel" to "oil", "तेल" to "oil",
        "namak" to "salt", "नमक" to "salt",
        // Telugu
        "చక్కెర" to "sugar", "పంచదార" to "sugar", "బియ్యం" to "rice", "బియ్యము" to "rice",
        "ఆటా" to "wheat flour", "మైదా" to "refined flour", "నూనె" to "oil", "ఉప్పు" to "salt",
        // Malayalam
        "പഞ്ചസാര" to "sugar", "അരി" to "rice", "ആട്ട" to "wheat flour", "മൈദ" to "refined flour",
        "എണ്ണ" to "oil", "ഉപ്പ്" to "salt",
        // Marathi
        "साखर" to "sugar", "तांदूळ" to "rice", "तांदुळ" to "rice", "पीठ" to "wheat flour",
        "मैदा" to "refined flour", "तेल" to "oil", "मीठ" to "salt",
        // Tamil
        "சர்க்கரை" to "sugar", "அரிசி" to "rice", "மாவு" to "wheat flour", "மைதா" to "refined flour",
        "எண்ணெய்" to "oil", "உப்பு" to "salt",
        // Kannada
        "ಸಕ್ಕರೆ" to "sugar", "ಅಕ್ಕಿ" to "rice", "ಗೋಧಿಹಿಟ್ಟು" to "wheat flour", "ಮೈದಾ" to "refined flour",
        "ಎಣ್ಣೆ" to "oil", "ಉಪ್ಪು" to "salt",
        // Bengali
        "চিনি" to "sugar", "চাল" to "rice", "আটা" to "wheat flour", "ময়দা" to "refined flour",
        "তেল" to "oil", "লবণ" to "salt",
        // Gujarati
        "ખાંડ" to "sugar", "ચોખા" to "rice", "ઘઉંનો લોટ" to "wheat flour", "મૈદો" to "refined flour",
        "તેલ" to "oil", "મીઠું" to "salt",
        // Punjabi
        "ਚੀਨੀ" to "sugar", "ਚਾਵਲ" to "rice", "ਆਟਾ" to "wheat flour", "ਮੈਦਾ" to "refined flour",
        "ਤੇਲ" to "oil", "ਨਮਕ" to "salt",
        // Odia
        "ଚିନି" to "sugar", "ଚାଉଳ" to "rice", "ଆଟା" to "wheat flour", "ମଇଦା" to "refined flour",
        "ତେଲ" to "oil", "ଲୁଣ" to "salt"
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
        val quantity = fractionToken?.let(FRACTIONS::get)
            ?: NUMBER.find(text)?.value?.replace(',', '.')?.toDoubleOrNull()
        if (quantity == null || quantity <= 0.0) return null

        text = text
            .replace(NUMBER, " ")
            .replace(KG, " ")
            .replace(G, " ")
            .replace(L, " ")
            .replace(ML, " ")
            .let(::cleanFractionTokens)
            .replace(Regex("\\b(ka|ki|ke|of|mein|me|dena|do|de|please|plz|aur|and|plus)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return text.takeIf { it.isNotBlank() }?.let { query ->
            VoiceSaleCommand(ALIASES[query] ?: query, quantity, explicitUnit)
        }
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
        "kg", "kgs", "kilo", "kilogram", "kilograms", "किलो", "किलोग्राम", "కిలో", "కిలోలు", "కిలోగ్రాము", "കിലോ", "കിലോഗ്രാം", "किलो", "किलोग्रॅम", "கிலோ", "கிலோகிராம்", "ಕಿಲೋ", "ಕಿಲೋಗ್ರಾಂ", "কেজি", "কিলো", "কিলোগ্রাম", "કિલો", "કિલોગ્રામ", "ਕਿਲੋ", "ਕਿਲੋਗ੍ਰਾਮ", "କିଲୋ", "କିଲୋଗ୍ରାମ" -> WeightUnit.KG
        "g", "gm", "gms", "gram", "grams", "ग्राम", "ग्राम", "గ్రా", "గ్రాము", "గ్రాములు", "ഗ്രാം", "ग्रॅम", "கிராம்", "கிரா", "ಗ್ರಾಂ", "ಗ್ರಾಮ", "ગ્રામ", "ਗ੍ਰਾਮ", "ଅ୍ରାମ", "ଗ୍ରାମ" -> WeightUnit.G
        "l", "lt", "ltr", "litre", "liter", "litres", "liters", "लीटर", "లీటర్", "లీటర్లు", "ലിറ്റർ", "लिटर", "लिटर", "லிட்டர்", "லிட்டர்கள்", "ಲೀಟರ್", "ಲೀಟರು", "লিটার", "લિટર", "લિટરો", "ਲੀਟਰ", "ਲੀਟਰਾਂ", "ଲିଟର" -> WeightUnit.L
        "ml", "millilitre", "milliliter", "मिली", "मिलीलिटर", "మిల్లీ", "మిల్లీలీటర్", "മില്ലി", "മില്ലിലിറ്റർ", "मिली", "மில்லி", "மில்லிலிட்டர்", "ಮಿಲಿ", "ಮಿಲಿಲೀಟರ್", "মিলি", "মিলিলিটার", "મિલી", "મિલિલિટર", "ਮਿਲੀ", "ਮਿਲੀਲੀਟਰ", "ମିଲି", "ମିଲିଲିଟର" -> WeightUnit.ML
        "pcs", "pc", "piece", "pieces", "item", "items", "नग", "నగ", "കഷണം", "नग", "நக", "ನಗ", "পিস", "નંગ", "ਨਗ", "ନଗ" -> WeightUnit.PIECE
        else -> null
    }
}
