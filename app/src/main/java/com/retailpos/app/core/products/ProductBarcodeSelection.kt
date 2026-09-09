package com.retailpos.app.core.products

/**
 * Chooses the most likely product barcode when the camera sees more than one.
 * The score favors larger, central candidates while requiring the caller to
 * provide only values already accepted as retail product identifiers.
 */
data class ProductBarcodeCandidate(
    val rawValue: String,
    val format: Int,
    val centerX: Float,
    val centerY: Float,
    val areaRatio: Float
)

object ProductBarcodeSelection {
    fun choose(
        candidates: List<ProductBarcodeCandidate>,
        frameWidth: Int,
        frameHeight: Int
    ): ProductBarcodeCandidate? {
        if (candidates.isEmpty() || frameWidth <= 0 || frameHeight <= 0) return null

        val frameCenterX = frameWidth / 2f
        val frameCenterY = frameHeight / 2f
        val maxDistance = kotlin.math.hypot(
            frameCenterX.toDouble(),
            frameCenterY.toDouble()
        ).toFloat().coerceAtLeast(1f)

        return candidates.maxByOrNull { candidate ->
            val distance = kotlin.math.hypot(
                (candidate.centerX - frameCenterX).toDouble(),
                (candidate.centerY - frameCenterY).toDouble()
            ).toFloat()
            val centrality = 1f - (distance / maxDistance).coerceIn(0f, 1f)
            val area = candidate.areaRatio.coerceIn(0f, 1f)
            centrality * 0.65f + area * 0.35f
        }
    }
}
