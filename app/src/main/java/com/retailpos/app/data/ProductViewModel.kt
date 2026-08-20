package com.retailpos.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.retailpos.app.core.identifiers.ProductIdentityRules
import com.retailpos.app.core.identifiers.ProductIdentifierValidator
import com.retailpos.app.core.products.ProductCaptureMetadataMapper
import com.retailpos.app.core.products.ProductCaptureObservation
import com.retailpos.app.core.products.ProductListFilter
import com.retailpos.app.core.products.ProductLocalCandidate
import com.retailpos.app.core.products.matches
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProductViewModel(application: Application, private val storeId: String) : AndroidViewModel(application) {
    private val database = RetailDatabase.get(application)
    private val repository = ProductRepository(database.productDao(), database.productBarcodeDao(), database)
    private val metadataRepository = ProductMetadataRepository(database.productMetadataDao())
    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(ProductListFilter.ALL)
    private val _editingProduct = MutableStateFlow<ProductEntity?>(null)
    private val _metadata = MutableStateFlow<ProductMetadataEntity?>(null)
    private val _barcodes = MutableStateFlow<List<ProductBarcodeEntity>>(emptyList())
    private val _localCandidates = MutableStateFlow<List<ProductLocalCandidate>>(emptyList())

    val query: StateFlow<String> = _query
    val filter: StateFlow<ProductListFilter> = _filter
    val editingProduct: StateFlow<ProductEntity?> = _editingProduct
    val metadata: StateFlow<ProductMetadataEntity?> = _metadata
    val barcodes: StateFlow<List<ProductBarcodeEntity>> = _barcodes
    val localCandidates: StateFlow<List<ProductLocalCandidate>> = _localCandidates

    val products: StateFlow<List<ProductEntity>> = _query
        .flatMapLatest { search ->
            if (search.isBlank()) repository.observeProducts(storeId)
            else repository.searchProducts(storeId, search)
        }
        .map { list -> list.filter { product -> _filter.value.matches(product.stock, product.lowStockThreshold) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { _query.value = value }
    fun setFilter(value: ProductListFilter) { _filter.value = value }

    fun findLocalCaptureCandidates(name: String?, brand: String?, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _localCandidates.value = repository.findLocalCandidates(storeId, name, brand)
            onComplete?.invoke()
        }
    }

    fun clearLocalCaptureCandidates() { _localCandidates.value = emptyList() }

    fun loadProduct(productId: String?) {
        if (productId == null) {
            _editingProduct.value = null
            _metadata.value = null
            _barcodes.value = emptyList()
            return
        }
        viewModelScope.launch {
            _editingProduct.value = repository.getById(productId, storeId)
            _metadata.value = metadataRepository.get(productId, storeId)
            repository.observeBarcodes(productId, storeId).collect { _barcodes.value = it }
        }
    }

    fun addSecondaryBarcode(productId: String?, value: String, type: String, onResult: (BarcodeMutationResult) -> Unit) {
        if (productId == null) { onResult(BarcodeMutationResult.Invalid); return }
        viewModelScope.launch { onResult(repository.addSecondaryBarcode(productId, storeId, value, type)) }
    }

    fun removeSecondaryBarcode(barcodeId: String) { viewModelScope.launch { repository.removeSecondaryBarcode(barcodeId, storeId) } }

    fun saveProduct(
        productId: String?, name: String, brand: String, barcode: String, sku: String,
        mrp: Double, sellingPrice: Double, purchasePrice: Double, stock: Double,
        unit: String, lowStockThreshold: Double,
        captureObservation: ProductCaptureObservation? = null,
        onResult: (SaveProductResult) -> Unit
    ) {
        val normalizedSku = ProductIdentityRules.normalizeSku(sku).ifBlank { null }
        val normalizedBarcode = ProductIdentityRules.normalizeBarcode(barcode)
        if (!ProductIdentityRules.isValidProductName(name) ||
            (normalizedSku != null && !ProductIdentityRules.isValidSku(normalizedSku)) ||
            (normalizedBarcode.isNotBlank() && !ProductIdentifierValidator.isValidRetailBarcode(normalizedBarcode)) ||
            mrp < 0 || sellingPrice < 0 || purchasePrice < 0 || stock < 0 || lowStockThreshold < 0 || sellingPrice > mrp
        ) { onResult(SaveProductResult.InvalidInput); return }

        viewModelScope.launch {
            try {
                if (normalizedSku != null) {
                    val existing = repository.getBySku(storeId, normalizedSku)
                    if (existing != null && existing.id != productId) { onResult(SaveProductResult.DuplicateSku); return@launch }
                }
                if (normalizedBarcode.isNotBlank()) {
                    val existing = repository.getByBarcode(storeId, normalizedBarcode)
                    if (existing != null && existing.productId != productId) { onResult(SaveProductResult.DuplicateBarcode); return@launch }
                }
                val current = productId?.let { repository.getById(it, storeId) }
                val product = ProductEntity(
                    id = productId ?: UUID.randomUUID().toString(), storeId = storeId,
                    name = name.trim(), brand = brand.trim(), barcode = normalizedBarcode.ifBlank { null }, sku = normalizedSku,
                    mrp = mrp, sellingPrice = sellingPrice, purchasePrice = purchasePrice,
                    stock = if (current != null) current.stock else stock,
                    unit = unit.trim().ifBlank { "pcs" }, lowStockThreshold = lowStockThreshold,
                    updatedAt = System.currentTimeMillis()
                )
                val saved = if (captureObservation != null) {
                    val existingMetadata = productId?.let { metadataRepository.get(it, storeId) }
                    val metadata = ProductCaptureMetadataMapper.map(
                        productId = product.id,
                        storeId = storeId,
                        observation = captureObservation,
                        existing = existingMetadata
                    )
                    repository.saveProductWithMetadata(product, metadata)
                } else {
                    repository.saveProductWithPrimaryBarcode(product)
                }
                if (!saved) {
                    onResult(SaveProductResult.DuplicateBarcode); return@launch
                }
                onResult(SaveProductResult.Success)
            } catch (_: Exception) { onResult(SaveProductResult.Error) }
        }
    }
}

enum class SaveProductResult { Success, DuplicateSku, DuplicateBarcode, InvalidInput, Error }

class ProductViewModelFactory(private val storeId: String) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] ?: error("Application is required")
        @Suppress("UNCHECKED_CAST") return ProductViewModel(application, storeId) as T
    }
}
