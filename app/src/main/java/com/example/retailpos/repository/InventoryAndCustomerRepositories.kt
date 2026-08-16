package com.example.retailpos.repository

import androidx.room.withTransaction
import com.example.retailpos.data.local.AppDatabase
import com.example.retailpos.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class InventoryRepository(private val db: AppDatabase) {

    fun getProducts(storeId: String): Flow<List<ProductEntity>> = db.productDao().getAllProducts(storeId)

    fun getLowStockProducts(storeId: String): Flow<List<ProductEntity>> = db.productDao().getLowStockProducts(storeId)

    suspend fun getProductBySku(storeId: String, sku: String): ProductEntity? = db.productDao().getProductBySku(storeId, sku)

    suspend fun saveProduct(product: ProductEntity) = db.withTransaction {
        val existing = db.productDao().getProductById(product.storeId, product.id)
        if (existing == null) {
            db.productDao().insertProduct(product)
            // Log initial stock movement
            if (product.currentStock > 0) {
                val movement = StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = product.storeId,
                    productId = product.id,
                    type = StockMovementType.INITIAL,
                    quantity = product.currentStock,
                    balanceAfter = product.currentStock,
                    notes = "Initial product stock setup"
                )
                db.stockMovementDao().insertStockMovement(movement)
            }
        } else {
            db.productDao().updateProduct(product)
        }
    }

    suspend fun addBatch(batch: BatchEntity) = db.withTransaction {
        db.batchDao().insertBatch(batch)
        // Add batch quantity to product stock
        db.productDao().atomicAddStock(batch.productId, batch.storeId, batch.initialQty)

        val product = db.productDao().getProductById(batch.storeId, batch.productId)
        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            storeId = batch.storeId,
            productId = batch.productId,
            batchId = batch.id,
            type = StockMovementType.PURCHASE,
            quantity = batch.initialQty,
            balanceAfter = product?.currentStock ?: batch.initialQty,
            notes = "New Batch Addition: ${batch.batchNumber}"
        )
        db.stockMovementDao().insertStockMovement(movement)
    }

    suspend fun quickRestock(
        storeId: String,
        productId: String,
        addQty: Double,
        mrp: Double,
        sellingPrice: Double,
        purchasePrice: Double
    ) = db.withTransaction {
        val batch = BatchEntity(
            id = UUID.randomUUID().toString(),
            productId = productId,
            storeId = storeId,
            batchNumber = "RESTOCK-" + System.currentTimeMillis().toString().takeLast(6),
            expiryDate = System.currentTimeMillis() + (180L * 24 * 3600 * 1000), // 6 months
            mrp = mrp,
            sellingPrice = sellingPrice,
            purchasePrice = purchasePrice,
            initialQty = addQty,
            remainingQty = addQty
        )
        // Re-use addBatch logic (must be within the same transaction if called here, but addBatch also has its own transaction)
        // Actually, db.withTransaction is re-entrant if it's the same thread/transaction.
        // But better to just inline the logic or move it to a non-transactional private helper.
        internalAddBatch(batch)
    }

    private suspend fun internalAddBatch(batch: BatchEntity) {
        db.batchDao().insertBatch(batch)
        db.productDao().atomicAddStock(batch.productId, batch.storeId, batch.initialQty)
        val product = db.productDao().getProductById(batch.storeId, batch.productId)
        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            storeId = batch.storeId,
            productId = batch.productId,
            batchId = batch.id,
            type = StockMovementType.PURCHASE,
            quantity = batch.initialQty,
            balanceAfter = product?.currentStock ?: 0.0,
            notes = "Quick Restock Batch: ${batch.batchNumber}"
        )
        db.stockMovementDao().insertStockMovement(movement)
    }

    fun getBatchesForProduct(storeId: String, productId: String): Flow<List<BatchEntity>> =
        db.batchDao().getBatchesForProductFlow(storeId, productId)

    fun getExpiringBatches(storeId: String, withinDays: Int = 30): Flow<List<BatchEntity>> {
        val threshold = System.currentTimeMillis() + (withinDays * 24 * 60 * 60 * 1000L)
        return db.batchDao().getExpiringBatches(storeId, threshold)
    }

    fun getStockMovements(storeId: String): Flow<List<StockMovementEntity>> =
        db.stockMovementDao().getAllMovements(storeId)

    fun getMovementsForProduct(storeId: String, productId: String): Flow<List<StockMovementEntity>> =
        db.stockMovementDao().getMovementsForProduct(storeId, productId)

    suspend fun adjustStock(
        storeId: String,
        productId: String,
        quantityChange: Double,
        type: StockMovementType,
        notes: String
    ): Boolean = db.withTransaction {
        val product = db.productDao().getProductById(storeId, productId) ?: return@withTransaction false
        
        val affectedRows = if (quantityChange > 0) {
            db.productDao().atomicAddStock(productId, storeId, quantityChange)
        } else {
            db.productDao().atomicDeductStock(productId, storeId, -quantityChange)
        }

        if (affectedRows > 0) {
            val updatedProduct = db.productDao().getProductById(storeId, productId)
            val movement = StockMovementEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = productId,
                type = type,
                quantity = quantityChange,
                balanceAfter = updatedProduct?.currentStock ?: 0.0,
                notes = notes
            )
            db.stockMovementDao().insertStockMovement(movement)
            true
        } else {
            false
        }
    }
}

class CustomerRepository(private val db: AppDatabase) {

    fun getAllCustomers(storeId: String): Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers(storeId)

    suspend fun saveCustomer(customer: CustomerEntity) {
        db.customerDao().insertCustomer(customer)
    }

    fun getLedgerForCustomer(storeId: String, customerId: String): Flow<List<CreditLedgerEntryEntity>> =
        db.creditLedgerDao().getLedgerForCustomer(storeId, customerId)

    suspend fun recordPayment(storeId: String, customerId: String, amount: Double, notes: String) {
        val customer = db.customerDao().getCustomerById(storeId, customerId) ?: return
        val newBalance = (customer.currentBalance - amount).coerceAtLeast(0.0)

        db.customerDao().updateBalance(customerId, storeId, -amount)

        val entry = CreditLedgerEntryEntity(
            id = UUID.randomUUID().toString(),
            storeId = storeId,
            customerId = customerId,
            type = LedgerEntryType.CREDIT,
            amount = amount,
            balanceAfter = newBalance,
            referenceId = "PAY-" + UUID.randomUUID().toString().take(8),
            notes = notes.ifEmpty { "Khata Payment Received" }
        )
        db.creditLedgerDao().insertLedgerEntry(entry)
    }
}
