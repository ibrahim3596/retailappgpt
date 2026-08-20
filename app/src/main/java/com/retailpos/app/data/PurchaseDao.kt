package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class SupplierDao {
    @Query("SELECT * FROM suppliers WHERE storeId = :storeId ORDER BY name COLLATE NOCASE")
    abstract suspend fun getAll(storeId: String): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :supplierId AND storeId = :storeId LIMIT 1")
    abstract suspend fun getById(supplierId: String, storeId: String): SupplierEntity?

    @Insert
    abstract suspend fun insert(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET name = :name, phone = :phone, address = :address, notes = :notes, updatedAt = :updatedAt WHERE id = :supplierId AND storeId = :storeId")
    abstract suspend fun update(supplierId: String, storeId: String, name: String, phone: String, address: String, notes: String, updatedAt: Long): Int
}

@Dao
abstract class PurchaseDao {
    @Query("SELECT * FROM purchases WHERE storeId = :storeId ORDER BY createdAt DESC")
    abstract suspend fun getAll(storeId: String): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE id = :purchaseId AND storeId = :storeId LIMIT 1")
    abstract suspend fun getById(purchaseId: String, storeId: String): PurchaseEntity?

    @Query("SELECT * FROM purchase_lines WHERE purchaseId = :purchaseId ORDER BY rowid")
    abstract suspend fun getLines(purchaseId: String): List<PurchaseLineEntity>

    @Insert
    abstract suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert
    abstract suspend fun insertLines(lines: List<PurchaseLineEntity>)

    @Transaction
    open suspend fun save(purchase: PurchaseEntity, lines: List<PurchaseLineEntity>) {
        require(lines.isNotEmpty()) { "Purchase must contain at least one line" }
        insertPurchase(purchase)
        insertLines(lines)
    }
}

@Dao
interface SupplierLedgerDao {
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM supplier_ledger WHERE storeId = :storeId AND supplierId = :supplierId")
    suspend fun balance(storeId: String, supplierId: String): Double

    @Query("SELECT * FROM supplier_ledger WHERE storeId = :storeId AND supplierId = :supplierId ORDER BY createdAt DESC")
    suspend fun entries(storeId: String, supplierId: String): List<SupplierLedgerEntry>

    @Insert
    suspend fun insert(entry: SupplierLedgerEntry)
}
