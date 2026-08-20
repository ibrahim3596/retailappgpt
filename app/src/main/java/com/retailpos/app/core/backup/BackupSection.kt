package com.retailpos.app.core.backup

object BackupSection {
    const val PRODUCTS = "products"
    const val PRODUCT_BARCODES = "product_barcodes"
    const val PRODUCT_METADATA = "product_metadata"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_LEDGER = "customer_ledger"
    const val SUPPLIERS = "suppliers"
    const val PURCHASES = "purchases"
    const val SUPPLIER_LEDGER = "supplier_ledger"
    const val SALES = "sales"
    const val SALE_LINES = "sale_lines"
    const val INVENTORY_MOVEMENTS = "inventory_movements"
    const val INVENTORY_BATCHES = "inventory_batches"
    const val EXPENSES = "expenses"
    const val HELD_BILLS = "held_bills"
    const val SETTINGS = "store_settings"

    val ALL = listOf(
        PRODUCTS, PRODUCT_BARCODES, PRODUCT_METADATA,
        CUSTOMERS, CUSTOMER_LEDGER,
        SUPPLIERS, PURCHASES, SUPPLIER_LEDGER,
        SALES, SALE_LINES,
        INVENTORY_MOVEMENTS, INVENTORY_BATCHES,
        EXPENSES, HELD_BILLS, SETTINGS
    )
}
