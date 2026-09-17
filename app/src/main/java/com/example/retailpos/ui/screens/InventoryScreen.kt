package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.BatchEntity
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.StockMovementEntity
import com.example.retailpos.data.local.entity.StockMovementType
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.MetricTile
import com.example.ui.theme.*
import com.example.ui.theme.Spacing
import com.example.ui.theme.IconSizes
import com.example.ui.theme.Shapes
import com.example.ui.theme.Elevation
import com.example.ui.theme.Shapes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val expiringBatches by viewModel.expiringSoonBatches.collectAsStateWithLifecycle()
    val movements by viewModel.allStockMovements.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, LOW, OUT, EXPIRING, HISTORY
    
    val lowStockCount = remember(products) {
        products.count { it.currentStock <= it.minStock && it.currentStock > 0 }
    }
    val outOfStockCount = remember(products) {
        products.count { it.currentStock <= 0 }
    }

    val filteredProducts = remember(products, searchQuery, selectedFilter) {
        products.filter { prod ->
            val matchesSearch = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery, ignoreCase = true) ||
                    prod.sku.contains(searchQuery, ignoreCase = true) ||
                    prod.brand.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "LOW" -> prod.currentStock <= prod.minStock && prod.currentStock > 0
                "OUT" -> prod.currentStock <= 0
                "HEALTHY" -> prod.currentStock > prod.minStock
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    var showAddBatchDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedProductId by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var batchQtyText by remember { mutableStateOf("50") }

    fun quickRestock(product: ProductEntity, addQty: Double) {
        viewModel.quickRestockWithAuth(product, addQty) { success ->
            if (success) {
                Toast.makeText(context, "Restocked +${addQty.toInt()} for ${product.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Unauthorized operation", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("STOCK MANAGEMENT", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
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
                onClick = { showAddBatchDialog = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = Shapes.pill,
                icon = { Icon(Icons.Default.AddBox, contentDescription = "Add Stock") },
                text = { Text("RECEIVE STOCK", fontWeight = FontWeight.Bold) }
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
                Text("INVENTORY", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Track stock, batches and expiry", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            // Summary Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                MetricTile(
                    label = "Items",
                    value = "${products.size}",
                    icon = Icons.Default.Inventory2,
                    iconColor = Primary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Low",
                    value = "$lowStockCount",
                    icon = Icons.Default.Warning,
                    iconColor = Warning,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Out",
                    value = "$outOfStockCount",
                    icon = Icons.Default.Error,
                    iconColor = Error,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Expiring",
                    value = "${expiringBatches.size}",
                    icon = Icons.Default.EventBusy,
                    iconColor = Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, SKU or barcode", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search products", tint = TextSecondary) },
                shape = Shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineVariant,
                    unfocusedContainerColor = Surface,
                    focusedContainerColor = Surface
                ),
                singleLine = true
            )

            // Filter Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All Items") },
                        shape = Shapes.pill
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "LOW",
                        onClick = { selectedFilter = "LOW" },
                        label = { Text("Low Stock") },
                        shape = Shapes.pill,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Warning.copy(alpha = 0.1f),
                            selectedLabelColor = Warning
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "OUT",
                        onClick = { selectedFilter = "OUT" },
                        label = { Text("Out of Stock") },
                        shape = Shapes.pill,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Error.copy(alpha = 0.1f),
                            selectedLabelColor = Error
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "EXPIRING",
                        onClick = { selectedFilter = "EXPIRING" },
                        label = { Text("Expiring Soon") },
                        shape = Shapes.pill
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "HISTORY",
                        onClick = { selectedFilter = "HISTORY" },
                        label = { Text("History") },
                        shape = Shapes.pill
                    )
                }
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (selectedFilter) {
                    "HISTORY" -> StockHistoryList(movements, products)
                    "EXPIRING" -> ExpiringBatchesList(expiringBatches, products)
                    "LOW" -> {
                        if (filteredProducts.isEmpty() && searchQuery.isBlank()) {
                            EmptyInventoryState("All products are adequately stocked.")
                        } else {
                            ProductsInventoryList(filteredProducts, ::quickRestock) { showAdjustDialog = it }
                        }
                    }
                    "OUT" -> {
                        if (filteredProducts.isEmpty() && searchQuery.isBlank()) {
                            EmptyInventoryState("No products are out of stock.")
                        } else {
                            ProductsInventoryList(filteredProducts, ::quickRestock) { showAdjustDialog = it }
                        }
                    }
                    else -> {
                        if (products.isEmpty()) {
                            EmptyInventoryState("Add products to start tracking stock.")
                        } else if (filteredProducts.isEmpty()) {
                            EmptyInventoryState("No matching products found.")
                        } else {
                            ProductsInventoryList(filteredProducts, ::quickRestock) { showAdjustDialog = it }
                        }
                    }
                }
            }
        }
    }

    if (showAdjustDialog != null) {
        val product = showAdjustDialog!!
        AdjustStockDialog(
            product = product,
            onDismiss = { showAdjustDialog = null },
            onConfirm = { change, reason ->
                viewModel.adjustStockWithAuth(
                    product = product,
                    quantityChange = change,
                    type = com.example.retailpos.data.local.entity.StockMovementType.ADJUSTMENT,
                    notes = reason
                ) { success ->
                    if (success) {
                        showAdjustDialog = null
                        Toast.makeText(context, "Stock adjusted successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Insufficient stock or Unauthorized", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showAddBatchDialog) {
        AddBatchDialog(
            products = products,
            onDismiss = { showAddBatchDialog = false },
            onConfirm = { batch ->
                viewModel.addBatchWithAuth(batch) { success ->
                    if (success) {
                        showAddBatchDialog = false
                        Toast.makeText(context, "Stock received successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Unauthorized operation", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ProductsInventoryList(
    products: List<ProductEntity>,
    onQuickRestock: (ProductEntity, Double) -> Unit,
    onAdjustStock: (ProductEntity) -> Unit
) {
    if (products.isEmpty()) {
        EmptyInventoryState("No items found")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products) { product ->
                val status = when {
                    product.currentStock <= 0 -> "OUT OF STOCK"
                    product.currentStock <= product.minStock -> "LOW STOCK"
                    else -> "IN STOCK"
                }
                val statusColor = when (status) {
                    "OUT OF STOCK" -> Error
                    "LOW STOCK" -> Warning
                    else -> Success
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.card,
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = Elevation.level1
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                ) {
                    Column(modifier = Modifier.padding(Spacing.cardPadding), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    Text("SKU: ${product.sku}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("•", color = TextSecondary)
                                    Text(product.brand, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.1f),
                                shape = Shapes.small
                            ) {
                                Text(
                                    text = status,
                                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = statusColor
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text("STOCK LEVEL", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    Text(
                                        "${product.currentStock.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = statusColor
                                    )
                                    Text(
                                        product.unit,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = Spacing.xs)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                OutlinedButton(
                                    onClick = { onAdjustStock(product) },
                                    contentPadding = PaddingValues(horizontal = Spacing.md),
                                    modifier = Modifier.height(36.dp),
                                    shape = Shapes.pill,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "Adjust Stock", modifier = Modifier.size(IconSizes.sm), tint = Primary)
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("ADJUST", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                                }
                                listOf(10, 50).forEach { qty ->
                                    OutlinedButton(
                                        onClick = { onQuickRestock(product, qty.toDouble()) },
                                        contentPadding = PaddingValues(horizontal = Spacing.md),
                                        modifier = Modifier.height(36.dp),
                                        shape = Shapes.pill,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                                    ) {
                                        Text("+$qty", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiringBatchesList(batches: List<BatchEntity>, products: List<ProductEntity>) {
    if (batches.isEmpty()) {
        EmptyInventoryState("No items expiring soon")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(batches) { batch ->
                val product = products.find { it.id == batch.productId }
                val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val isExpired = batch.expiryDate < System.currentTimeMillis()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.card,
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isExpired) Error.copy(alpha = 0.3f) else OutlineVariant)
                ) {
                    Row(modifier = Modifier.padding(Spacing.cardPadding), horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (isExpired) Error.copy(alpha = 0.1f) else Warning.copy(alpha = 0.1f),
                            shape = Shapes.medium,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val icon = if (isExpired) Icons.Default.Block else Icons.Default.EventBusy
                                val desc = if (isExpired) "Expired" else "Expiring Soon"
                                val iconColor = if (isExpired) Error else Warning
                                Icon(
                                    icon,
                                    contentDescription = desc,
                                    tint = iconColor,
                                    modifier = Modifier.size(IconSizes.lg)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product?.name ?: "Unknown Product", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Batch: ${batch.batchNumber}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                "Expires: ${df.format(Date(batch.expiryDate))}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpired) Error else Warning
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${batch.remainingQty.toInt()} LEFT", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("MRP ₹${batch.mrp}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StockHistoryList(movements: List<StockMovementEntity>, products: List<ProductEntity>) {
    if (movements.isEmpty()) {
        EmptyInventoryState("No stock movements yet")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(movements) { movement ->
                val product = products.find { it.id == movement.productId }
                val df = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

                Surface(
                    color = Surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.cardPadding),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val color = when {
                            movement.quantity > 0 -> Success
                            movement.quantity < 0 -> Error
                            else -> TextSecondary
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(product?.name ?: "Unknown Product", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("${movement.type} • ${df.format(Date(movement.timestamp))}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            if (movement.notes.isNotBlank()) {
                                Text(movement.notes, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (movement.quantity > 0) "+${movement.quantity.toInt()}" else "${movement.quantity.toInt()}",
                                fontWeight = FontWeight.Black,
                                color = color,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Bal: ${movement.balanceAfter.toInt()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
                Divider(color = OutlineVariant, modifier = Modifier.padding(horizontal = Spacing.cardPadding))
            }
        }
    }
}

@Composable
fun EmptyInventoryState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Inventory, contentDescription = "Inventory", modifier = Modifier.size(64.dp), tint = TextTertiary.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AdjustStockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Manual correction") }
    var isAddition by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("Stock Adjustment", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { isAddition = true },
                        color = if (isAddition) Success.copy(alpha = 0.1f) else SurfaceVariant,
                        shape = Shapes.medium,
                        border = if (isAddition) androidx.compose.foundation.BorderStroke(1.dp, Success) else null
                    ) {
                        Box(Modifier.padding(Spacing.md), contentAlignment = Alignment.Center) {
                            Text("ADD (+)", fontWeight = FontWeight.Bold, color = if (isAddition) Success else TextSecondary)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).clickable { isAddition = false },
                        color = if (!isAddition) Error.copy(alpha = 0.1f) else SurfaceVariant,
                        shape = Shapes.medium,
                        border = if (!isAddition) androidx.compose.foundation.BorderStroke(1.dp, Error) else null
                    ) {
                        Box(Modifier.padding(Spacing.md), contentAlignment = Alignment.Center) {
                            Text("REDUCE (-)", fontWeight = FontWeight.Bold, color = if (!isAddition) Error else TextSecondary)
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) quantityText = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (e.g. Damage, Missing)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    if (qty <= 0) return@Button
                    onConfirm(if (isAddition) qty else -qty, reason)
                },
                enabled = quantityText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = Shapes.pill
            ) {
                Text("APPLY ADJUSTMENT", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatchDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: (BatchEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var batchNumber by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("") }
    var mrpText by remember { mutableStateOf("") }
    var sellingPriceText by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") } // YYYY-MM-DD or similar simplified

    LaunchedEffect(selectedProduct) {
        selectedProduct?.let {
            mrpText = it.mrp.toString()
            sellingPriceText = it.sellingPrice.toString()
        }
    }

    val filteredResults = remember(searchQuery) {
        if (searchQuery.length < 2) emptyList()
        else products.filter { it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery) }.take(3)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("Receive New Stock", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                if (selectedProduct == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Product to Restock") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
                    )
                    filteredResults.forEach { prod ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { selectedProduct = prod },
                            color = SurfaceVariant,
                            shape = Shapes.medium
                        ) {
                            Row(modifier = Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prod.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("SKU: ${prod.sku}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary.copy(alpha = 0.05f),
                        shape = Shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(selectedProduct!!.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Text("Current Stock: ${selectedProduct!!.currentStock.toInt()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            IconButton(onClick = { selectedProduct = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(IconSizes.md))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) quantityText = it },
                        label = { Text("Quantity Received") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
                    )

                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedTextField(
                            value = mrpText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) mrpText = it },
                            label = { Text("MRP") },
                            modifier = Modifier.weight(1f),
                            shape = Shapes.medium
                        )
                        OutlinedTextField(
                            value = sellingPriceText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) sellingPriceText = it },
                            label = { Text("Sale Price") },
                            modifier = Modifier.weight(1f),
                            shape = Shapes.medium
                        )
                    }

                    Text("Expiry handling uses standard 6-month default for simulation if left blank.", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prod = selectedProduct ?: return@Button
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    if (qty <= 0) return@Button

                    val batch = BatchEntity(
                        id = UUID.randomUUID().toString(),
                        productId = prod.id,
                        storeId = prod.storeId,
                        batchNumber = batchNumber.ifBlank { "B-${System.currentTimeMillis().toString().takeLast(6)}" },
                        expiryDate = System.currentTimeMillis() + (180L * 24 * 3600 * 1000), // 6 months
                        mrp = mrpText.toDoubleOrNull() ?: prod.mrp,
                        sellingPrice = sellingPriceText.toDoubleOrNull() ?: prod.sellingPrice,
                        purchasePrice = prod.purchasePrice,
                        initialQty = qty,
                        remainingQty = qty
                    )
                    onConfirm(batch)
                },
                enabled = selectedProduct != null && quantityText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = Shapes.pill
            ) {
                Text("RECEIVE STOCK", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

