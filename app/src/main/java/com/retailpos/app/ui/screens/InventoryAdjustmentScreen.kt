package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.retailpos.app.data.ProductEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryAdjustmentScreen(
    product: ProductEntity,
    batchesLoader: suspend () -> List<InventoryBatchEntity>,
    onBack: () -> Unit,
    onAdjust: (Double, String) -> Unit,
    onAdjustBatch: (String, Double, String) -> Unit,
    error: String? = null
) {
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("ADJUSTMENT") }
    var selectedBatchId by remember { mutableStateOf<String?>(null) }
    var batches by remember { mutableStateOf<List<InventoryBatchEntity>>(emptyList()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(product.id) {
        batches = batchesLoader()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("ADJUST STOCK", fontWeight = FontWeight.Black) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text("Current stock: ${"%.2f".format(Locale.getDefault(), product.stock)} ${product.unit}")
            }
            if (batches.isNotEmpty()) {
                item { Text("Select batch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(batches, key = { it.id }) { batch ->
                    val selected = selectedBatchId == batch.id
                    val expiry = batch.expiryDate?.let { dateFormatter.format(Date(it)) } ?: "No expiry"
                    Card(onClick = { selectedBatchId = if (selected) null else batch.id }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(batch.batchNumber?.takeIf { it.isNotBlank() } ?: "UNBATCHED", fontWeight = FontWeight.Bold)
                            Text("Available: ${"%.2f".format(Locale.getDefault(), batch.quantity)} ${product.unit}")
                            Text("Expiry: $expiry", style = MaterialTheme.typography.labelMedium)
                            Text(if (selected) "SELECTED" else "Tap to select", color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Quantity change (+/-)") }
                )
            }
            item {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.uppercase() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Reason") }
                )
            }
            item {
                Text(
                    if (selectedBatchId == null && batches.isNotEmpty()) "No batch selected: adjustment applies to aggregate stock only." else "Selected batch adjustment updates both batch and product stock.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                    Button(
                        onClick = {
                            quantity.toDoubleOrNull()?.let { value ->
                                selectedBatchId?.let { batchId -> onAdjustBatch(batchId, value, reason) }
                                    ?: onAdjust(value, reason)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = quantity.toDoubleOrNull()?.let { it != 0.0 } == true
                    ) { Text("SAVE") }
                }
            }
        }
    }
}
