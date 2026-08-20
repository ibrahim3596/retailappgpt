package com.retailpos.app.ui.screens

import android.content.Intent
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.customer.KhataRules
import com.retailpos.app.core.khata.KhataStatementRules
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.CustomerLedgerEntry
import com.retailpos.app.data.KhataDao
import com.retailpos.app.data.RetailDatabase
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Composable
fun CustomerKhataScreen(storeId: String, customerId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { RetailDatabase.get(context) }
    val customer by produceState<CustomerEntity?>(initialValue = null, customerId) {
        value = database.customerDao().getById(customerId, storeId)
    }
    if (customer != null) CustomerKhataScreen(storeId, customer, database.khataDao(), onBack)
    else Scaffold(topBar = { TopAppBar(title = { Text("KHATA", fontWeight = FontWeight.Black) }) }) { padding ->
        Text("Customer not found.", Modifier.fillMaxSize().padding(padding).padding(24.dp), color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerKhataScreen(storeId: String, customer: CustomerEntity, dao: KhataDao, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val balance by dao.observeBalance(storeId, customer.id).collectAsState(initial = 0.0)
    val entries by dao.observeEntries(storeId, customer.id).collectAsState(initial = emptyList())
    var showPayment by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(customer.name, fontWeight = FontWeight.Black) }, actions = { TextButton(onClick = {
        val statement = KhataStatementRules.format(customer, balance, entries)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, statement) }, "Share Khata statement"))
    }) { Text("SHARE") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("OUTSTANDING", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(money(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(when (KhataRules.displayState(balance)) {
                        com.retailpos.app.core.customer.KhataState.DUE -> "Customer owes the store"
                        com.retailpos.app.core.customer.KhataState.CREDIT -> "Store has excess credit"
                        com.retailpos.app.core.customer.KhataState.SETTLED -> "Settled"
                        com.retailpos.app.core.customer.KhataState.INVALID -> "Invalid balance"
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (balance > 0.0) Button(onClick = { showPayment = true }) { Text("RECORD PAYMENT") }
                } }
            }
            item { Text("TRANSACTIONS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            if (entries.isEmpty()) item { Text("No Khata transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else items(entries, key = { it.id }) { entry -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(entry.type.replace('_', ' '), fontWeight = FontWeight.Bold); Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(if (entry.amount >= 0) "+${money(entry.amount)}" else money(entry.amount), fontWeight = FontWeight.Bold) } } }
            item { TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }

    if (showPayment) PaymentDialog(outstanding = balance, onDismiss = { showPayment = false }, onSave = { amount, note -> scope.launch { dao.insert(CustomerLedgerEntry(UUID.randomUUID().toString(), storeId, customer.id, -amount, "PAYMENT", note.ifBlank { "Payment received" }, "PAYMENT", UUID.randomUUID().toString(), System.currentTimeMillis())); showPayment = false } })
}

@Composable
private fun PaymentDialog(outstanding: Double, onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Record payment") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Outstanding: ${money(outstanding)}", color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, singleLine = true, label = { Text("Amount") }); OutlinedTextField(value = note, onValueChange = { note = it }, singleLine = true, label = { Text("Note (optional)") }); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } }, confirmButton = { Button(onClick = { val value = amount.replace(',', '.').toDoubleOrNull(); val validation = value?.let { KhataRules.validatePayment(outstanding, it) } ?: "Payment must be a valid number"; if (validation != null) error = validation else onSave(value!!, note.trim()) }) { Text("SAVE PAYMENT") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
