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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.retailpos.app.ui.components.SectionHeader
import com.retailpos.app.ui.components.StatusPill

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
                title = {
                    Column {
                        Text("Products", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Catalog · pricing · stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } },
                actions = { IconButton(onClick = onIntelligentCapture) { Icon(Icons.Default.CameraAlt, contentDescription = "Scan or capture product") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search name, SKU, barcode or brand") }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ProductFilterButton("All", ProductListFilter.ALL, filter, viewModel::setFilter, Modifier.weight(1f))
                    ProductFilterButton("Low", ProductListFilter.LOW_STOCK, filter, viewModel::setFilter, Modifier.weight(1f))
                    ProductFilterButton("Out", ProductListFilter.OUT_OF_STOCK, filter, viewModel::setFilter, Modifier.weight(1f))
                    ProductFilterButton("Archived", ProductListFilter.ARCHIVED, filter, viewModel::setFilter, Modifier.weight(1.25f))
                }
            }

            when {
                products.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 58.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(17.dp)) }
                        Text("No matching products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                filter == ProductListFilter.ARCHIVED && query.isBlank() -> "Archived products will appear here."
                                query.isBlank() -> "Try another stock filter or add a product."
                                else -> "Nothing matches \"$query\"."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (query.isNotBlank()) TextButton(onClick = { viewModel.setQuery("") }) { Text("Clear search") }
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "${products.size} products",
                            action = "Capture"
                        , onAction = onIntelligentCapture)
                    }
                    items(products, key = { it.id }) { product ->
                        ProductRow(
                            product = product,
                            onClick = { onEditProduct(product.id) },
                            onEditDetails = { onEditDetails(product.id) },
                            onArchive = { viewModel.archiveProduct(product.id, !product.isArchived) }
                        )
                    }
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
    if (selected == value) {
        Button(onClick = { onSelect(value) }, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label, maxLines = 1) }
    } else {
        OutlinedButton(onClick = { onSelect(value) }, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label, maxLines = 1) }
    }
}

@Composable
private fun ProductRow(
    product: ProductEntity,
    onClick: () -> Unit,
    onEditDetails: () -> Unit,
    onArchive: () -> Unit
) {
    val stockState = when {
        product.isArchived -> "Archived"
        product.stock <= 0.0 -> "Out of stock"
        product.stock <= product.lowStockThreshold -> "Low stock"
        else -> "Healthy"
    }
    val positive = stockState == "Healthy"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(product.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(product.brand.takeIf { it.isNotBlank() }, product.sku?.takeIf { it.isNotBlank() }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(stockState, positive)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("₹${"%.2f".format(product.sellingPrice)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${"%.2f".format(product.stock)} ${product.unit} in stock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    product.barcode?.takeIf { it.isNotBlank() }?.let { Text("Barcode · $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEditDetails) { Icon(Icons.Default.MoreHoriz, contentDescription = "Product details") }
                    IconButton(onClick = onArchive) { Icon(Icons.Default.StarBorder, contentDescription = if (product.isArchived) "Restore product" else "Archive product") }
                }
            }
        }
    }
}
