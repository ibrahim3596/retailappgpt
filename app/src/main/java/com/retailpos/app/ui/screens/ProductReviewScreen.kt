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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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

@Composable
fun ProductReviewScreen(
    storeId: String,
    onBack: () -> Unit
) {
    val factory = remember(storeId) { ProductViewModelFactory(storeId) }
    val viewModel: ProductViewModel = viewModel(factory = factory)

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ADD PRODUCT", fontWeight = FontWeight.Black) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Product details", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Product name") }, singleLine = true)
            OutlinedTextField(brand, { brand = it }, Modifier.fillMaxWidth(), label = { Text("Brand") }, singleLine = true)
            OutlinedTextField(barcode, { barcode = it }, Modifier.fillMaxWidth(), label = { Text("Barcode / GTIN") }, singleLine = true)
            OutlinedTextField(sku, { sku = it }, Modifier.fillMaxWidth(), label = { Text("SKU / Item code") }, singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(mrp, { mrp = it }, Modifier.weight(1f), label = { Text("MRP") }, singleLine = true)
                OutlinedTextField(sellingPrice, { sellingPrice = it }, Modifier.weight(1f), label = { Text("Sale price") }, singleLine = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(purchasePrice, { purchasePrice = it }, Modifier.weight(1f), label = { Text("Purchase price") }, singleLine = true)
                OutlinedTextField(stock, { stock = it }, Modifier.weight(1f), label = { Text("Opening stock") }, singleLine = true)
            }
            OutlinedTextField(unit, { unit = it }, Modifier.fillMaxWidth(), label = { Text("Unit") }, singleLine = true)

            Button(
                onClick = {
                    viewModel.saveProduct(
                        name = name,
                        brand = brand,
                        barcode = barcode,
                        sku = sku,
                        mrp = mrp.toDoubleOrNull() ?: -1.0,
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: -1.0,
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: -1.0,
                        stock = stock.toDoubleOrNull() ?: 0.0,
                        unit = unit,
                        onSaved = onBack
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("SAVE PRODUCT", fontWeight = FontWeight.Bold)
            }
        }
    }
}
