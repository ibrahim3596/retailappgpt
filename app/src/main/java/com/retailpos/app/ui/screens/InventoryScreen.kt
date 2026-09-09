package com.retailpos.app.ui.screens

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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.InventoryMovementEntity
import com.retailpos.app.data.ProductRepository
import com.retailpos.app.ui.components.AiInsight
import com.retailpos.app.ui.components.SectionHeader
import com.retailpos.app.ui.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class StockFilter { ALL, LOW, OUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    storeId: String,
    repository: ProductRepository,
    inventoryMovements: suspend () -> List<InventoryMovementEntity>,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onAdjustProduct: (String) -> Unit,
    onReceiveProduct: (String) -> Unit
) {
    val products by repository.observeProducts(storeId).collectAsState(initial = emptyList())
    var movements by remember { mutableStateOf<List<InventoryMovementEntity>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(StockFilter.ALL) }

    LaunchedEffect(storeId) { movements = inventoryMovements() }

    val visibleProducts = products.filter { product ->
        val q = query.trim()
        val matchesQuery = q.isBlank() || listOfNotNull(product.name, product.brand, product.sku, product.barcode)
            .any { it.contains(q, ignoreCase = true) }
        val matchesFilter = when (filter) {
            StockFilter.ALL -> true
            StockFilter.LOW -> product.stock > 0.0 && product.stock <= product.lowStockThreshold
            StockFilter.OUT -> product.stock <= 0.0
        }
        matchesQuery && matchesFilter
    }

    val lowStock = products.count { it.stock > 0.0 && it.stock <= it.lowStockThreshold }
    val outOfStock = products.count { it.stock <= 0.0 }
    val totalUnits = products.sumOf { it.stock.coerceAtLeast(0.0) }
    val stockValue = products.sumOf { it.purchasePrice * it.stock.coerceAtLeast(0.0) }
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Inventory · attention · movement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val needsAttention = lowStock + outOfStock > 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (needsAttention) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(Modifier.size(44.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface.copy(alpha = .75f)) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.padding(11.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (needsAttention) "${lowStock + outOfStock} items need attention" else "Stock looks healthy", fontWeight = FontWeight.SemiBold)
                            Text(if (needsAttention) "$lowStock low stock · $outOfStock out of stock" else "No low or out-of-stock products.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryMetric("Products", products.size.toString(), Modifier.weight(1f))
                    InventoryMetric("Units", compact(totalUnits), Modifier.weight(1f))
                    InventoryMetric("Stock value", "₹${money(stockValue)}", Modifier.weight(1.3f))
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search product, SKU or barcode") }
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StockFilterButton("All", StockFilter.ALL, filter, { filter = it }, Modifier.weight(1f))
                    StockFilterButton("Low $lowStock", StockFilter.LOW, filter, { filter = it }, Modifier.weight(1f))
                    StockFilterButton("Out $outOfStock", StockFilter.OUT, filter, { filter = it }, Modifier.weight(1f))
                }
            }
            item {
                AiInsight(
                    if (lowStock > 0) "$lowStock products are at or below their reorder threshold. Review the fastest-moving items first."
                    else "No products are currently below their reorder threshold.",
                    action = if (lowStock > 0) "Show low stock" else null,
                    onAction = { filter = StockFilter.LOW }
                )
            }
            item { SectionHeader("Products", "${visibleProducts.size}") }
            if (visibleProducts.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Nothing needs attention here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Try another search or stock filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Clear search") }
                    }
                }
            } else {
                items(visibleProducts, key = { it.id }) { product ->
                    val state = when {
                        product.stock <= 0.0 -> "Out of stock"
                        product.stock <= product.lowStockThreshold -> "Low stock"
                        else -> "Healthy"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenProduct(product.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(Modifier.size(42.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(product.name.take(1).uppercase(), modifier = Modifier.padding(11.dp), fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold)
                                    Text("${compact(product.stock)} ${product.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(state, state == "Healthy")
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Value · ₹${money(product.purchasePrice * product.stock.coerceAtLeast(0.0))}", style = MaterialTheme.typography.labelMedium)
                                    product.sku?.takeIf { it.isNotBlank() }?.let { Text("SKU · $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                OutlinedButton(onClick = { onReceiveProduct(product.id) }) { Text("Receive") }
                                Button(onClick = { onAdjustProduct(product.id) }) { Text("Adjust") }
                            }
                        }
                    }
                }
            }
            item { SectionHeader("Recent movements") }
            if (movements.isEmpty()) {
                item { Text("No inventory movements recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(movements.take(10), key = { it.id }) { movement ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(movement.reason.replace('_', ' '), fontWeight = FontWeight.Medium)
                            Text(formatter.format(Date(movement.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text((if (movement.quantityDelta >= 0) "+" else "") + compact(movement.quantityDelta), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockFilterButton(label: String, value: StockFilter, selected: StockFilter, onSelect: (StockFilter) -> Unit, modifier: Modifier) {
    if (value == selected) Button(onClick = { onSelect(value) }, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label) }
    else OutlinedButton(onClick = { onSelect(value) }, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label) }
}

@Composable
private fun InventoryMetric(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun money(value: Double) = String.format(Locale.US, "%.2f", value)
private fun compact(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
