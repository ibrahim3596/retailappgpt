package com.retailpos.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SaleDao {
    @Insert suspend fun insertSale(sale: SaleEntity)
    @Insert suspend fun insertLines(lines: List<SaleLineEntity>)
    @Insert suspend fun insertInventoryMovements(movements: List<InventoryMovementEntity>)
    @Insert suspend fun insertCostAllocations(allocations: List<SaleCostAllocationEntity>)
    @Insert suspend fun insertLedgerEntry(entry: CustomerLedgerEntry)

    @Query("SELECT * FROM sales WHERE storeId = :storeId AND idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun findByIdempotencyKey(storeId: String, idempotencyKey: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE id = :saleId AND storeId = :storeId LIMIT 1")
    suspend fun getSale(storeId: String, saleId: String): SaleEntity?

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY id")
    suspend fun getSaleLines(saleId: String): List<SaleLineEntity>

    @Query("SELECT * FROM sale_cost_allocations WHERE saleId = :saleId ORDER BY createdAt ASC")
    suspend fun getSaleCostAllocations(saleId: String): List<SaleCostAllocationEntity>

    @Query("SELECT COALESCE(SUM(totalCost), 0) FROM sale_cost_allocations WHERE saleId IN (SELECT id FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end)")
    suspend fun getCogsTotal(storeId: String, start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end")
    suspend fun getSalesTotal(storeId: String, start: Long, end: Long): Double

    @Query("SELECT COUNT(*) FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end")
    suspend fun getSalesCount(storeId: String, start: Long, end: Long): Int

    @Query("SELECT COALESCE(SUM(sl.quantity), 0) FROM sale_lines sl INNER JOIN sales s ON s.id = sl.saleId WHERE s.storeId = :storeId AND s.createdAt >= :start AND s.createdAt < :end")
    suspend fun getItemsSold(storeId: String, start: Long, end: Long): Double

    @Query("SELECT paymentMethod, COUNT(*) AS transactionCount, COALESCE(SUM(total), 0) AS total FROM sales WHERE storeId = :storeId AND createdAt >= :start AND createdAt < :end GROUP BY paymentMethod ORDER BY total DESC")
    suspend fun getPaymentSummary(storeId: String, start: Long, end: Long): List<PaymentSummary>

    @Query("SELECT sl.productId AS productId, MAX(sl.name) AS name, COALESCE(SUM(sl.quantity), 0.0) AS quantity, COALESCE(SUM(sl.total), 0.0) AS revenue FROM sale_lines sl INNER JOIN sales s ON s.id = sl.saleId WHERE s.storeId = :storeId AND s.createdAt >= :start AND s.createdAt < :end GROUP BY sl.productId ORDER BY quantity DESC, revenue DESC LIMIT :limit")
    suspend fun getTopProducts(storeId: String, start: Long, end: Long, limit: Int = 10): List<TopProductSales>

    @Query("SELECT * FROM sales WHERE storeId = :storeId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentSales(storeId: String, limit: Int): List<SaleEntity>

    @Query("SELECT * FROM product_metadata WHERE productId = :productId AND storeId = :storeId LIMIT 1")
    suspend fun getProductMetadata(productId: String, storeId: String): ProductMetadataEntity?

    @Query("SELECT purchasePrice FROM products WHERE id = :productId AND storeId = :storeId LIMIT 1")
    suspend fun getCurrentPurchasePrice(productId: String, storeId: String): Double?

    @Query("SELECT * FROM store_settings WHERE storeId = :storeId LIMIT 1")
    suspend fun getStoreSettings(storeId: String): StoreSettingsEntity?

    @Query("SELECT isArchived FROM products WHERE id = :productId AND storeId = :storeId LIMIT 1")
    suspend fun isProductArchived(productId: String, storeId: String): Boolean?

    @Query("SELECT * FROM products WHERE id = :productId AND storeId = :storeId LIMIT 1")
    suspend fun queryProduct(productId: String, storeId: String): ProductEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM customers WHERE id = :customerId AND storeId = :storeId)")
    suspend fun customerBelongsToStore(customerId: String, storeId: String): Boolean

    @Query("UPDATE products SET stock = stock - :quantity, updatedAt = :updatedAt WHERE id = :productId AND storeId = :storeId AND stock >= :quantity")
    suspend fun decrementStock(productId: String, storeId: String, quantity: Double, updatedAt: Long): Int

    @Query("SELECT * FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0 AND (expiryDate IS NULL OR expiryDate > :now) ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, createdAt ASC")
    suspend fun getAvailableBatchesFefo(storeId: String, productId: String, now: Long): List<InventoryBatchEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM inventory_batches WHERE storeId = :storeId AND productId = :productId AND quantity > 0)")
    suspend fun hasPositiveBatchStock(storeId: String, productId: String): Boolean

    @Query("UPDATE inventory_batches SET quantity = quantity - :quantity WHERE id = :batchId AND storeId = :storeId AND quantity >= :quantity")
    suspend fun decrementBatch(batchId: String, storeId: String, quantity: Double): Int
}
