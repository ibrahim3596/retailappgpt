package com.example.retailpos.engine.ocr

data class OcrProductResult(
    val productName: String? = null,
    val brand: String? = null,
    val packSize: String? = null,
    val mrp: Double? = null,
    val mfd: String? = null,
    val exp: String? = null,
    val hsnCode: String? = null,
    val gstRate: Double? = null,
    val barcode: String? = null,
    val rawText: String,
    val mrpConflictDetected: Boolean = false,
    val confidence: Double = 0.85
)

object PackagingOcrParser {

    private val MRP_REGEX = Regex(
        "(?:m\\.?r\\.?p\\.?|max(?:imum)?\\s*retail\\s*price|rs\\.?|inr|₹)\\s*[:=-]?\\s*([0-9]+(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    private val PACK_SIZE_REGEX = Regex(
        "(?:net\\s*(?:wt\\.?|weight|qty\\.?|quantity)\\s*[:=-]?\\s*)?([0-9]+\\s*(?:g|kg|ml|l|ltr|gm|grams|kgm|pcs|pack\\s*of\\s*[0-9]+))",
        RegexOption.IGNORE_CASE
    )

    private val EXP_REGEX = Regex(
        "(?:exp(?:iry)?\\s*(?:date)?|use\\s*by|best\\s*before)\\s*[:=-]?\\s*([0-9]{2}[/-][0-9]{2,4}|[0-9]{2}\\s+[a-zA-Z]{3}\\s+[0-9]{2,4})",
        RegexOption.IGNORE_CASE
    )

    private val MFD_REGEX = Regex(
        "(?:mfd|mfg|manufactured)\\s*[:=-]?\\s*([0-9]{2}[/-][0-9]{2,4}|[0-9]{2}\\s+[a-zA-Z]{3}\\s+[0-9]{2,4})",
        RegexOption.IGNORE_CASE
    )

    private val HSN_REGEX = Regex(
        "(?:hsn(?:\\s*code)?)\\s*[:=-]?\\s*([0-9]{4,8})",
        RegexOption.IGNORE_CASE
    )

    private val GST_REGEX = Regex(
        "(?:gst|tax)\\s*[:=-]?\\s*([0-9]{1,2})\\s*%",
        RegexOption.IGNORE_CASE
    )

    fun parsePackagingText(lines: List<String>): OcrProductResult {
        val fullText = lines.joinToString("\n")
        val foundMrps = mutableListOf<Double>()

        for (line in lines) {
            val mrpMatches = MRP_REGEX.findAll(line)
            for (match in mrpMatches) {
                val valueStr = match.groupValues[1]
                valueStr.toDoubleOrNull()?.let { foundMrps.add(it) }
            }
        }

        val primaryMrp = foundMrps.firstOrNull()
        val distinctMrps = foundMrps.distinct()
        val mrpConflict = distinctMrps.size > 1

        var packSize: String? = null
        for (line in lines) {
            val match = PACK_SIZE_REGEX.find(line)
            if (match != null) {
                packSize = match.groupValues[1].trim()
                break
            }
        }

        var expDate: String? = null
        var mfdDate: String? = null
        var hsn: String? = null
        var gst: Double? = null

        for (line in lines) {
            if (expDate == null) {
                EXP_REGEX.find(line)?.let { expDate = it.groupValues[1] }
            }
            if (mfdDate == null) {
                MFD_REGEX.find(line)?.let { mfdDate = it.groupValues[1] }
            }
            if (hsn == null) {
                HSN_REGEX.find(line)?.let { hsn = it.groupValues[1] }
            }
            if (gst == null) {
                GST_REGEX.find(line)?.let { gst = it.groupValues[1].toDoubleOrNull() }
            }
        }

        // Candidate brand and product name from top non-numeric text lines
        val validTextLines = lines.map { it.trim() }
            .filter { it.length > 2 && !it.matches(Regex("^[0-9\\W]+$")) }

        val brand = validTextLines.firstOrNull()
        val productName = validTextLines.drop(1).firstOrNull() ?: brand

        return OcrProductResult(
            productName = productName,
            brand = brand,
            packSize = packSize,
            mrp = primaryMrp,
            mfd = mfdDate,
            exp = expDate,
            hsnCode = hsn,
            gstRate = gst,
            rawText = fullText,
            mrpConflictDetected = mrpConflict,
            confidence = if (primaryMrp != null && productName != null) 0.90 else 0.65
        )
    }
}
