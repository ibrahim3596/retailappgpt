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
    val effectiveCost: Double get() = if (stockQuantity > 0) net / stockQuantity else 0.0
}

private fun formatExpiryDate(epochMillis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseEntryScreen(
    repository: PurchaseRepository,
    supplierDao: SupplierDao,
    productRepository: ProductRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val suppliers by supplierDao.observeAll().collectAsState(initial = emptyList())
    val products by productRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var supplierExpanded by remember { mutableStateOf(false) }
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var invoiceNumber by remember { mutableStateOf("") }
    var paidAmount by remember { mutableStateOf("") }
    var showLineDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val lines = remember { mutableStateListOf<PurchaseUiLine>() }

    val gross = lines.sumOf { it.gross }
    val scheme = lines.sumOf { it.schemeDiscount }
    val net = lines.sumOf { it.net }
    val paid = paidAmount.toDoubleOrNull() ?: 0.0
    val outstanding = (net - paid).coerceAtLeast(0.0)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Purchase Entry") }, navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SUPPLIER", fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(expanded = supplierExpanded, onExpandedChange = { supplierExpanded = !supplierExpanded }) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            label = { Text("Supplier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) }
                        )
                        DropdownMenu(expanded = supplierExpanded, onDismissRequest = { supplierExpanded = false }) {
                            suppliers.forEach { supplier ->
                                DropdownMenuItem(text = { Text(supplier.name) }, onClick = { selectedSupplier = supplier; supplierExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Invoice number") })
                } }
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
                    line.expiryDate?.let { Text("Expiry ${formatExpiryDate(it)}") }
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
                } }
            }
            item {
                Button(
                    onClick = {
                        val supplier = selectedSupplier
                        if (supplier == null) { error = "Select a supplier."; return@Button }
                        if (lines.isEmpty()) { error = "Add at least one purchase item."; return@Button }
                        scope.launch {
                            try {
                                val purchaseId = UUID.randomUUID().toString()
                                repository.createPurchase(
                                    PurchaseEntity(
                                        id = purchaseId,
                                        supplierId = supplier.id,
                                        invoiceNumber = invoiceNumber.trim().ifBlank { null },
                                        grossAmount = gross,
                                        schemeDiscount = scheme,
                                        netAmount = net,
                                        paidAmount = paid,
                                        outstandingAmount = outstanding,
                                        createdAt = System.currentTimeMillis()
                                    ),
                                    lines.map { line ->
                                        PurchaseLineEntity(
                                            id = UUID.randomUUID().toString(),
                                            purchaseId = purchaseId,
                                            productId = line.product.id,
                                            orderedQuantity = line.orderedQuantity,
                                            freeQuantity = line.freeQuantity,
                                            purchaseRate = line.purchaseRate,
                                            schemeDiscount = line.schemeDiscount,
                                            netAmount = line.net,
                                            effectiveCost = line.effectiveCost,
                                            batchNumber = line.batchNumber,
                                            expiryDate = line.expiryDate
                                        )
                                    },
                                    lines.map { line ->
                                        InventoryBatchEntity(
                                            id = UUID.randomUUID().toString(),
                                            productId = line.product.id,
                                            batchNumber = line.batchNumber,
                                            expiryDate = line.expiryDate,
                                            quantity = line.stockQuantity,
                                            purchaseRate = line.effectiveCost,
                                            createdAt = System.currentTimeMillis()
                                        )
                                    }
                                )
                                onSaved()
                            } catch (t: Throwable) {
                                error = t.message ?: "Unable to save purchase."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("SAVE PURCHASE") }
            }
        }
    }

    if (showLineDialog) {
        PurchaseLineDialog(
            products = products,
            onDismiss = { showLineDialog = false },
            onAdd = { line -> lines.add(line); showLineDialog = false }
        )
    }
}

@Composable
private fun PurchaseLineDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onAdd: (PurchaseUiLine) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(products.firstOrNull()) }
    var quantity by remember { mutableStateOf("") }
    var freeQuantity by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("0") }
    var batch by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var productExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add purchase item") },
        text = {
            Column(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = productExpanded, onExpandedChange = { productExpanded = !productExpanded }) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Product") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) }
                    )
                    DropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                        products.forEach { product ->
                            DropdownMenuItem(text = { Text(product.name) }, onClick = { selectedProduct = product; productExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, singleLine = true, label = { Text("Ordered quantity") })
                OutlinedTextField(value = freeQuantity, onValueChange = { freeQuantity = it }, singleLine = true, label = { Text("Free quantity") })
                OutlinedTextField(value = rate, onValueChange = { rate = it }, singleLine = true, label = { Text("Purchase rate") })
                OutlinedTextField(value = discount, onValueChange = { discount = it }, singleLine = true, label = { Text("Scheme discount") })
                OutlinedTextField(value = batch, onValueChange = { batch = it }, singleLine = true, label = { Text("Batch number") })
                OutlinedTextField(value = expiry, onValueChange = { expiry = it }, singleLine = true, label = { Text("Expiry YYYY-MM-DD") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val product = selectedProduct
                val ordered = quantity.toDoubleOrNull()
                val free = freeQuantity.toDoubleOrNull() ?: 0.0
                val purchaseRate = rate.toDoubleOrNull()
                val schemeDiscount = discount.toDoubleOrNull() ?: 0.0
                if (product == null || ordered == null || ordered <= 0.0 || purchaseRate == null || purchaseRate < 0.0) {
                    error = "Enter a valid product, quantity and purchase rate."
                    return@TextButton
                }
                val expiryMillis = expiry.trim().takeIf { it.isNotBlank() }?.let {
                    runCatching { LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrElse {
                        error = "Expiry must use YYYY-MM-DD."
                        return@TextButton
                    }
                }
                onAdd(PurchaseUiLine(product, ordered, free, purchaseRate, schemeDiscount, batch.trim().ifBlank { null }, expiryMillis))
            }) { Text("ADD") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
private fun fmtMoney(value: Double): String = String.format(Locale.US, "%.2f", value)
