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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.PurchaseDao
import com.retailpos.app.data.PurchaseEntity
import com.retailpos.app.data.SupplierDao
import com.retailpos.app.data.SupplierEntity
import com.retailpos.app.data.SupplierLedgerEntry
import com.retailpos.app.data.SupplierLedgerDao
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Composable
fun SupplierPurchaseHistoryScreen(
    storeId: String,
    supplierDao: SupplierDao,
    purchaseDao: PurchaseDao,
    supplierLedgerDao: SupplierLedgerDao,
    onBack: () -> Unit,
    onNewPurchase: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var suppliers by remember { mutableStateOf<List<SupplierEntity>>(emptyList()) }
    var purchases by remember { mutableStateOf<List<PurchaseEntity>>(emptyList()) }
    var balances by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var paymentSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    suspend fun reload() {
        suppliers = supplierDao.getAll(storeId)
        purchases = purchaseDao.getAll(storeId).take(30)
        balances = suppliers.associate { it.id to supplierLedgerDao.balance(storeId, it.id) }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { TopAppBar(title = { Text("SUPPLIERS & PURCHASES", fontWeight = FontWeight.Black) }, navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Button(onClick = onNewPurchase, modifier = Modifier.fillMaxWidth()) { Text("NEW PURCHASE") } }
            item { Text("SUPPLIERS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            if (suppliers.isEmpty()) item { Text("No suppliers yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(suppliers, key = { it.id }) { supplier ->
                val balance = balances[supplier.id] ?: 0.0
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (supplier.phone.isNotBlank()) Text(supplier.phone)
                    Text("CURRENT PAYABLE ₹${money(balance)}", color = if (balance > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    if (balance > 0.0) OutlinedButton(onClick = { paymentSupplier = supplier }) { Text("PAY SUPPLIER") }
                } }
            }
            item { Text("RECENT PURCHASES", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp)) }
            if (purchases.isEmpty()) item { Text("No purchases recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(purchases, key = { it.id }) { purchase ->
                val supplier = suppliers.firstOrNull { it.id == purchase.supplierId }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(supplier?.name ?: "Unknown supplier", fontWeight = FontWeight.Bold)
                    Text(purchase.invoiceNumber?.let { "Invoice $it" } ?: "Purchase ${purchase.id.take(8)}")
                    Text("Net ₹${money(purchase.netAmount)} • Paid at posting ₹${money(purchase.paidAmount)} • Initial outstanding ₹${money(purchase.outstandingAmount)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
            }
        }
    }

    paymentSupplier?.let { supplier ->
        SupplierPaymentDialog(supplierName = supplier.name, outstanding = balances[supplier.id] ?: 0.0, onDismiss = { paymentSupplier = null }, onSave = { amount, note ->
            scope.launch {
                supplierLedgerDao.insert(SupplierLedgerEntry(UUID.randomUUID().toString(), storeId, supplier.id, -amount, "PAYMENT", note.ifBlank { "Supplier payment" }, "SUPPLIER_PAYMENT", UUID.randomUUID().toString(), System.currentTimeMillis()))
                paymentSupplier = null
                reload()
            }
        })
    }
}

@Composable
private fun SupplierPaymentDialog(supplierName: String, outstanding: Double, onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pay $supplierName") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Current outstanding: ₹${money(outstanding)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(amount, { amount = it }, singleLine = true, label = { Text("Amount") })
        OutlinedTextField(note, { note = it }, singleLine = true, label = { Text("Note") })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(onClick = {
        val value = amount.replace(',', '.').toDoubleOrNull()
        when { value == null || value <= 0.0 -> error = "Enter a positive amount."; value > outstanding + 1e-9 -> error = "Payment cannot exceed supplier outstanding."; else -> onSave(value, note.trim()) }
    }) { Text("SAVE PAYMENT") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
