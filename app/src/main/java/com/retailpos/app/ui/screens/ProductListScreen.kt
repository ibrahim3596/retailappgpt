package com.retailpos.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.retailpos.app.core.products.ProductListFilter
import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.ProductViewModel
import com.retailpos.app.data.ProductViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    storeId: String,
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onIntelligentCapture: () -> Unit,
    onEditProduct: (String) -> Unit,
    onEditDetails: (String) -> Unit = {}
) {
    val factory = remember(storeId) { ProductViewModelFactory(storeId) }
    val viewModel: ProductViewModel = viewModel(factory = factory)
    val products by viewModel.products.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()

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
            Button(onClick = onIntelligentCapture, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("INTELLIGENT PRODUCT CAPTURE")
            }
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search name, SKU, barcode or brand") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ProductFilterButton("ALL", ProductListFilter.ALL, filter, viewModel::setFilter, Modifier.weight(1f))
                ProductFilterButton("LOW", ProductListFilter.LOW_STOCK, filter, viewModel::setFilter, Modifier.weight(1f))
                ProductFilterButton("OUT", ProductListFilter.OUT_OF_STOCK, filter, viewModel::setFilter, Modifier.weight(1f))
            }
            if (products.isEmpty()) {
                val message = if (query.isBlank()) "No products match this filter." else "No products match \"$query\"."
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products, key = { it.id }) { product -> ProductRow(product, onClick = { onEditProduct(product.id) }, onEditDetails = { onEditDetails(product.id) }) }
                }
            }
        }
    }
}

@Composable
private fun ProductFilterButton(
    label: String,
    value: ProductListFilter,
    selected: ProductListFilter,
    onSelect: (ProductListFilter) -> Unit,
    modifier: Modifier
) {
    if (selected == value) Button(onClick = { onSelect(value) }, modifier = modifier) { Text(label) }
    else OutlinedButton(onClick = { onSelect(value) }, modifier = modifier) { Text(label) }
}

@Composable
private fun ProductRow(product: ProductEntity, onClick: () -> Unit, onEditDetails: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (product.brand.isNotBlank()) Text(product.brand, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("₹${"%.2f".format(product.sellingPrice)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Stock ${"%.2f".format(product.stock)} ${product.unit}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            product.sku?.takeIf { it.isNotBlank() }?.let { Text("SKU $it", style = MaterialTheme.typography.labelMedium) }
            product.barcode?.takeIf { it.isNotBlank() }?.let { Text("Barcode $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButtonLike(onClick = onEditDetails)
        }
    }
}

@Composable
private fun TextButtonLike(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("EDIT PRODUCT DETAILS") }
}
