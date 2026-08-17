package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.retailpos.app.data.InventoryMovementEntity
import com.retailpos.app.data.ProductRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    storeId: String,
    repository: ProductRepository,
    inventoryMovements: suspend () -> List<InventoryMovementEntity>,
    onBack: () -> Unit,
    onAdjustProduct: (String) -> Unit
) {
    val products by repository.observeProducts(storeId).collectAsState(initial = emptyList())
    var movements by remember { mutableStateOf<List<InventoryMovementEntity>>(emptyList()) }

    LaunchedEffect(storeId) {
        movements = inventoryMovements()
    }

    val lowStock = products.count { it.stock <= it.lowStockThreshold }
    val totalUnits = products.sumOf { it.stock }
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("INVENTORY", fontWeight = FontWeight.Black) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InventoryStat("PRODUCTS", products.size.toString(), Modifier.weight(1f))
                    InventoryStat("UNITS", String.format(Locale.getDefault(), "%.0f", totalUnits), Modifier.weight(1f))
                    InventoryStat("LOW STOCK", lowStock.toString(), Modifier.weight(1f))
                }
            }
            item { Text("Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            if (products.isEmpty()) {
                item { Text("No products yet. Add products before managing inventory.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(products, key = { it.id }) { product ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (product.brand.isNotBlank()) Text(product.brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${String.format(Locale.getDefault(), "%.2f", product.stock)} ${product.unit}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                            if (product.stock <= product.lowStockThreshold) {
                                Text("LOW STOCK • threshold ${product.lowStockThreshold}", color = MaterialTheme.colorScheme.error)
                            }
                            product.sku?.takeIf { it.isNotBlank() }?.let { Text("SKU $it", style = MaterialTheme.typography.labelMedium) }
                            OutlinedButton(onClick = { onAdjustProduct(product.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("ADJUST STOCK")
                            }
                        }
                    }
                }
            }
            item {
                Text("Recent movements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
            }
            if (movements.isEmpty()) {
                item { Text("No inventory movements recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(movements.take(20), key = { it.id }) { movement ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val sign = if (movement.quantityDelta >= 0) "+" else ""
                            Text(movement.reason.replace('_', ' '), fontWeight = FontWeight.Bold)
                            Text("$sign${String.format(Locale.getDefault(), "%.2f", movement.quantityDelta)} units")
                            Text(formatter.format(Date(movement.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            movement.referenceId?.let { Text("Ref $it", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }
}

@Composable
private fun InventoryStat(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}
