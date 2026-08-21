package com.retailpos.app.data

import androidx.room.withTransaction
import com.retailpos.app.core.identifiers.ProductIdentityRules
import com.retailpos.app.core.identifiers.ProductIdentifierValidator
import com.retailpos.app.core.products.ProductCaptureParser
import com.retailpos.app.core.products.ProductLocalCandidate
import com.retailpos.app.core.products.ProductLocalCandidateRanking
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProductRepository(
    private val dao: ProductDao,
    private val barcodeDao: ProductBarcodeDao,
    private val database: RetailDatabase? = null
) {
    fun observeProducts(storeId: String): Flow<List<ProductEntity>> = dao.observeProducts(storeId)
    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>> = dao.searchProducts(storeId, query.trim())
    fun observeBarcodes(productId: String, storeId: String): Flow<List<ProductBarcodeEntity>> = barcodeDao.observeForProduct(productId, storeId)
    suspend fun getById(productId: String, storeId: String): ProductEntity? = dao.getById(productId, storeId)
    suspend fun getBySku(storeId: String, sku: String): ProductEntity? = dao.getBySku(storeId, ProductIdentityRules.normalizeSku(sku))
    suspend fun getByBarcode(storeId: String, value: String): ProductBarcodeEntity? = barcodeDao.getByValue(storeId, ProductIdentifierValidator.normalize(value))
    suspend fun getProductByBarcode(storeId: String, value: String): ProductEntity? = barcodeDao.getProductByBarcode(storeId, ProductIdentifierValidator.normalize(value))

    suspend fun findLocalCandidates(
        storeId: String,
        name: String?,
        brand: String?,
        observedPackSize: Double? = null,
        observedPackUnit: String? = null,
        limit: Int = 30
    ): List<ProductLocalCandidate> {
        val queries = listOf(name, brand)
            .filter { !it.isNullOrBlank() }
            .map { ProductCaptureParser.normalizeForMatching(it.orEmpty()) }
            .filter { it.isNotBlank() }
            .distinct()
        val candidates = queries.flatMap { dao.findLocalCandidates(storeId, it, limit) }.distinctBy { it.id }
        val metadata = if (database != null && candidates.isNotEmpty()) {
            database.productMetadataDao().getForProducts(storeId, candidates.map { it.id }).associateBy { it.productId }
        } else emptyMap()
        val feedback = if (database != null && candidates.isNotEmpty()) {
            candidates.associate { product -> product.id to database.productIdentificationFeedbackDao().rankingBoostForCandidate(storeId, product.id).coerceIn(-8, 8) }
        } else emptyMap()
        return ProductLocalCandidateRanking.rank(name, brand, candidates, observedPackSize, observedPackUnit, metadata, feedback)
    }

    suspend fun save(product: ProductEntity) = dao.upsert(product)

    suspend fun saveProductWithPrimaryBarcode(product: ProductEntity, barcodeType: String = "UNKNOWN"): Boolean {
        suspend fun operation(): Boolean {
            val normalized = ProductIdentityRules.normalizeBarcode(product.barcode.orEmpty())
            if (normalized.isNotBlank()) {
                if (!ProductIdentifierValidator.isValidRetailBarcode(normalized)) return false
                val existing = barcodeDao.getByValue(product.storeId, normalized)
                if (existing != null && existing.productId != product.id) return false
            }
            dao.upsert(product.copy(barcode = normalized.ifBlank { null }))
            return savePrimaryBarcode(product.id, product.storeId, normalized, barcodeType)
        }
        return if (database != null) database.withTransaction { operation() } else operation()
    }

    suspend fun saveProductWithMetadata(product: ProductEntity, metadata: ProductMetadataEntity, barcodeType: String = "UNKNOWN"): Boolean {
        require(metadata.productId == product.id) { "Product metadata must belong to the same product." }
        require(metadata.storeId == product.storeId) { "Product metadata must belong to the same store." }
        val db = database ?: return false
        return db.withTransaction {
            val normalized = ProductIdentityRules.normalizeBarcode(product.barcode.orEmpty())
            if (normalized.isNotBlank()) {
                if (!ProductIdentifierValidator.isValidRetailBarcode(normalized)) return@withTransaction false
                val existing = barcodeDao.getByValue(product.storeId, normalized)
                if (existing != null && existing.productId != product.id) return@withTransaction false
            }
            dao.upsert(product.copy(barcode = normalized.ifBlank { null }))
            if (!savePrimaryBarcode(product.id, product.storeId, normalized, barcodeType)) return@withTransaction false
            db.productMetadataDao().upsert(metadata)
            true
        }
    }

    suspend fun savePrimaryBarcode(productId: String, storeId: String, value: String, type: String = "UNKNOWN"): Boolean {
        val normalized = ProductIdentityRules.normalizeBarcode(value)
        if (normalized.isBlank()) {
            barcodeDao.deletePrimary(productId, storeId)
            return true
        }
        if (!ProductIdentifierValidator.isValidRetailBarcode(normalized)) return false
        val existing = barcodeDao.getByValue(storeId, normalized)
        if (existing != null && existing.productId != productId) return false
        barcodeDao.deletePrimary(productId, storeId)
        barcodeDao.upsert(ProductBarcodeEntity(UUID.randomUUID().toString(), productId, storeId, normalized, type, true, System.currentTimeMillis()))
        return true
    }

    suspend fun addSecondaryBarcode(productId: String, storeId: String, value: String, type: String = "UNKNOWN"): BarcodeMutationResult {
        val normalized = ProductIdentityRules.normalizeBarcode(value)
        if (normalized.isBlank() || !ProductIdentifierValidator.isValidRetailBarcode(normalized)) return BarcodeMutationResult.Invalid
        val existing = barcodeDao.getByValue(storeId, normalized)
        if (existing != null) return BarcodeMutationResult.Duplicate
        barcodeDao.upsert(ProductBarcodeEntity(UUID.randomUUID().toString(), productId, storeId, normalized, type, false, System.currentTimeMillis()))
        return BarcodeMutationResult.Success
    }

    suspend fun removeSecondaryBarcode(barcodeId: String, storeId: String) { barcodeDao.delete(barcodeId, storeId) }

    suspend fun delete(productId: String, storeId: String) {
        if (database != null) {
            database.withTransaction {
                barcodeDao.deleteForProduct(productId, storeId)
                dao.delete(productId, storeId)
            }
        } else {
            barcodeDao.deleteForProduct(productId, storeId)
            dao.delete(productId, storeId)
        }
    }
}

enum class BarcodeMutationResult { Success, Duplicate, Invalid }
