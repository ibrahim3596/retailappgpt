package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retailpos.app.data.BarcodeMutationResult
import com.retailpos.app.data.ProductViewModel
import com.retailpos.app.data.ProductViewModelFactory
import com.retailpos.app.data.SaveProductResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewScreen(storeId: String, productId: String?, initialBarcode: String = "", onBack: () -> Unit) {
    val factory = remember(storeId) { ProductViewModelFactory(storeId) }
    val viewModel: ProductViewModel = viewModel(factory = factory)
    val editingProduct by viewModel.editingProduct.collectAsState()
    val barcodes by viewModel.barcodes.collectAsState()
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(initialBarcode) }
    var sku by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var lowStockThreshold by remember { mutableStateOf("5") }
    var secondaryBarcode by remember { mutableStateOf("") }
    var secondaryBarcodeError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var captureHint by remember { mutableStateOf<String?>(null) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showIntelligentCapture by remember { mutableStateOf(false) }

    LaunchedEffect(productId, initialBarcode) {
        viewModel.loadProduct(productId)
        if (productId == null && initialBarcode.isNotBlank()) barcode = initialBarcode.trim()
    }
    LaunchedEffect(editingProduct?.id) {
        editingProduct?.let { product ->
            name = product.name; brand = product.brand; barcode = product.barcode.orEmpty(); sku = product.sku.orEmpty()
            mrp = product.mrp.toString(); sellingPrice = product.sellingPrice.toString(); purchasePrice = product.purchasePrice.toString()
            stock = product.stock.toString(); unit = product.unit; lowStockThreshold = product.lowStockThreshold.toString(); errorMessage = null
        }
    }
    val isEdit = productId != null
    if (showBarcodeScanner) {
        BarcodeScannerScreen("SCAN PRODUCT BARCODE", { showBarcodeScanner = false }) { raw, _ -> barcode = raw.trim(); errorMessage = null; showBarcodeScanner = false }
        return
    }
    if (showIntelligentCapture) {
        IntelligentProductCaptureScreen(
            onBack = { showIntelligentCapture = false },
            onResult = { result ->
                result.barcode?.let { barcode = it }
                result.detectedName?.let { name = it }
                result.detectedBrand?.let { brand = it }
                result.detectedMrp?.let { mrp = it.toString() }
                captureHint = result.categoryHint?.let { hint ->
                    val confidence = result.labelConfidence?.let { String.format(java.util.Locale.US, "%.0f%%", it * 100f) } ?: ""
                    "Visual hint: $hint ${if (confidence.isNotBlank()) "($confidence)" else ""}. Verify before saving."
                }
                errorMessage = null
                showIntelligentCapture = false
            }
        )
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text(if (isEdit) "EDIT PRODUCT" else "ADD PRODUCT", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isEdit) "Update product details" else "Product details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!isEdit) {
                Button(onClick = { showIntelligentCapture = true }, modifier = Modifier.fillMaxWidth()) { Text("INTELLIGENTLY IDENTIFY PRODUCT", fontWeight = FontWeight.Bold) }
                Text("Point the camera at the product. The app combines barcode, printed text and visual category signals, then pre-fills what it can identify.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(name, { name = it; errorMessage = null }, Modifier.fillMaxWidth(), label = { Text("Product name") }, singleLine = true)
            OutlinedTextField(brand, { brand = it }, Modifier.fillMaxWidth(), label = { Text("Brand") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(barcode, { barcode = it; errorMessage = null }, Modifier.weight(1f), label = { Text("Primary barcode / GTIN") }, singleLine = true)
                OutlinedButton(onClick = { showBarcodeScanner = true }, modifier = Modifier.padding(top = 8.dp)) { Text("SCAN") }
            }
            OutlinedTextField(sku, { sku = it; errorMessage = null }, Modifier.fillMaxWidth(), label = { Text("SKU / Item code") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(mrp, { mrp = it }, Modifier.weight(1f), label = { Text("MRP") }, singleLine = true)
                OutlinedTextField(sellingPrice, { sellingPrice = it }, Modifier.weight(1f), label = { Text("Sale price") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(purchasePrice, { purchasePrice = it }, Modifier.weight(1f), label = { Text("Purchase price") }, singleLine = true)
                OutlinedTextField(value = stock, onValueChange = { if (!isEdit) stock = it }, modifier = Modifier.weight(1f), label = { Text(if (isEdit) "Current stock" else "Opening stock") }, singleLine = true, enabled = !isEdit)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(unit, { unit = it }, Modifier.weight(1f), label = { Text("Unit") }, singleLine = true)
                OutlinedTextField(lowStockThreshold, { lowStockThreshold = it }, Modifier.weight(1f), label = { Text("Low-stock alert") }, singleLine = true)
            }
            captureHint?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            Text("ADDITIONAL BAR CODES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            if (productId == null) {
                Text("Save the product first, then add additional barcodes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                barcodes.filter { !it.isPrimary }.forEach { code ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(code.value, fontWeight = FontWeight.Bold); Text(code.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        TextButton(onClick = { viewModel.removeSecondaryBarcode(code.id) }) { Text("REMOVE") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(secondaryBarcode, { secondaryBarcode = it; secondaryBarcodeError = null }, Modifier.weight(1f), label = { Text("Secondary barcode") }, singleLine = true)
                    OutlinedButton(onClick = {
                        viewModel.addSecondaryBarcode(productId, secondaryBarcode, "UNKNOWN") { result ->
                            when (result) {
                                BarcodeMutationResult.Success -> { secondaryBarcode = ""; secondaryBarcodeError = null }
                                BarcodeMutationResult.Duplicate -> secondaryBarcodeError = "That barcode is already assigned to a product in this store."
                                BarcodeMutationResult.Invalid -> secondaryBarcodeError = "Enter a barcode before adding it."
                            }
                        }
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("ADD") }
                }
                secondaryBarcodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Text("Sale price cannot exceed MRP. Stock is managed separately after product creation. QR codes are not accepted as product identifiers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                val mrpValue = mrp.toDoubleOrNull() ?: -1.0
                val saleValue = sellingPrice.toDoubleOrNull() ?: -1.0
                val purchaseValue = purchasePrice.toDoubleOrNull() ?: -1.0
                val stockValue = stock.toDoubleOrNull() ?: -1.0
                val thresholdValue = lowStockThreshold.toDoubleOrNull() ?: -1.0
                viewModel.saveProduct(productId, name, brand, barcode, sku, mrpValue, saleValue, purchaseValue, stockValue, unit, thresholdValue) { result ->
                    when (result) {
                        SaveProductResult.Success -> onBack()
                        SaveProductResult.DuplicateSku -> errorMessage = "That SKU is already used by another product in this store."
                        SaveProductResult.DuplicateBarcode -> errorMessage = "That barcode is already assigned to another product in this store."
                        SaveProductResult.InvalidInput -> errorMessage = "Check the product name and numbers. Sale price must not exceed MRP, and stock/threshold cannot be negative."
                        SaveProductResult.Error -> errorMessage = "Unable to save the product. Please try again."
                    }
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentPadding = PaddingValues(vertical = 16.dp)) { Text(if (isEdit) "SAVE CHANGES" else "SAVE PRODUCT", fontWeight = FontWeight.Bold) }
        }
    }
}
