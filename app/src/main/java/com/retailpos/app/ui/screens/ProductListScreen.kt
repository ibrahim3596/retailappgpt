package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.ProductViewModel
import com.retailpos.app.data.ProductViewModelFactory

@Composable
fun ProductListScreen(
    storeId: String,
    onBack: () -> Unit,
    onAddProduct: () -> Unit
) {
    val factory = remember(storeId) { ProductViewModelFactory(storeId) }
    val viewModel: ProductViewModel = viewModel(factory = factory)
    val products by viewModel.products.collectAsState()
    var queryState = viewModel.query

    Scaffold(
        topBar = { TopAppBar(title = { Text("PRODUCTS", fontWeight = FontWeight.Black) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = queryState.value,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search name, brand, barcode or SKU") }
            )

            if (products.isEmpty()) {
                Text(
                    "No products yet. Add your first product to start billing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(product)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(product: ProductEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (product.brand.isNotBlank()) {
                Text(product.brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("₹${"%.2f".format(product.sellingPrice)}  •  Stock ${"%.2f".format(product.stock)} ${product.unit}")
            product.sku?.takeIf { it.isNotBlank() }?.let { Text("SKU $it", style = MaterialTheme.typography.labelMedium) }
        }
    }
}
