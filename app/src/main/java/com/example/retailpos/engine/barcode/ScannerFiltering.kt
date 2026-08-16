package com.example.retailpos.engine.barcode

import com.google.mlkit.vision.barcode.common.Barcode

object ScannerFiltering {
    /**
     * Determines if a detected barcode format is supported for product scanning.
     * QR Codes are explicitly rejected for product lookup/billing.
     */
    fun isSupportedProductFormat(rawFormat: Int): Boolean {
        return when (rawFormat) {
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_CODE_128 -> true
            Barcode.FORMAT_QR_CODE -> false
            else -> false // Unknown or unsupported formats
        }
    }

    fun getFormatName(rawFormat: Int): String {
        return when (rawFormat) {
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_CODE_128 -> "CODE-128"
            Barcode.FORMAT_QR_CODE -> "QR-CODE"
            else -> "BARCODE"
        }
    }
}
