package com.retailpos.app.data

import com.retailpos.app.core.identifiers.ProductIdentifierValidator
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
            barcodeDao.deletePrimary(productId, storeId)
            return true
        }
        if (!ProductIdentifierValidator.isValidRetailBarcode(normalized)) return false

        val existing = barcodeDao.getByValue(storeId, normalized)
        if (existing != null && existing.productId != productId) return false

        barcodeDao.deletePrimary(productId, storeId)
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

    suspend fun addSecondaryBarcode(
        productId: String,
        storeId: String,
        value: String,
        type: String = "UNKNOWN"
    ): BarcodeMutationResult {
        val normalized = ProductIdentifierValidator.normalize(value)
        if (normalized.isBlank() || !ProductIdentifierValidator.isValidRetailBarcode(normalized)) {
            return BarcodeMutationResult.Invalid
        }
        val existing = barcodeDao.getByValue(storeId, normalized)
        if (existing != null) return BarcodeMutationResult.Duplicate

        barcodeDao.upsert(
            ProductBarcodeEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                storeId = storeId,
                value = normalized,
                type = type,
                isPrimary = false,
                createdAt = System.currentTimeMillis()
            )
        )
        return BarcodeMutationResult.Success
    }

    suspend fun removeSecondaryBarcode(barcodeId: String, storeId: String) {
        barcodeDao.delete(barcodeId, storeId)
    }

    suspend fun delete(productId: String, storeId: String) {
        barcodeDao.deleteForProduct(productId, storeId)
        dao.delete(productId, storeId)
    }
}

enum class BarcodeMutationResult {
    Success,
    Duplicate,
    Invalid
}
