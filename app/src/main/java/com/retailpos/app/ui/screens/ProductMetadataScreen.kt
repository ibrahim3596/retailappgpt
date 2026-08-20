package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retailpos.app.data.ProductMetadataSaveResult
import com.retailpos.app.data.ProductMetadataViewModel
import com.retailpos.app.data.ProductMetadataViewModelFactory
import com.retailpos.app.ui.components.ProductMetadataEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductMetadataScreen(
    storeId: String,
    productId: String,
    onBack: () -> Unit
) {
    val factory = remember(storeId) { ProductMetadataViewModelFactory(storeId) }
    val viewModel: ProductMetadataViewModel = viewModel(factory = factory)
    val form by viewModel.form.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(productId) { viewModel.load(productId) }

    Scaffold(topBar = { TopAppBar(title = { Text("PRODUCT DETAILS") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductMetadataEditor(form = form, onChange = viewModel::update)
            error?.let { Text(it) }
            Button(onClick = {
                viewModel.save(productId) { result ->
                    when (result) {
                        ProductMetadataSaveResult.Success -> onBack()
                        is ProductMetadataSaveResult.Invalid -> Unit
                        ProductMetadataSaveResult.Error -> Unit
                    }
                }
            }) {
                Text("SAVE PRODUCT DETAILS")
            }
        }
    }
}
