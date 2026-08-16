package com.example.retailpos.domain.model

/**
 * Barcode symbologies RetailPOS can recognize.
 * GTIN variants are identifiers; symbology describes how an identifier is encoded.
 */
enum class BarcodeType {
    EAN_8,
    EAN_13,
    UPC_A,
    UPC_E,
    ITF_14,
    CODE_128,
    CODE_39,
    CODE_93,
    CODABAR,
    ITF,
    QR_CODE,
    DATA_MATRIX,
    PDF_417,
    AZTEC,
    UNKNOWN
}
