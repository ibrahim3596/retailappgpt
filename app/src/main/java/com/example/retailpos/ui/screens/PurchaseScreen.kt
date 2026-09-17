package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.PurchaseEntity
import com.example.retailpos.data.local.entity.PurchaseItemEntity
import com.example.retailpos.data.local.entity.SupplierEntity
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.PurchaseItemEntry
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.MetricTile
import com.example.ui.theme.*
import com.example.ui.theme.Spacing
import com.example.ui.theme.IconSizes
import com.example.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val purchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var invoiceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<MutableList<PurchaseItemEntry>>(mutableListOf()) }

    val filteredPurchases = remember(purchases, searchQuery) {
        if (searchQuery.isBlank()) purchases
        else purchases.filter {
            it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
            it.supplierName.contains(searchQuery, ignoreCase = true)
        }
    }

    val supplierOptions = suppliers.map { it.name }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("PURCHASES", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedSupplier = null
                    invoiceNumber = ""
                    notes = ""
                    items.clear()
                    showAddDialog = true
                },
                containerColor = Primary,
                contentColor = Color.White,
                shape = Shapes.pill,
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Purchase Order") },
                text = { Text("NEW PURCHASE", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Header
            Column {
                Text("PURCHASE ORDERS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Manage supplier purchase orders and stock receipts", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            // Summary Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                MetricTile(
                    label = "Total POs",
                    value = "${purchases.size}",
                    icon = Icons.Default.ReceiptLong,
                    iconColor = Primary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Total Value",
                    value = "₹${String.format("%,.0f", purchases.sumOf { it.totalAmount })}",
                    icon = Icons.Default.AttachMoney,
                    iconColor = Success,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Suppliers",
                    value = "${suppliers.size}",
                    icon = Icons.Default.Business,
                    iconColor = Info,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Items",
                    value = "${products.size}",
                    icon = Icons.Default.Inventory2,
                    iconColor = Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by PO number or supplier", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                shape = Shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineVariant,
                    unfocusedContainerColor = Surface,
                    focusedContainerColor = Surface
                ),
                singleLine = true
            )

            // Purchase List
            if (filteredPurchases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Purchase Orders", modifier = Modifier.size(64.dp), tint = TextTertiary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text("No purchase orders found", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Text("Create a purchase order to restock inventory", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(filteredPurchases) { purchase ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Surface,
                            shape = Shapes.card,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(purchase.invoiceNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Supplier: ${purchase.supplierName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Text("₹${String.format("%,.2f", purchase.totalAmount)}", fontWeight = FontWeight.Black, color = Primary, style = MaterialTheme.typography.titleMedium)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(purchase.createdAt))}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    Text("GST: ₹${String.format("%,.2f", purchase.gstTotal)}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Purchase Order", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    // Supplier selection
                    if (suppliers.isEmpty()) {
                        Text("No suppliers available. Add a supplier first.", style = MaterialTheme.typography.bodyMedium, color = Warning)
                    } else {
                        // Simple text display for supplier selection (full dropdown requires anchor)
                        Text("Supplier: ${selectedSupplier?.name ?: "Select supplier"}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }

                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Invoice Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text("Items (add at least one)", fontWeight = FontWeight.Bold, color = TextSecondary)

                    // Simplified: just show a button to add items
                    if (items.isEmpty()) {
                        Text("Items will be added after saving the PO header", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.heightIn(max = 200.dp)) {
                            items(items) { entry ->
                                Row(modifier = Modifier.fillMaxWidth().padding(Spacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${entry.product.name} x ${entry.quantity.toInt()}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text("₹${String.format("%,.2f", entry.purchasePrice * entry.quantity)}", fontWeight = FontWeight.Bold, color = Primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentStore = store
                        if (selectedSupplier != null && currentStore != null) {
                            val newPurchase = PurchaseEntity(
                                id = UUID.randomUUID().toString(),
                                storeId = currentStore.id,
                                supplierId = selectedSupplier!!.id,
                                supplierName = selectedSupplier!!.name,
                                invoiceNumber = invoiceNumber,
                                totalAmount = items.sumOf { it.purchasePrice * it.quantity },
                                gstTotal = 0.0,
                                notes = notes
                            )
                            scope.launch { viewModel.savePurchase(newPurchase, items) }
                            showAddDialog = false
                            Toast.makeText(context, "Purchase order created", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = selectedSupplier != null,
                    shape = Shapes.pill
                ) {
                    Text("CREATE PO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("CANCEL", color = TextSecondary) } }
        )
    }
}