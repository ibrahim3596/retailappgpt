package com.retailpos.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retailpos.app.data.BarcodeMutationResult
import com.retailpos.app.data.ProductMetadataSaveResult
import com.retailpos.app.data.ProductMetadataViewModel
import com.retailpos.app.data.ProductMetadataViewModelFactory
import com.retailpos.app.data.ProductViewModel
import com.retailpos.app.data.ProductViewModelFactory
import com.retailpos.app.ui.components.ProductMetadataEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductMetadataScreen(
    storeId: String,
    productId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val metadataFactory = remember(storeId) { ProductMetadataViewModelFactory(storeId) }
    val metadataViewModel: ProductMetadataViewModel = viewModel(factory = metadataFactory)
    val productFactory = remember(storeId) { ProductViewModelFactory(storeId) }
    val productViewModel: ProductViewModel = viewModel(key = "product-$productId", factory = productFactory)
    val form by metadataViewModel.form.collectAsState()
    val error by metadataViewModel.error.collectAsState()
    val barcodes by productViewModel.barcodes.collectAsState()
    var secondaryBarcode by remember { mutableStateOf("") }
    var barcodeMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            metadataViewModel.setImageUri(it.toString())
        }
    }

    val imageBitmap by produceState<Bitmap?>(initialValue = null, form.imageUri) {
        value = withContext(Dispatchers.IO) {
            form.imageUri?.let { uriString ->
                runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(uriString))?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }.getOrNull()
            }
        }
    }

    LaunchedEffect(productId) {
        metadataViewModel.load(productId)
        productViewModel.loadProduct(productId)
    }

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
            imageBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Product image",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = ContentScale.Fit
                )
            }
            if (form.imageUri.isNullOrBlank()) Text("No product image selected.")
            else if (imageBitmap == null) Text("Image selected but preview is unavailable on this device.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { imagePicker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }, modifier = Modifier.weight(1f)) {
                    Text(if (form.imageUri.isNullOrBlank()) "ADD IMAGE" else "CHANGE IMAGE")
                }
                if (!form.imageUri.isNullOrBlank()) {
                    OutlinedButton(onClick = { metadataViewModel.setImageUri(null) }, modifier = Modifier.weight(1f)) {
                        Text("REMOVE")
                    }
                }
            }

            Text("BARCODES")
            if (barcodes.isEmpty()) Text("No barcode records yet.")
            barcodes.forEach { code ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(code.value)
                        Text(if (code.isPrimary) "PRIMARY • ${code.type}" else "ALTERNATE • ${code.type}")
                    }
                    if (!code.isPrimary) {
                        OutlinedButton(onClick = { productViewModel.removeSecondaryBarcode(code.id) }) { Text("REMOVE") }
                    }
                }
            }
            OutlinedTextField(
                value = secondaryBarcode,
                onValueChange = { secondaryBarcode = it; barcodeMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Add alternate barcode") },
                singleLine = true
            )
            Button(onClick = {
                productViewModel.addSecondaryBarcode(productId, secondaryBarcode, "UNKNOWN") { result ->
                    barcodeMessage = when (result) {
                        BarcodeMutationResult.Success -> { secondaryBarcode = ""; "Alternate barcode added." }
                        BarcodeMutationResult.Duplicate -> "That barcode is already assigned."
                        BarcodeMutationResult.Invalid -> "Enter a valid retail barcode."
                    }
                }
            }, enabled = secondaryBarcode.isNotBlank()) { Text("ADD BARCODE") }
            barcodeMessage?.let { Text(it) }

            ProductMetadataEditor(form = form, onChange = metadataViewModel::update)
            error?.let { Text(it) }
            Button(onClick = {
                metadataViewModel.save(productId) { result ->
                    when (result) {
                        ProductMetadataSaveResult.Success -> onBack()
                        is ProductMetadataSaveResult.Invalid -> Unit
                        ProductMetadataSaveResult.Error -> Unit
                    }
                }
            }) { Text("SAVE PRODUCT DETAILS") }
        }
    }
}
