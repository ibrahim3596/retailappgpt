package com.example.retailpos.engine.fefo

import com.example.retailpos.data.local.dao.BatchDao
import com.example.retailpos.data.local.dao.ProductDao
import com.example.retailpos.data.local.dao.StockMovementDao
import com.example.retailpos.data.local.entity.BatchEntity
import com.example.retailpos.data.local.entity.StockMovementEntity
import com.example.retailpos.data.local.entity.StockMovementType
import java.util.UUID

class InsufficientStockException(
    val productId: String,
    val requested: Double,
    val available: Double
) : Exception("Insufficient stock for product $productId. Requested: $requested, Available: $available")

data class BatchDeduction(
    val batchId: String,
    val batchNumber: String,
    val expiryDate: Long,
    val deductedQty: Double,
    val remainingInBatch: Double
)

class FefoAllocationEngine(
    private val productDao: ProductDao,
    private val batchDao: BatchDao,
    private val stockMovementDao: StockMovementDao
) {

    suspend fun allocateAndDeductFefo(
        storeId: String,
        productId: String,
        requestedQty: Double,
        referenceId: String,
        createdBy: String = "POS"
    ): List<BatchDeduction> {
        val product = productDao.getProductById(storeId, productId)
            ?: throw IllegalArgumentException("Product not found: $productId")

        val batches = batchDao.getFefoBatchesForProduct(storeId, productId)
        val totalAvailableInBatches = batches.sumOf { it.remainingQty }

        if (product.currentStock < requestedQty || totalAvailableInBatches < requestedQty) {
            val available = minOf(product.currentStock, totalAvailableInBatches)
            throw InsufficientStockException(productId, requestedQty, available)
        }

        var remainingToDeduct = requestedQty
        val deductions = mutableListOf<BatchDeduction>()
        var runningProductBalance = product.currentStock

        for (batch in batches) {
            if (remainingToDeduct <= 0) break

            val deductFromBatch = minOf(batch.remainingQty, remainingToDeduct)
            val updatedBatchRemaining = batch.remainingQty - deductFromBatch

            // Atomic batch deduction
            val updatedRows = batchDao.atomicDeductBatchQty(batch.id, deductFromBatch)
            if (updatedRows == 0) {
                // Concurrency retry fail
                throw InsufficientStockException(productId, requestedQty, batch.remainingQty)
            }

            remainingToDeduct -= deductFromBatch
            runningProductBalance -= deductFromBatch

            // Create StockMovement for batch
            val movement = StockMovementEntity(
                id = UUID.randomUUID().toString(),
                storeId = storeId,
                productId = productId,
                batchId = batch.id,
                type = StockMovementType.SALE,
                quantity = -deductFromBatch,
                balanceAfter = runningProductBalance,
                referenceId = referenceId,
                notes = "FEFO Deduction from Batch ${batch.batchNumber}",
                timestamp = System.currentTimeMillis(),
                createdBy = createdBy
            )
            stockMovementDao.insertStockMovement(movement)

            deductions.add(
                BatchDeduction(
                    batchId = batch.id,
                    batchNumber = batch.batchNumber,
                    expiryDate = batch.expiryDate,
                    deductedQty = deductFromBatch,
                    remainingInBatch = updatedBatchRemaining
                )
            )
        }

        if (remainingToDeduct > 0) {
            throw InsufficientStockException(productId, requestedQty, totalAvailableInBatches - remainingToDeduct)
        }

        // Deduct overall product stock
        val productUpdatedRows = productDao.atomicDeductStock(productId, storeId, requestedQty)
        if (productUpdatedRows == 0) {
            throw InsufficientStockException(productId, requestedQty, product.currentStock)
        }

        return deductions
    }
}
