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
    var supplierQuery by remember { mutableStateOf("") }
    var paymentSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var editorSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var showSupplierEditor by remember { mutableStateOf(false) }

    suspend fun reload() {
        suppliers = supplierDao.getAll(storeId)
        purchases = purchaseDao.getAll(storeId).take(50)
        balances = suppliers.associate { it.id to supplierLedgerDao.balance(storeId, it.id) }
    }

    LaunchedEffect(Unit) { reload() }

    val filteredSuppliers = suppliers.filter {
        supplierQuery.isBlank() ||
            it.name.contains(supplierQuery, ignoreCase = true) ||
            it.phone.contains(supplierQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SUPPLIERS & PURCHASES", fontWeight = FontWeight.Black) },
                navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNewPurchase, modifier = Modifier.weight(1f)) { Text("NEW PURCHASE") }
                    OutlinedButton(onClick = {
                        editorSupplier = null
                        showSupplierEditor = true
                    }, modifier = Modifier.weight(1f)) { Text("NEW SUPPLIER") }
                }
            }
            item {
                OutlinedTextField(
                    value = supplierQuery,
                    onValueChange = { supplierQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search supplier") }
                )
            }
            item { Text("SUPPLIERS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            if (filteredSuppliers.isEmpty()) {
                item { Text("No matching suppliers.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(filteredSuppliers, key = { it.id }) { supplier ->
                val balance = balances[supplier.id] ?: 0.0
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (supplier.phone.isNotBlank()) Text(supplier.phone)
                        if (supplier.address.isNotBlank()) Text(supplier.address, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "CURRENT PAYABLE ₹${money(balance)}",
                            color = if (balance > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                editorSupplier = supplier
                                showSupplierEditor = true
                            }, modifier = Modifier.weight(1f)) { Text("EDIT") }
                            if (balance > 0.0) {
                                OutlinedButton(onClick = { paymentSupplier = supplier }, modifier = Modifier.weight(1f)) { Text("PAY") }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "RECENT PURCHASES",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (purchases.isEmpty()) {
                item { Text("No purchases recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(purchases, key = { it.id }) { purchase ->
                val supplier = suppliers.firstOrNull { it.id == purchase.supplierId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(supplier?.name ?: "Unknown supplier", fontWeight = FontWeight.Bold)
                        Text(purchase.invoiceNumber?.let { "Invoice $it" } ?: "Purchase ${purchase.id.take(8)}")
                        Text(
                            "Net ₹${money(purchase.netAmount)} • Paid ₹${money(purchase.paidAmount)} • Initial outstanding ₹${money(purchase.outstandingAmount)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSupplierEditor) {
        SupplierEditorDialog(
            existing = editorSupplier,
            onDismiss = { showSupplierEditor = false },
            onSave = { name, phone, address, notes ->
                scope.launch {
                    val now = System.currentTimeMillis()
                    if (editorSupplier == null) {
                        supplierDao.insert(
                            SupplierEntity(
                                id = UUID.randomUUID().toString(),
                                storeId = storeId,
                                name = name,
                                phone = phone,
                                address = address,
                                notes = notes,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    } else {
                        supplierDao.update(
                            supplierId = editorSupplier!!.id,
                            storeId = storeId,
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            updatedAt = now
                        )
                    }
                    showSupplierEditor = false
                    reload()
                }
            }
        )
    }

    paymentSupplier?.let { supplier ->
        SupplierPaymentDialog(
            supplierName = supplier.name,
            outstanding = balances[supplier.id] ?: 0.0,
            onDismiss = { paymentSupplier = null },
            onSave = { amount, note ->
                scope.launch {
                    supplierLedgerDao.insert(
                        SupplierLedgerEntry(
                            id = UUID.randomUUID().toString(),
                            storeId = storeId,
                            supplierId = supplier.id,
                            amount = -amount,
                            type = "PAYMENT",
                            note = note.ifBlank { "Supplier payment" },
                            referenceType = "SUPPLIER_PAYMENT",
                            referenceId = UUID.randomUUID().toString(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    paymentSupplier = null
                    reload()
                }
            }
        )
    }
}

@Composable
private fun SupplierEditorDialog(
    existing: SupplierEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember(existing) { mutableStateOf(existing?.phone.orEmpty()) }
    var address by remember(existing) { mutableStateOf(existing?.address.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var error by remember(existing) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add supplier" else "Edit supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Supplier name") })
                OutlinedTextField(phone, { phone = it }, singleLine = true, label = { Text("Phone") })
                OutlinedTextField(address, { address = it }, singleLine = true, label = { Text("Address") })
                OutlinedTextField(notes, { notes = it }, minLines = 2, label = { Text("Notes") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cleaned = name.trim()
                if (cleaned.isBlank()) error = "Supplier name is required."
                else onSave(cleaned, phone.trim(), address.trim(), notes.trim())
            }) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun SupplierPaymentDialog(
    supplierName: String,
    outstanding: Double,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay $supplierName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current outstanding: ₹${money(outstanding)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(amount, { amount = it }, singleLine = true, label = { Text("Amount") })
                OutlinedTextField(note, { note = it }, singleLine = true, label = { Text("Note") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.replace(',', '.').toDoubleOrNull()
                when {
                    value == null || value <= 0.0 -> error = "Enter a positive amount."
                    value > outstanding + 1e-9 -> error = "Payment cannot exceed supplier outstanding."
                    else -> onSave(value, note.trim())
                }
            }) { Text("SAVE PAYMENT") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
