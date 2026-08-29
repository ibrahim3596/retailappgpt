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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
fun InventoryScreen(storeId: String, repository: ProductRepository, inventoryMovements: suspend () -> List<InventoryMovementEntity>, onBack: () -> Unit, onOpenProduct: (String) -> Unit, onAdjustProduct: (String) -> Unit, onReceiveProduct: (String) -> Unit) {
    val products by repository.observeProducts(storeId).collectAsState(initial = emptyList())
    var movements by remember { mutableStateOf<List<InventoryMovementEntity>>(emptyList()) }
    LaunchedEffect(storeId) { movements = inventoryMovements() }
    val lowStock = products.count { it.stock in 0.0..it.lowStockThreshold }
    val outOfStock = products.count { it.stock <= 0.0 }
    val totalUnits = products.sumOf { it.stock }
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Scaffold(topBar = { TopAppBar(title = { Column { Text("Stock", fontWeight = FontWeight.SemiBold); Text("What needs attention", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Surface(color = if (lowStock > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (lowStock > 0) "$lowStock products need attention" else "Stock looks healthy", fontWeight = FontWeight.SemiBold)
                        Text("$outOfStock out of stock · ${lowStock - outOfStock.coerceAtMost(lowStock)} low", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SmallStat("Products", products.size.toString(), Modifier.weight(1f)); SmallStat("Units", fmt(totalUnits), Modifier.weight(1f)); SmallStat("Low", lowStock.toString(), Modifier.weight(1f)) } }
            item { Text("Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (products.isEmpty()) item { Text("No products yet. Add products before managing stock.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else items(products, key = { it.id }) { product ->
                val critical = product.stock <= 0.0
                val low = product.stock > 0.0 && product.stock <= product.lowStockThreshold
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); if (product.brand.isNotBlank()) Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("${fmt(product.stock)} ${product.unit}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(when { critical -> "OUT OF STOCK"; low -> "LOW STOCK · reorder at ${fmt(product.lowStockThreshold)}"; else -> "HEALTHY STOCK" }, color = when { critical -> MaterialTheme.colorScheme.error; low -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.primary }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        product.sku?.takeIf { it.isNotBlank() }?.let { Text("SKU $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onOpenProduct(product.id) }, modifier = Modifier.weight(1f)) { Text("DETAILS") }; OutlinedButton(onClick = { onReceiveProduct(product.id) }, modifier = Modifier.weight(1f)) { Text("RECEIVE") }; OutlinedButton(onClick = { onAdjustProduct(product.id) }, modifier = Modifier.weight(1f)) { Text("ADJUST") } }
                    }
                }
            }
            item { Text("Recent movements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
            if (movements.isEmpty()) item { Text("No movements recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else items(movements.take(15), key = { it.id }) { movement ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(movement.reason.replace('_', ' '), fontWeight = FontWeight.Medium); Text(formatter.format(Date(movement.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(if (movement.quantityDelta >= 0) "+${fmt(movement.quantityDelta)}" else fmt(movement.quantityDelta), fontWeight = FontWeight.SemiBold) }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }
}

@Composable private fun SmallStat(label: String, value: String, modifier: Modifier) { Card(modifier) { Column(Modifier.padding(10.dp)) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } } }
private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
