package com.retailpos.app.core.products

private val OCR_METADATA_PATTERN = Regex(
    "mrp|mfd|mfg|exp|expiry|best before|net wt|net weight|gross wt|qty|quantity|price|rs\\.?|inr|batch|lot|customer care|manufactured|marketed by|made in|use within|barcode|ingredients|nutrition|calories|protein|fat|carbohydrate|address|www\\.|@|fssai",
    RegexOption.IGNORE_CASE
)

private val OCR_NUMBER_ONLY_PATTERN = Regex("[0-9 .₹$€£,/:%#*+()\\-]+")
private val MRP_PATTERN = Regex(
    "(?:MRP|M\\.?R\\.?P\\.?)\\s*(?:₹|Rs\\.?|INR)?\\s*([0-9]+(?:[.,][0-9]{1,2})?)",
    RegexOption.IGNORE_CASE
)

data class ParsedProductText(
    val name: String?,
    val brand: String?,
    val mrp: Double?,
    val usefulLines: List<String>
)

object ProductCaptureParser {
    fun parse(rawText: String): ParsedProductText {
        val usefulLines = cleanLines(rawText)
        val name = usefulLines
            .sortedWith(
                compareByDescending<String> { line ->
                    val words = line.split(' ').count { it.length >= 2 }
                    val letters = line.count { it.isLetter() }
                    letters + words * 4
                }.thenBy { it.length > 60 }
            )
            .firstOrNull()
        val brand = usefulLines
            .asSequence()
            .filter { it != name }
            .filter { it.length in 2..40 }
            .filterNot { it.count(Char::isDigit) > it.count(Char::isLetter) }
            .sortedByDescending { it.count(Char::isLetter) }
            .firstOrNull()

        val mrp = MRP_PATTERN.find(rawText.replace('\n', ' '))
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()

        return ParsedProductText(
            name = name?.takeIf(String::isNotBlank),
            brand = brand?.takeIf { it.isNotBlank() && it != name },
            mrp = mrp,
            usefulLines = usefulLines
        )
    }

    private fun cleanLines(rawText: String): List<String> = rawText
        .lines()
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.length in 2..80 }
        .filterNot(OCR_NUMBER_ONLY_PATTERN::matches)
        .filterNot(OCR_METADATA_PATTERN::containsMatchIn)
        .distinctBy(String::lowercase)
}
