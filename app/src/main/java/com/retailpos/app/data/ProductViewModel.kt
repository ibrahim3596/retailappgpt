package com.retailpos.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ProductViewModel(
    application: Application,
    private val storeId: String
) : AndroidViewModel(application) {
    private val repository = ProductRepository(RetailDatabase.get(application).productDao())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val products: StateFlow<List<ProductEntity>> =
        _query
            .flatMapLatest { search ->
                if (search.isBlank()) repository.observeProducts(storeId)
                else repository.searchProducts(storeId, search)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun saveProduct(
        name: String,
        brand: String,
        barcode: String,
        sku: String,
        mrp: Double,
        sellingPrice: Double,
        purchasePrice: Double,
        stock: Double,
        unit: String,
        onSaved: () -> Unit
    ) {
        if (name.isBlank() || mrp < 0 || sellingPrice < 0 || purchasePrice < 0 || stock < 0) return
        viewModelScope.launch {
            repository.save(
                ProductEntity(
                    id = UUID.randomUUID().toString(),
                    storeId = storeId,
                    name = name.trim(),
                    brand = brand.trim(),
                    barcode = barcode.trim().ifBlank { null },
                    sku = sku.trim().ifBlank { null },
                    mrp = mrp,
                    sellingPrice = sellingPrice,
                    purchasePrice = purchasePrice,
                    stock = stock,
                    unit = unit.trim().ifBlank { "pcs" },
                    updatedAt = System.currentTimeMillis()
                )
            )
            onSaved()
        }
    }
}

class ProductViewModelFactory(
    private val storeId: String
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(
        modelClass: Class<T>,
        extras: androidx.lifecycle.viewmodel.MutableCreationExtras
    ): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application is required")
        @Suppress("UNCHECKED_CAST")
        return ProductViewModel(application, storeId) as T
    }
}
