package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.CustomerLedgerEntry
import com.retailpos.app.data.KhataDao
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerKhataScreen(storeId: String, customer: CustomerEntity, dao: KhataDao, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val balance by dao.observeBalance(storeId, customer.id).collectAsState(initial = 0.0)
    val entries by dao.observeEntries(storeId, customer.id).collectAsState(initial = emptyList())
    var showPayment by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(customer.name, fontWeight = FontWeight.Black) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("OUTSTANDING", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(money(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(if (balance > 0.0) "Customer owes the store" else if (balance < 0.0) "Store has excess credit" else "Settled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (balance > 0.0) Button(onClick = { showPayment = true }) { Text("RECORD PAYMENT") }
                } }
            }
            item { Text("TRANSACTIONS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            if (entries.isEmpty()) item { Text("No Khata transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(entries, key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(entry.type.replace('_', ' '), fontWeight = FontWeight.Bold); Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(if (entry.amount >= 0) "+${money(entry.amount)}" else money(entry.amount), fontWeight = FontWeight.Bold)
                } }
            }
            item { TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }

    if (showPayment) {
        PaymentDialog(
            onDismiss = { showPayment = false },
            onSave = { amount, note ->
                scope.launch {
                    dao.insert(CustomerLedgerEntry(UUID.randomUUID().toString(), storeId, customer.id, -amount, "PAYMENT", note.ifBlank { "Payment received" }, "PAYMENT", UUID.randomUUID().toString(), System.currentTimeMillis()))
                    showPayment = false
                }
            }
        )
    }
}

@Composable
private fun PaymentDialog(onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Record payment") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = amount, onValueChange = { amount = it }, singleLine = true, label = { Text("Amount") })
            OutlinedTextField(value = note, onValueChange = { note = it }, singleLine = true, label = { Text("Note (optional)") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { Button(onClick = { val value = amount.toDoubleOrNull(); if (value == null || value <= 0.0) error = "Enter a valid positive amount." else onSave(value, note.trim()) }) { Text("SAVE PAYMENT") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
