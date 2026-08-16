package com.retailpos.app.data

import kotlinx.coroutines.flow.Flow

class ProductRepository(private val dao: ProductDao) {
    fun observeProducts(storeId: String): Flow<List<ProductEntity>> = dao.observeProducts(storeId)

    fun searchProducts(storeId: String, query: String): Flow<List<ProductEntity>> =
        dao.searchProducts(storeId, query.trim())

    suspend fun getById(productId: String, storeId: String): ProductEntity? =
        dao.getById(productId, storeId)

    suspend fun getBySku(storeId: String, sku: String): ProductEntity? =
        dao.getBySku(storeId, sku)

    suspend fun save(product: ProductEntity) = dao.upsert(product)

    suspend fun delete(productId: String, storeId: String) = dao.delete(productId, storeId)
}
