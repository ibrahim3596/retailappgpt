package com.retailpos.app.core.products

private val OCR_METADATA_PATTERN = Regex(
    "mrp|mfd|mfg|exp|expiry|best before|net wt|net weight|gross wt|qty|quantity|price|rs\\.?|inr|batch|lot|customer care|manufactured|marketed by|made in|use within|barcode|ingredients|nutrition|calories|protein|fat|carbohydrate|address|www\\.|@|fssai",
    RegexOption.IGNORE_CASE
)
private val OCR_NUMBER_ONLY_PATTERN = Regex("[0-9 .₹$€£,/:%#*+()\\-]+")
private val OCR_SYMBOL_PATTERN = Regex("[^\\p{L}\\p{N}\\s&+.-]")
private val MRP_PATTERNS = listOf(
    Regex("(?:MRP|M\\.?R\\.?P\\.?)\\s*[:\\-]?\\s*(?:₹|Rs\\.?|INR)?\\s*([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE),
    Regex("(?:₹|Rs\\.?|INR)\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:MRP|M\\.?R\\.?P\\.?)", RegexOption.IGNORE_CASE)
)
private val BRAND_PATTERN = Regex("^brand\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
private val PRODUCT_PATTERN = Regex("^(?:product|product name|name)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)

data class ParsedProductText(
    val name: String?,
    val brand: String?,
    val mrp: Double?,
    val usefulLines: List<String>
)

object ProductCaptureParser {
    fun normalizeForMatching(value: String): String = value
        .trim()
        .lowercase()
        .replace('&', ' ')
        .replace(OCR_SYMBOL_PATTERN, " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun parse(rawText: String): ParsedProductText {
        // Extract explicit labels from the original OCR lines before punctuation cleanup.
        // Otherwise `Brand: ...` becomes `Brand ...` and the label parser loses precedence.
        val rawLines = rawText.lines().map { it.replace(Regex("\\s+"), " ").trim() }
        val explicitName = rawLines.firstNotNullOfOrNull {
            PRODUCT_PATTERN.matchEntire(it)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
        }
        val explicitBrand = rawLines.firstNotNullOfOrNull {
            BRAND_PATTERN.matchEntire(it)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
        }
        val usefulLines = cleanLines(rawText)
        val name = explicitName ?: usefulLines
            .filterNot { BRAND_PATTERN.matches(it) || PRODUCT_PATTERN.matches(it) }
            .sortedWith(
                compareByDescending<String> { line ->
                    val words = line.split(' ').count { it.length >= 2 }
                    val letters = line.count { it.isLetter() }
                    letters + words * 4
                }.thenBy { it.length > 60 }
            )
            .firstOrNull()
        val brand = explicitBrand ?: usefulLines
            .asSequence()
            .filter { it != name }
            .filter { it.length in 2..40 }
            .filterNot { it.count(Char::isDigit) > it.count(Char::isLetter) }
            .sortedByDescending { it.count(Char::isLetter) }
            .firstOrNull()

        val flattened = rawText.replace('\n', ' ')
        val mrp = MRP_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.find(flattened)
                ?.groupValues
                ?.getOrNull(1)
                ?.replace(',', '.')
                ?.toDoubleOrNull()
        }

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
        .filterNot { line ->
            OCR_METADATA_PATTERN.containsMatchIn(line) &&
                !BRAND_PATTERN.matches(line) &&
                !PRODUCT_PATTERN.matches(line)
        }
        .map { it.replace(OCR_SYMBOL_PATTERN, " ").replace(Regex("\\s+"), " ").trim() }
        .filter { it.length >= 2 }
        .distinctBy(::normalizeForMatching)
}
