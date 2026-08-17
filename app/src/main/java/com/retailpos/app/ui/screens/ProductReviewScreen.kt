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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.retailpos.app.data.ProductViewModel
import com.retailpos.app.data.ProductViewModelFactory
import com.retailpos.app.data.SaveProductResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewScreen(
    storeId: String,
    productId: String?,
    onBack: () -> Unit
) {
    val factory = remember(storeId) { ProductViewModelFactory(storeId) }
    val viewModel: ProductViewModel = viewModel(factory = factory)
    val editingProduct by viewModel.editingProduct.collectAsState()

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    LaunchedEffect(editingProduct?.id) {
        editingProduct?.let { product ->
            name = product.name
            brand = product.brand
            barcode = product.barcode.orEmpty()
            sku = product.sku.orEmpty()
            mrp = product.mrp.toString()
            sellingPrice = product.sellingPrice.toString()
            purchasePrice = product.purchasePrice.toString()
            stock = product.stock.toString()
            unit = product.unit
            errorMessage = null
        }
    }

    val isEdit = productId != null

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEdit) "EDIT PRODUCT" else "ADD PRODUCT", fontWeight = FontWeight.Black) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isEdit) "Update product details" else "Product details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(name, { name = it; errorMessage = null }, Modifier.fillMaxWidth(), label = { Text("Product name") }, singleLine = true)
            OutlinedTextField(brand, { brand = it }, Modifier.fillMaxWidth(), label = { Text("Brand") }, singleLine = true)
            OutlinedTextField(barcode, { barcode = it; errorMessage = null }, Modifier.fillMaxWidth(), label = { Text("Barcode / GTIN") }, singleLine = true)
            OutlinedTextField(sku, { sku = it; errorMessage = null }, Modifier.fillMaxWidth(), label = { Text("SKU / Item code") }, singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(mrp, { mrp = it }, Modifier.weight(1f), label = { Text("MRP") }, singleLine = true)
                OutlinedTextField(sellingPrice, { sellingPrice = it }, Modifier.weight(1f), label = { Text("Sale price") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(purchasePrice, { purchasePrice = it }, Modifier.weight(1f), label = { Text("Purchase price") }, singleLine = true)
                OutlinedTextField(
                    value = stock,
                    onValueChange = { if (!isEdit) stock = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(if (isEdit) "Current stock" else "Opening stock") },
                    singleLine = true,
                    enabled = !isEdit
                )
            }
            OutlinedTextField(unit, { unit = it }, Modifier.fillMaxWidth(), label = { Text("Unit") }, singleLine = true)

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    viewModel.saveProduct(
                        productId = productId,
                        name = name,
                        brand = brand,
                        barcode = barcode,
                        sku = sku,
                        mrp = mrp.toDoubleOrNull() ?: -1.0,
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: -1.0,
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: -1.0,
                        stock = stock.toDoubleOrNull() ?: -1.0,
                        unit = unit,
                        onResult = { result ->
                            when (result) {
                                SaveProductResult.Success -> onBack()
                                SaveProductResult.DuplicateSku -> errorMessage = "That SKU is already used by another product in this store."
                                SaveProductResult.DuplicateBarcode -> errorMessage = "That barcode is already assigned to another product in this store."
                                SaveProductResult.InvalidInput -> errorMessage = "Enter a product name and valid non-negative numeric values."
                                SaveProductResult.Error -> errorMessage = "Unable to save the product. Please try again."
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(if (isEdit) "SAVE CHANGES" else "SAVE PRODUCT", fontWeight = FontWeight.Bold)
            }
        }
    }
}
