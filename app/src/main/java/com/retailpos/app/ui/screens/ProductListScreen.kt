package com.retailpos.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        topBar = {
            TopAppBar(
                title = { Column { Text("Products", fontWeight = FontWeight.SemiBold); Text("Catalog", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } },
                actions = { IconButton(onClick = onAddProduct) { Icon(Icons.Default.Add, contentDescription = "Add product") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onIntelligentCapture, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                Text("AI", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search name, SKU, barcode") }
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ProductFilterButton("All", ProductListFilter.ALL, filter, viewModel::setFilter, Modifier.weight(1f))
                ProductFilterButton("Low", ProductListFilter.LOW_STOCK, filter, viewModel::setFilter, Modifier.weight(1f))
                ProductFilterButton("Out", ProductListFilter.OUT_OF_STOCK, filter, viewModel::setFilter, Modifier.weight(1f))
                ProductFilterButton("Archived", ProductListFilter.ARCHIVED, filter, viewModel::setFilter, Modifier.weight(1.25f))
            }
            if (filter != ProductListFilter.ALL && query.isBlank()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when (filter) {
                            ProductListFilter.LOW_STOCK -> "Showing products at or below their reorder level."
                            ProductListFilter.OUT_OF_STOCK -> "Showing products with no sellable stock."
                            ProductListFilter.ARCHIVED -> "Archived products are excluded from normal billing."
                            else -> ""
                        },
                        Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (products.isEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text(if (query.isBlank()) "Nothing here yet" else "No products found", fontWeight = FontWeight.SemiBold)
                        Text(if (query.isBlank()) "Add a product or change the filter." else "Try another name, SKU or barcode.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (query.isBlank()) OutlinedButton(onClick = onAddProduct) { Text("ADD PRODUCT") }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(top = 2.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(product, { onEditProduct(product.id) }, { onEditDetails(product.id) }, { viewModel.archiveProduct(product.id, !product.isArchived) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductFilterButton(label: String, value: ProductListFilter, selected: ProductListFilter, onSelect: (ProductListFilter) -> Unit, modifier: Modifier) {
    if (selected == value) Button(onClick = { onSelect(value) }, modifier = modifier, contentPadding = PaddingValues(horizontal = 7.dp)) { Text(label) }
    else OutlinedButton(onClick = { onSelect(value) }, modifier = modifier, contentPadding = PaddingValues(horizontal = 7.dp)) { Text(label) }
}

@Composable
private fun ProductRow(product: ProductEntity, onClick: () -> Unit, onEditDetails: () -> Unit, onArchive: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) { BoxLabel(product) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (product.brand.isNotBlank()) Text(product.brand, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val stockText = "${fmt(product.stock)} ${product.unit}"
                Text("${money(product.sellingPrice)}  ·  $stockText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (product.sku?.isNotBlank() == true) Text("SKU ${product.sku}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (product.isArchived) Text("Archived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                else if (product.stock <= 0.0) Text("Out of stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                else if (product.stock <= product.lowStockThreshold) Text("Low stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onEditDetails, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Details") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 7.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onArchive) { Text(if (product.isArchived) "Restore" else "Archive") }
        }
    }
}

@Composable
private fun BoxLabel(product: ProductEntity) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(product.name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
private fun money(value: Double): String = String.format(java.util.Locale.US, "₹%.2f", value)
private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.2f", value)
