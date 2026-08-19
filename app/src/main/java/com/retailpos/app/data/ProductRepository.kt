package com.retailpos.app.data

import com.example.retailpos.domain.model.ProductIdentifierValidator
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProductRepository(
    private val dao: ProductDao,
    private val barcodeDao: ProductBarcodeDao
) {
    fun observeProducts(storeId: String): Flow<List<ProductEntity>> = dao.observeProducts(storeId)

    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>> =
        dao.searchProducts(storeId, query.trim())

    fun observeBarcodes(productId: String, storeId: String): Flow<List<ProductBarcodeEntity>> =
        barcodeDao.observeForProduct(productId, storeId)

    suspend fun getById(productId: String, storeId: String): ProductEntity? =
        dao.getById(productId, storeId)

    suspend fun getBySku(storeId: String, sku: String): ProductEntity? =
        dao.getBySku(storeId, sku)

    suspend fun getByBarcode(storeId: String, value: String): ProductBarcodeEntity? =
        barcodeDao.getByValue(storeId, ProductIdentifierValidator.normalize(value))

    suspend fun save(product: ProductEntity) = dao.upsert(product)

    suspend fun savePrimaryBarcode(
        productId: String,
        storeId: String,
        value: String,
        type: String = "UNKNOWN"
    ): Boolean {
        val normalized = ProductIdentifierValidator.normalize(value)
        if (normalized.isBlank()) {
            barcodeDao.deleteForProduct(productId, storeId)
            return true
        }

        val existing = barcodeDao.getByValue(storeId, normalized)
        if (existing != null && existing.productId != productId) return false

        barcodeDao.deleteForProduct(productId, storeId)
        barcodeDao.upsert(
            ProductBarcodeEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                storeId = storeId,
                value = normalized,
                type = type,
                isPrimary = true,
                createdAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun delete(productId: String, storeId: String) {
        barcodeDao.deleteForProduct(productId, storeId)
        dao.delete(productId, storeId)
    }
}
