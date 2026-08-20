package com.retailpos.app.core.products

enum class ProductBarcodeDecision { ACCEPT, IGNORE_QR, REJECT_INVALID }

object ProductBarcodeSafety {
    fun classify(raw: String): ProductBarcodeDecision {
        val value = raw.trim()
        if (value.isBlank()) return ProductBarcodeDecision.REJECT_INVALID
        if (value.contains("://") || value.contains("upi://") || value.contains("BEGIN:VCARD", true)) return ProductBarcodeDecision.IGNORE_QR
        val normalized = value.filter(Char::isLetterOrDigit)
        if (normalized.length !in 8..18) return ProductBarcodeDecision.REJECT_INVALID
        return ProductBarcodeDecision.ACCEPT
    }
}
