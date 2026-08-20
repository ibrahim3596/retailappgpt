package com.retailpos.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.retailpos.app.core.products.ProductMetadataFormState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductMetadataViewModel(
    application: Application,
    private val storeId: String
) : AndroidViewModel(application) {
    private val repository = ProductMetadataRepository(RetailDatabase.get(application).productMetadataDao())
    private val _form = MutableStateFlow(ProductMetadataFormState())
    private val _error = MutableStateFlow<String?>(null)
    val form: StateFlow<ProductMetadataFormState> = _form.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(productId: String) {
        viewModelScope.launch {
            val metadata = repository.get(productId, storeId)
            _form.value = if (metadata == null) ProductMetadataFormState() else ProductMetadataFormState(
                category = metadata.category,
                subcategory = metadata.subcategory,
                packSize = metadata.packSize?.toString().orEmpty(),
                packUnit = metadata.packUnit,
                description = metadata.description,
                imageUri = metadata.imageUri
            )
            _error.value = null
        }
    }

    fun update(transform: (ProductMetadataFormState) -> ProductMetadataFormState) {
        _form.value = transform(_form.value)
        _error.value = null
    }

    fun setImageUri(uri: String?) {
        _form.value = _form.value.copy(imageUri = uri)
    }

    fun clearError() { _error.value = null }

    fun save(productId: String, onResult: (ProductMetadataSaveResult) -> Unit) {
        val normalized = _form.value.normalized()
        val validationError = normalized.validate()
        if (validationError != null) {
            _error.value = validationError
            onResult(ProductMetadataSaveResult.Invalid(validationError))
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.save(
                    ProductMetadataEntity(
                        productId = productId,
                        storeId = storeId,
                        category = normalized.category,
                        subcategory = normalized.subcategory,
                        packSize = normalized.packSizeValue(),
                        packUnit = normalized.packUnit,
                        description = normalized.description,
                        imageUri = normalized.imageUri,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }.onSuccess {
                _form.value = normalized
                _error.value = null
                onResult(ProductMetadataSaveResult.Success)
            }.onFailure {
                _error.value = "Unable to save product details."
                onResult(ProductMetadataSaveResult.Error)
            }
        }
    }
}

sealed interface ProductMetadataSaveResult {
    data object Success : ProductMetadataSaveResult
    data object Error : ProductMetadataSaveResult
    data class Invalid(val message: String) : ProductMetadataSaveResult
}

class ProductMetadataViewModelFactory(private val storeId: String) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] ?: error("Application is required")
        @Suppress("UNCHECKED_CAST") return ProductMetadataViewModel(application, storeId) as T
    }
}
