package com.retailpos.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
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

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setImageUri(it.toString()) }
    }

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
            Text("PRODUCT IMAGE")
            if (form.imageUri.isNullOrBlank()) {
                Text("No product image selected.")
            } else {
                Text("Image selected")
                Text(form.imageUri!!)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Text(if (form.imageUri.isNullOrBlank()) "ADD IMAGE" else "CHANGE IMAGE")
                }
                if (!form.imageUri.isNullOrBlank()) {
                    OutlinedButton(onClick = { viewModel.setImageUri(null) }, modifier = Modifier.weight(1f)) {
                        Text("REMOVE")
                    }
                }
            }

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
