package com.retailpos.app.core.products

/**
 * Collapses multiple camera observations into one conservative consensus record.
 * A field is retained only when there is agreement or a single strong observation;
 * the aggregator never invents a missing value.
 */
object ProductCaptureConsensus {
    fun merge(observations: List<ProductCaptureObservation>): ProductCaptureObservation? {
        if (observations.isEmpty()) return null
        val barcode = mostFrequentNonBlank(observations.mapNotNull { it.barcode })
        val name = mostFrequentNonBlank(observations.mapNotNull { it.printedName })
        val brand = mostFrequentNonBlank(observations.mapNotNull { it.printedBrand })
        val category = mostFrequentNonBlank(observations.mapNotNull { it.categoryHint })
        val mrp = mostFrequentDouble(observations.mapNotNull { it.mrp }, tolerance = 0.01)
        val pack = mostFrequentPack(observations.mapNotNull { it.pack })

        return ProductCaptureObservation(
            barcode = barcode,
            printedName = name,
            printedBrand = brand,
            mrp = mrp,
            categoryHint = category,
            categoryConfidence = observations.mapNotNull { it.categoryConfidence }.maxOrNull(),
            pack = pack,
            frameCount = observations.size
        )
    }

    private fun mostFrequentNonBlank(values: List<String>): String? = values
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .groupingBy { it.lowercase() }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?.let { key -> values.firstOrNull { it.trim().equals(key, ignoreCase = true) }?.trim() }

    private fun mostFrequentDouble(values: List<Double>, tolerance: Double): Double? {
        if (values.isEmpty()) return null
        val groups = mutableListOf<MutableList<Double>>()
        values.forEach { value ->
            val group = groups.firstOrNull { kotlin.math.abs(it.first() - value) <= tolerance }
            if (group == null) groups += mutableListOf(value) else group += value
        }
        return groups.maxByOrNull { it.size }?.average()
    }

    private fun mostFrequentPack(values: List<ParsedPack>): ParsedPack? {
        if (values.isEmpty()) return null
        val grouped = values.groupBy { "${it.unit.lowercase()}|${it.size}" }
        return grouped.maxByOrNull { it.value.size }?.value?.firstOrNull()
    }
}
