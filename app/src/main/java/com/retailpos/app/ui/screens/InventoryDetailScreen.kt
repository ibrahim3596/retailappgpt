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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.InventoryBatchEntity
import com.retailpos.app.data.InventoryMovementEntity
import com.retailpos.app.data.ProductEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    product: ProductEntity,
    batchesLoader: suspend () -> List<InventoryBatchEntity>,
    movementsLoader: suspend () -> List<InventoryMovementEntity>,
    onBack: () -> Unit,
    onAdjust: () -> Unit,
    onReceive: () -> Unit
) {
    var batches by remember { mutableStateOf<List<InventoryBatchEntity>>(emptyList()) }
    var movements by remember { mutableStateOf<List<InventoryMovementEntity>>(emptyList()) }
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(product.id) {
        batches = batchesLoader()
        movements = movementsLoader()
    }

    val activeBatches = batches.filter { it.quantity > 0 }
    val totalBatchUnits = activeBatches.sumOf { it.quantity }

    Scaffold(
        topBar = { TopAppBar(title = { Text("INVENTORY DETAIL", fontWeight = FontWeight.Black) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        if (product.brand.isNotBlank()) Text(product.brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Current stock: ${"%.2f".format(Locale.getDefault(), product.stock)} ${product.unit}", fontWeight = FontWeight.Bold)
                        Text("Batch stock: ${"%.2f".format(Locale.getDefault(), totalBatchUnits)} ${product.unit}")
                        product.sku?.takeIf { it.isNotBlank() }?.let { Text("SKU $it") }
                    }
                }
            }

            item { Text("Batches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            if (activeBatches.isEmpty()) {
                item { Text("No active batches recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(activeBatches, key = { it.id }) { batch ->
                    val expired = batch.expiryDate?.let { it < System.currentTimeMillis() } == true
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(batch.batchNumber?.takeIf { it.isNotBlank() } ?: "UNBATCHED", fontWeight = FontWeight.Bold)
                                Text(
                                    if (expired) "EXPIRED" else "AVAILABLE",
                                    color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text("Quantity: ${"%.2f".format(Locale.getDefault(), batch.quantity)} ${product.unit}")
                            Text("Purchase price: ${"%.2f".format(Locale.getDefault(), batch.purchasePrice)} / ${product.unit}")
                            Text("Expiry: ${batch.expiryDate?.let { dateFormatter.format(Date(it)) } ?: "No expiry"}")
                            Text("Received: ${formatter.format(Date(batch.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Text("Movement history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp)) }
            if (movements.isEmpty()) {
                item { Text("No movements recorded for this product.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(movements.take(50), key = { it.id }) { movement ->
                    val sign = if (movement.quantityDelta >= 0) "+" else ""
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(movement.reason.replace('_', ' '), fontWeight = FontWeight.Bold)
                            Text("$sign${"%.2f".format(Locale.getDefault(), movement.quantityDelta)} ${product.unit}")
                            movement.batchId?.let { Text("Batch $it", style = MaterialTheme.typography.labelSmall) }
                            movement.referenceId?.let { Text("Ref $it", style = MaterialTheme.typography.labelSmall) }
                            Text(formatter.format(Date(movement.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReceive, modifier = Modifier.weight(1f)) { Text("RECEIVE") }
                    OutlinedButton(onClick = onAdjust, modifier = Modifier.weight(1f)) { Text("ADJUST") }
                }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }
}
