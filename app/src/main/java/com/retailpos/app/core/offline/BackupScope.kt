package com.retailpos.app.core.offline

enum class BackupSection { STORE_SETTINGS, PRODUCTS, BARCODES, CUSTOMERS, KHAATA, SUPPLIERS, PURCHASES, SALES, INVENTORY, EXPENSES, HELD_BILLS }

object BackupScope {
    val DEFAULT = BackupSection.entries.toSet()

    fun validate(selected: Set<BackupSection>): Set<BackupSection> =
        selected.filterTo(mutableSetOf()) { it in BackupSection.entries }
}
