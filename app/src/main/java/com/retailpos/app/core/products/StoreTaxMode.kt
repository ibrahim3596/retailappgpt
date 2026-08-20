package com.retailpos.app.core.products

enum class StoreTaxMode(
    val storageValue: String,
    val label: String,
    val description: String
) {
    NO_GST(
        "NO_GST",
        "I don't charge GST",
        "The POS will not add GST to customer bills."
    ),
    REGULAR(
        "REGULAR",
        "Regular GST taxpayer",
        "Product GST rates can be applied to taxable sales."
    ),
    COMPOSITION(
        "COMPOSITION",
        "Composition taxpayer",
        "GST is not added as a separate customer charge."
    );

    companion object {
        fun fromStorage(value: String): StoreTaxMode =
            entries.firstOrNull { it.storageValue == value } ?: NO_GST
    }
}

fun StoreTaxMode.toTaxTreatment(): TaxTreatment = when (this) {
    StoreTaxMode.NO_GST, StoreTaxMode.COMPOSITION -> TaxTreatment.NO_TAX
    StoreTaxMode.REGULAR -> TaxTreatment.GST_ADDED
}
