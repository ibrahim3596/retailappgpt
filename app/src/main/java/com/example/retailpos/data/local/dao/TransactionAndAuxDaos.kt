package com.example.retailpos.data.local.dao

import androidx.room.*
import com.example.retailpos.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE storeId = :storeId AND productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(storeId: String, productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE storeId = :storeId ORDER BY timestamp DESC LIMIT 100")
    fun getAllMovements(storeId: String): Flow<List<StockMovementEntity>>
}

@Dao
interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE storeId = :storeId AND localId = :localId LIMIT 1")
    suspend fun getInvoiceByLocalId(storeId: String, localId: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE storeId = :storeId AND id = :id LIMIT 1")
    suspend fun getInvoiceById(storeId: String, id: String): InvoiceEntity?

    @Transaction
    @Query("SELECT * FROM invoices WHERE storeId = :storeId ORDER BY createdAt DESC")
    fun getAllInvoices(storeId: String): Flow<List<InvoiceWithItems>>

    @Query("SELECT * FROM invoices WHERE storeId = :storeId AND createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getInvoicesForRange(storeId: String, startTime: Long, endTime: Long): Flow<List<InvoiceEntity>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE storeId = :storeId AND createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getInvoicesWithItemsForRange(storeId: String, startTime: Long, endTime: Long): Flow<List<InvoiceWithItems>>

    @Query("SELECT COUNT(*) FROM invoices WHERE storeId = :storeId")
    suspend fun getInvoiceCount(storeId: String): Int
}

@Dao
interface InvoiceItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getInvoiceItemsFlow(invoiceId: String): Flow<List<InvoiceItemEntity>>
}

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE storeId = :storeId ORDER BY name ASC")
    fun getAllCustomers(storeId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE storeId = :storeId AND id = :id LIMIT 1")
    suspend fun getCustomerById(storeId: String, id: String): CustomerEntity?

    @Query("UPDATE customers SET currentBalance = currentBalance + :amount, updatedAt = :updatedAt WHERE id = :customerId AND storeId = :storeId")
    suspend fun updateBalance(customerId: String, storeId: String, amount: Double, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface CreditLedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: CreditLedgerEntryEntity)

    @Query("SELECT * FROM credit_ledger WHERE storeId = :storeId AND customerId = :customerId ORDER BY timestamp DESC")
    fun getLedgerForCustomer(storeId: String, customerId: String): Flow<List<CreditLedgerEntryEntity>>
}

@Dao
interface SupplierDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM suppliers WHERE storeId = :storeId ORDER BY name ASC")
    fun getAllSuppliers(storeId: String): Flow<List<SupplierEntity>>
}

@Dao
interface PurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Query("SELECT * FROM purchases WHERE storeId = :storeId ORDER BY createdAt DESC")
    fun getAllPurchases(storeId: String): Flow<List<PurchaseEntity>>
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(syncLog: SyncLogEntity)

    @Query("SELECT * FROM sync_queue WHERE storeId = :storeId AND status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingSyncLogs(storeId: String): List<SyncLogEntity>

    @Update
    suspend fun updateSyncLog(syncLog: SyncLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncCommand(command: SyncCommandEntity)

    @Query("SELECT * FROM sync_commands WHERE storeId = :storeId AND status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingSyncCommands(storeId: String): List<SyncCommandEntity>

    @Update
    suspend fun updateSyncCommand(command: SyncCommandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncConflict(conflict: SyncConflictEntity)

    @Query("SELECT * FROM sync_conflicts WHERE storeId = :storeId AND status = 'UNRESOLVED' ORDER BY createdAt DESC")
    fun getUnresolvedConflicts(storeId: String): Flow<List<SyncConflictEntity>>

    @Update
    suspend fun updateSyncConflict(conflict: SyncConflictEntity)
}

@Dao
interface ProvenanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvenance(provenance: ProductProvenanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvenanceHistory(history: ProductProvenanceHistoryEntity)

    @Query("SELECT * FROM product_provenance WHERE productId = :productId")
    fun getProvenanceForProduct(productId: String): Flow<List<ProductProvenanceEntity>>
}
