package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.data.ReturnCandidateLine
import com.retailpos.app.data.ReturnRepository
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.SaleEntity
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ReturnScreen(
    storeId: String,
    database: RetailDatabase,
    staffRole: StaffRole,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sales by remember { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var selectedSale by remember { mutableStateOf<SaleEntity?>(null) }
    var lines by remember { mutableStateOf<List<ReturnCandidateLine>>(emptyList()) }
    val quantities = remember { mutableStateMapOf<String, String>() }
    var refundMethod by remember { mutableStateOf("CASH") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { sales = database.saleDao().getRecentSales(storeId, 50) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("RETURNS / REFUNDS", fontWeight = FontWeight.Black) }, navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } })
    }) { padding ->
        if (selectedSale == null) {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Select a recent sale", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
                if (sales.isEmpty()) item { Text("No sales available.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(sales, key = { it.id }) { sale ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Sale ${sale.id.take(8)}", fontWeight = FontWeight.Bold)
                            Text("${sale.paymentMethod} • ₹${money(sale.total)}")
                            TextButton(onClick = {
                                scope.launch {
                                    val saleLines = database.saleDao().getSaleLines(sale.id)
                                    lines = saleLines.map { line -> ReturnCandidateLine(line, database.returnDao().alreadyReturnedQuantity(line.id)) }
                                    quantities.clear()
                                    selectedSale = sale
                                    error = null
                                }
                            }) { Text("SELECT") }
                        }
                    }
                }
            }
        } else {
            val sale = selectedSale!!
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Sale ${sale.id.take(8)} • ₹${money(sale.total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
                item { Text("Select quantities to return. Already-returned quantities are excluded.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(lines, key = { it.saleLine.id }) { candidate ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val input = quantities[candidate.saleLine.id] ?: ""
                            Text(candidate.saleLine.name, fontWeight = FontWeight.Bold)
                            Text("Sold ${fmt(candidate.saleLine.quantity)} ${candidate.saleLine.unit} • Remaining ${fmt(candidate.remainingQuantity)} ${candidate.saleLine.unit}")
                            OutlinedTextField(
                                value = input,
                                onValueChange = { value -> quantities[candidate.saleLine.id] = value.filter { it.isDigit() || it == '.' || it == ',' }; error = null },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Return quantity") }
                            )
                        }
                    }
                }
                item {
                    RefundMethodPicker(refundMethod, onSelect = { refundMethod = it })
                    OutlinedTextField(reason, { reason = it; error = null }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("Return reason") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            val invalidInput = quantities.entries.firstOrNull { (_, raw) -> raw.isNotBlank() && raw.replace(',', '.').toDoubleOrNull() == null }
                            val selected = quantities.mapNotNull { (id, raw) ->
                                val q = raw.replace(',', '.').toDoubleOrNull()
                                if (q != null && q > 0.0) id to q else null
                            }.toMap()
                            when {
                                invalidInput != null -> error = "Return quantity must be a valid number."
                                selected.isEmpty() -> error = "Enter at least one return quantity."
                                reason.isBlank() -> error = "Return reason is required."
                                else -> {
                                    busy = true
                                    scope.launch {
                                        runCatching {
                                            ReturnRepository(database).processReturn(storeId, sale, selected, refundMethod, reason, staffRole)
                                        }.onSuccess { onBack() }
                                            .onFailure {
                                                error = it.message ?: "Return failed. No stock or ledger changes were committed."
                                                busy = false
                                            }
                                    }
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "PROCESSING..." else "PROCESS RETURN") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefundMethodPicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val methods = listOf("CASH", "UPI", "CARD", "CREDIT_REVERSAL")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selected, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("Refund method") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            methods.forEach { method -> DropdownMenuItem(text = { Text(method) }, onClick = { onSelect(method); expanded = false }) }
        }
    }
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
