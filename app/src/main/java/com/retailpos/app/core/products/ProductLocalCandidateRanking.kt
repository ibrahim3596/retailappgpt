package com.retailpos.app.core.products

import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.ProductMetadataEntity

data class ProductLocalCandidate(
    val product: ProductEntity,
    val score: Int,
    val explanation: String
)

object ProductLocalCandidateRanking {
    fun rank(
        queryName: String?,
        queryBrand: String?,
        candidates: List<ProductEntity>,
        observedPackSize: Double? = null,
        observedPackUnit: String? = null,
        metadataByProductId: Map<String, ProductMetadataEntity> = emptyMap(),
        feedbackBoostByProductId: Map<String, Int> = emptyMap()
    ): List<ProductLocalCandidate> {
        val name = ProductCaptureParser.normalizeForMatching(queryName.orEmpty())
        val brand = ProductCaptureParser.normalizeForMatching(queryBrand.orEmpty())
        if (name.isBlank() && brand.isBlank()) return emptyList()

        return candidates.mapNotNull { product ->
            val productName = ProductCaptureParser.normalizeForMatching(product.name)
            val productBrand = ProductCaptureParser.normalizeForMatching(product.brand)
            val nameMatch = similarity(name, productName)
            val brandMatch = similarity(brand, productBrand)
            val exactName = name.isNotBlank() && name == productName
            val exactBrand = brand.isNotBlank() && brand == productBrand
            var score = (nameMatch * 70 + brandMatch * 25).toInt() + if (exactName) 5 else 0
            val explanation = buildString {
                append("Local catalog")
                if (exactName) append(" exact name") else if (nameMatch >= 0.8) append(" strong name match")
                if (exactBrand) append(" exact brand") else if (brandMatch >= 0.8) append(" strong brand match")

                val metadata = metadataByProductId[product.id]
                if (observedPackSize != null && !observedPackUnit.isNullOrBlank() && metadata != null && metadata.packSize != null) {
                    val sameUnit = metadata.packUnit.equals(observedPackUnit, ignoreCase = true)
                    val closeSize = kotlin.math.abs(metadata.packSize - observedPackSize) <= 0.01
                    when {
                        sameUnit && closeSize -> { score += 4; append(" exact pack variant") }
                        sameUnit -> { score -= 2; append(" different pack size") }
                        else -> { score -= 4; append(" different pack unit") }
                    }
                }

                val feedbackBoost = feedbackBoostByProductId[product.id]?.coerceIn(-8, 8) ?: 0
                if (feedbackBoost != 0) {
                    score += feedbackBoost
                    append(if (feedbackBoost > 0) " trusted by prior review" else " corrected by prior review")
                }
                append(". Review before using this product.")
            }

            if (score < 45) null else ProductLocalCandidate(product, score.coerceIn(0, 99), explanation.toString())
        }.sortedByDescending { it.score }
    }

    private fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0
        if (a.contains(b) || b.contains(a)) return 0.9
        val aTokens = a.split(' ').filter(String::isNotBlank).toSet()
        val bTokens = b.split(' ').filter(String::isNotBlank).toSet()
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0
        return aTokens.intersect(bTokens).size.toDouble() / maxOf(aTokens.size, bTokens.size)
    }
}
