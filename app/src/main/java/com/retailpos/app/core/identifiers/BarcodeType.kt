package com.retailpos.app.core.identifiers

/**
 * Barcode and 2D symbologies RetailPOS may encounter while scanning.
 * A symbology describes how the value is encoded; it is not itself proof
 * that the value is a globally issued product identifier.
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
