package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.ProductEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryReceiveScreen(
    product: ProductEntity,
    onBack: () -> Unit,
    onReceive: (Double, String?, Long?, Double) -> Unit,
    error: String? = null
) {
    var quantity by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf(String.format("%.2f", product.purchasePrice)) }

    Scaffold(topBar = { TopAppBar(title = { Text("RECEIVE STOCK", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(product.name, fontWeight = FontWeight.Bold)
            Text("Current stock: ${"%.2f".format(product.stock)} ${product.unit}")
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Received quantity") }
            )
            OutlinedTextField(
                value = batchNumber,
                onValueChange = { batchNumber = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Batch number (optional)") }
            )
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Expiry date YYYY-MM-DD (optional)") }
            )
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Purchase price per unit") }
            )
            error?.let { Text(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                Button(
                    onClick = {
                        val qty = quantity.toDoubleOrNull()
                        val price = purchasePrice.toDoubleOrNull()
                        val expiry = expiryDate.trim().takeIf { it.isNotBlank() }?.let { value ->
                            runCatching {
                                java.time.LocalDate.parse(value).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            }.getOrNull()
                        }
                        if (qty != null && price != null && (expiryDate.isBlank() || expiry != null)) {
                            onReceive(qty, batchNumber.trim().ifBlank { null }, expiry, price)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = quantity.toDoubleOrNull()?.let { it > 0 } == true && purchasePrice.toDoubleOrNull()?.let { it >= 0 } == true
                ) { Text("RECEIVE") }
            }
        }
    }
}
