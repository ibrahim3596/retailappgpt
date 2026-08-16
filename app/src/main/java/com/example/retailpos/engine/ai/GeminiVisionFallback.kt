package com.example.retailpos.engine.ai

import com.example.retailpos.engine.ocr.OcrProductResult
import org.json.JSONObject

data class AiVisionResult(
    val productName: String?,
    val brand: String?,
    val variant: String?,
    val packSize: String?,
    val mrp: Double?,
    val hsnCode: String?,
    val gstRate: Double?,
    val confidence: Double,
    val requiresShopkeeperReview: Boolean = true
)

object GeminiVisionFallback {

    fun parsePackagingWithAi(ocrResult: OcrProductResult): AiVisionResult {
        // Safe fallback logic preserving uncertainty and never fabricating values
        val mrp = ocrResult.mrp
        val gst = ocrResult.gstRate

        return AiVisionResult(
            productName = ocrResult.productName,
            brand = ocrResult.brand,
            variant = ocrResult.packSize,
            packSize = ocrResult.packSize,
            mrp = mrp,
            hsnCode = ocrResult.hsnCode,
            gstRate = gst,
            confidence = if (mrp != null) 0.85 else 0.50,
            requiresShopkeeperReview = true // ALWAYS require shopkeeper confirmation
        )
    }

    fun parseAiJsonResponse(jsonString: String): AiVisionResult {
        return try {
            val json = JSONObject(jsonString)
            AiVisionResult(
                productName = json.optString("productName", null),
                brand = json.optString("brand", null),
                variant = json.optString("variant", null),
                packSize = json.optString("packSize", null),
                mrp = if (json.has("mrp") && !json.isNull("mrp")) json.getDouble("mrp") else null,
                hsnCode = json.optString("hsnCode", null),
                gstRate = if (json.has("gstRate") && !json.isNull("gstRate")) json.getDouble("gstRate") else null,
                confidence = json.optDouble("confidence", 0.70),
                requiresShopkeeperReview = true
            )
        } catch (e: Exception) {
            AiVisionResult(
                productName = null,
                brand = null,
                variant = null,
                packSize = null,
                mrp = null,
                hsnCode = null,
                gstRate = null,
                confidence = 0.0,
                requiresShopkeeperReview = true
            )
        }
    }
}
