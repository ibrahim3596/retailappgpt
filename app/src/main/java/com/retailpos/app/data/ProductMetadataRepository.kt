package com.retailpos.app.data

import kotlinx.coroutines.flow.Flow

class ProductMetadataRepository(private val dao: ProductMetadataDao) {
    suspend fun get(productId: String, storeId: String): ProductMetadataEntity? = dao.get(productId, storeId)

    fun observe(productId: String, storeId: String): Flow<ProductMetadataEntity?> =
        dao.observe(productId, storeId)

    fun observeCategories(storeId: String): Flow<List<String>> = dao.observeCategories(storeId)

    suspend fun save(metadata: ProductMetadataEntity) = dao.upsert(metadata)

    suspend fun delete(productId: String, storeId: String) = dao.delete(productId, storeId)
}
