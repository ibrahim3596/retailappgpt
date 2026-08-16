package com.retailpos.app.data

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class ProductViewModel(
    application: Application,
    private val storeId: String
) : AndroidViewModel(application) {
    private val repository = ProductRepository(RetailDatabase.get(application).productDao())
    val query = mutableStateOf("")

    val products: StateFlow<List<ProductEntity>> =
        kotlinx.coroutines.flow.combine(
            MutableStateFlow(Unit),
            kotlinx.coroutines.flow.flow { emit(Unit) }
        ) { _, _ -> query.value }
            .flatMapLatest { search ->
                if (search.isBlank()) repository.observeProducts(storeId)
                else repository.searchProducts(storeId, search)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }
}

class ProductViewModelFactory(
    private val storeId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProductViewModel(
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                .get(Application::class.java),
            storeId
        ) as T
    }
}
