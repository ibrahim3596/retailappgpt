package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.InventoryBatchEntity
import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.PurchaseEntity
import com.retailpos.app.data.PurchaseLineEntity
import com.retailpos.app.data.PurchaseRepository
import com.retailpos.app.data.SupplierEntity
import com.retailpos.app.data.SupplierDao
import com.retailpos.app.data.ProductRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

private data class PurchaseUiLine(
    val product: ProductEntity,
    val orderedQuantity: Double,
    val freeQuantity: Double,
    val purchaseRate: Double,
    val schemeDiscount: Double,
    val batchNumber: String?,
    val expiryDate: Long?
) {
    val gross: Double get() = orderedQuantity * purchaseRate
    val net: Double get() = gross - schemeDiscount
    val stockQuantity: Double get() = orderedQuantity + freeQuantity
    val effectiveCost: Double get() = if (stockQuantity > 0.0) net / stockQuantity else 0.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseEntryScreen(
    storeId: String,
    repository: ProductRepository,
    supplierDao: SupplierDao,
    purchaseRepository: PurchaseRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var suppliers by remember { mutableStateOf<List<SupplierEntity>>(emptyList()) }
    val products by repository.observeProducts(storeId).collectAsState(initial = emptyList())
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    val lines = remember { mutableStateListOf<PurchaseUiLine>() }
    var invoiceNumber by remember { mutableStateOf("") }
    var paidAmount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSupplierDialog by remember { mutableStateOf(false) }
    var showLineDialog by remember { mutableStateOf(false) }

    fun loadSuppliers() { scope.launch { suppliers = supplierDao.getAll(storeId) } }
    LaunchedEffect(Unit) { loadSuppliers() }

    val gross = lines.sumOf { it.gross }
    val scheme = lines.sumOf { it.schemeDiscount }
    val net = lines.sumOf { it.net }
    val paid = paidAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val outstanding = (net - paid).coerceAtLeast(0.0)

    Scaffold(topBar = { TopAppBar(title = { Text("PURCHASE", fontWeight = FontWeight.Black) }, navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) { Text("SUPPLIERS / HISTORY") }
                    OutlinedButton(onClick = { showSupplierDialog = true }, modifier = Modifier.weight(1f)) { Text("ADD SUPPLIER") }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("SUPPLIER", fontWeight = FontWeight.Bold)
                        SupplierPicker(selectedSupplier, suppliers, onSelect = { selectedSupplier = it })
                        OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Invoice number (optional)") })
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ITEMS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    TextButton(onClick = { if (products.isEmpty()) error = "Add products to the catalog before recording purchases." else showLineDialog = true }) { Text("ADD ITEM") }
                }
            }
            if (lines.isEmpty()) item { Text("No purchase items yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(lines) { line ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(line.product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Paid ${fmt(line.orderedQuantity)} ${line.product.unit} + free ${fmt(line.freeQuantity)} = stock ${fmt(line.stockQuantity)} ${line.product.unit}")
                    Text("Rate ₹${fmtMoney(line.purchaseRate)} • Net ₹${fmtMoney(line.net)} • Effective ₹${fmtMoney(line.effectiveCost)}")
                    if (line.batchNumber != null) Text("Batch ${line.batchNumber}")
                    line.expiryDate?.let { Text("Expiry ${LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneId.systemDefault())}") }
                } }
            }
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SUMMARY", fontWeight = FontWeight.Bold)
                    Text("Gross ₹${fmtMoney(gross)}")
                    Text("Scheme ₹${fmtMoney(scheme)}")
                    Text("NET ₹${fmtMoney(net)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    OutlinedTextField(value = paidAmount, onValueChange = { paidAmount = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Paid to supplier") })
                    Text("Outstanding ₹${fmtMoney(outstanding)}", fontWeight = FontWeight.Bold)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = {
                        val supplier = selectedSupplier
                        val paidValue = paidAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
                        when {
                            supplier == null -> error = "Select a supplier."
                            lines.isEmpty() -> error = "Add at least one purchase item."
                            lines.map { it.product.id }.distinct().size != lines.size -> error = "Each product can appear only once per purchase."
                            paidValue < 0.0 -> error = "Paid amount cannot be negative."
                            paidValue > net + 1e-9 -> error = "Paid amount cannot exceed purchase total."
                            else -> scope.launch {
                                runCatching {
                                    val now = System.currentTimeMillis()
                                    val purchaseId = UUID.randomUUID().toString()
                                    val entity = PurchaseEntity(purchaseId, storeId, supplier.id, invoiceNumber.trim().ifBlank { null }, gross, scheme, net, paidValue, outstanding, now)
                                    val purchaseLines = lines.map { line -> PurchaseLineEntity(purchaseId, storeId, line.product.id, line.orderedQuantity, line.freeQuantity, line.purchaseRate, line.schemeDiscount, line.net, line.effectiveCost, line.batchNumber, line.expiryDate, now) }
                                    val batches = lines.map { line -> InventoryBatchEntity(UUID.randomUUID().toString(), storeId, line.product.id, line.batchNumber, line.expiryDate, line.stockQuantity, line.effectiveCost, now) }
                                    purchaseRepository.recordPurchase(entity, purchaseLines, batches, now)
                                }.onSuccess { onSaved() }.onFailure { error = it.message ?: "Purchase could not be saved." }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = selectedSupplier != null && lines.isNotEmpty()) { Text("RECEIVE PURCHASE") }
                } }
            }
        }
    }

    if (showSupplierDialog) SupplierDialog(onDismiss = { showSupplierDialog = false }, onSave = { name, phone, address, notes ->
        scope.launch {
            val now = System.currentTimeMillis()
            val supplier = SupplierEntity(UUID.randomUUID().toString(), storeId, name.trim(), phone.trim(), address.trim(), notes.trim(), now, now)
            supplierDao.insert(supplier); selectedSupplier = supplier; showSupplierDialog = false; loadSuppliers()
        }
    })

    if (showLineDialog) PurchaseLineDialog(
        products = products,
        alreadySelectedProductIds = lines.map { it.product.id }.toSet(),
        onDismiss = { showLineDialog = false },
        onAdd = { product, ordered, free, rate, discount, batch, expiry ->
            lines += PurchaseUiLine(product, ordered, free, rate, discount, batch.ifBlank { null }, expiry)
            showLineDialog = false
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierPicker(selected: SupplierEntity?, suppliers: List<SupplierEntity>, onSelect: (SupplierEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = selected?.name.orEmpty(), onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("Supplier") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) { suppliers.forEach { supplier -> DropdownMenuItem(text = { Text(supplier.name) }, onClick = { onSelect(supplier); expanded = false }) } }
    }
}

@Composable
private fun SupplierDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add supplier") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }); OutlinedTextField(phone, { phone = it }, singleLine = true, label = { Text("Phone") }); OutlinedTextField(address, { address = it }, minLines = 2, label = { Text("Address") }); OutlinedTextField(notes, { notes = it }, minLines = 2, label = { Text("Notes") })
    } }, confirmButton = { Button(onClick = { if (name.isNotBlank()) onSave(name, phone, address, notes) }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseLineDialog(products: List<ProductEntity>, alreadySelectedProductIds: Set<String>, onDismiss: () -> Unit, onAdd: (ProductEntity, Double, Double, Double, Double, String, Long?) -> Unit) {
    var selected by remember { mutableStateOf<ProductEntity?>(null) }; var expanded by remember { mutableStateOf(false) }; var ordered by remember { mutableStateOf("1") }; var free by remember { mutableStateOf("0") }; var rate by remember { mutableStateOf("") }; var discount by remember { mutableStateOf("0") }; var batch by remember { mutableStateOf("") }; var expiry by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    val expiryMillis = expiry.trim().takeIf { it.isNotBlank() }?.let { raw -> runCatching { LocalDate.parse(raw).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add purchase item") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selected?.name.orEmpty(), onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("Product") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) { products.filterNot { it.id in alreadySelectedProductIds }.forEach { product -> DropdownMenuItem(text = { Text("${product.name} • ${product.unit}") }, onClick = { selected = product; expanded = false }) } }
        }
        if (products.none { it.id !in alreadySelectedProductIds }) Text("All available products are already on this purchase.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(ordered, { ordered = it }, singleLine = true, label = { Text("Paid quantity") }); OutlinedTextField(free, { free = it }, singleLine = true, label = { Text("Free quantity") }); OutlinedTextField(rate, { rate = it }, singleLine = true, label = { Text("Purchase rate") }); OutlinedTextField(discount, { discount = it }, singleLine = true, label = { Text("Scheme discount") }); OutlinedTextField(batch, { batch = it }, singleLine = true, label = { Text("Batch number") }); OutlinedTextField(expiry, { expiry = it }, singleLine = true, label = { Text("Expiry YYYY-MM-DD") }); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } }, confirmButton = { Button(onClick = {
        val q = ordered.replace(',', '.').toDoubleOrNull(); val f = free.replace(',', '.').toDoubleOrNull(); val r = rate.replace(',', '.').toDoubleOrNull(); val d = discount.replace(',', '.').toDoubleOrNull()
        when {
            selected == null -> error = "Select a product."
            q == null || q <= 0 -> error = "Paid quantity must be greater than zero."
            f == null || f < 0 -> error = "Free quantity cannot be negative."
            r == null || r < 0 -> error = "Purchase rate cannot be negative."
            d == null || d < 0 -> error = "Scheme discount cannot be negative."
            d > (q!! * r!! + 1e-9) -> error = "Scheme discount cannot exceed gross cost."
            expiry.isNotBlank() && expiryMillis == null -> error = "Use expiry format YYYY-MM-DD."
            expiryMillis != null && expiryMillis < System.currentTimeMillis() -> error = "Expiry date cannot be in the past."
            expiryMillis != null && batch.isBlank() -> error = "Batch number is required when expiry is entered."
            selected!!.id in alreadySelectedProductIds -> error = "That product is already on this purchase."
            else -> onAdd(selected!!, q!!, f!!, r!!, d!!, batch.trim(), expiryMillis)
        }
    }) { Text("ADD") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
private fun fmtMoney(value: Double): String = String.format(Locale.US, "%.2f", value)
