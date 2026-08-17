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
fun InventoryAdjustmentScreen(
    product: ProductEntity,
    onBack: () -> Unit,
    onAdjust: (Double, String) -> Unit,
    error: String? = null
) {
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("ADJUSTMENT") }

    Scaffold(topBar = { TopAppBar(title = { Text("ADJUST STOCK", fontWeight = FontWeight.Black) }) }) { padding ->
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
                label = { Text("Quantity change (+/-)") }
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it.uppercase() },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Reason") }
            )
            error?.let { Text(it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                Button(
                    onClick = { quantity.toDoubleOrNull()?.let { onAdjust(it, reason) } },
                    modifier = Modifier.weight(1f),
                    enabled = quantity.toDoubleOrNull()?.let { it != 0.0 } == true
                ) { Text("SAVE") }
            }
        }
    }
}
