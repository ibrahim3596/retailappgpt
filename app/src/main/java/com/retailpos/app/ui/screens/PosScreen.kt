package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.payment.ActiveCartRecovery
import com.retailpos.app.core.pos.QuickAddProduct
import com.retailpos.app.core.pos.QuickAddRules
import com.retailpos.app.core.pos.RecentProductRules
import com.retailpos.app.data.CartLine
import com.retailpos.app.data.FavoriteProductEntity
import com.retailpos.app.data.HeldBillStore
import com.retailpos.app.data.ProductEntity
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.ui.components.AiInsight
import com.retailpos.app.ui.components.PosQuickAddSection
import com.retailpos.app.ui.components.StatusPill
import com.retailpos.app.ui.components.VoiceBillingButton
import kotlinx.coroutines.launch
import java.util.Locale

private const val LOCAL_STORE_ID = "local-store"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    cart: List<CartLine>, searchResults: List<ProductEntity>, recentlySold: List<QuickAddProduct> = emptyList(), favorites: List<QuickAddProduct> = emptyList(),
    onSearchQueryChanged: (String) -> Unit, onAddProduct: (ProductEntity) -> Unit, onQuickAdd: (QuickAddProduct) -> Unit = {}, onToggleFavorite: (QuickAddProduct) -> Unit = {}, isFavorite: (QuickAddProduct) -> Boolean = { false },
    onVoiceInput: (String) -> Unit, onVoiceError: (String) -> Unit, onSetCartQuantity: (CartLine, Double) -> Unit, onRemoveFromCart: (String) -> Unit,
    onBack: () -> Unit, onOpenScanner: () -> Unit, onCheckout: () -> Unit, onHoldBill: () -> Unit = { if (cart.isNotEmpty()) HeldBillStore.hold(cart) }, onOpenHeldBills: () -> Unit = {}, onClearBill: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { RetailDatabase.get(context) }
    val favoriteIds by database.favoriteProductDao().observeIds(LOCAL_STORE_ID).collectAsState(initial = emptyList())
    var persistedFavorites by remember { mutableStateOf<List<QuickAddProduct>>(emptyList()) }
    var persistedRecentlySold by remember { mutableStateOf<List<QuickAddProduct>>(emptyList()) }
    var recoveryIssues by remember { mutableStateOf(emptyList<com.retailpos.app.core.payment.ActiveCartRecoveryIssue>()) }
    var query by remember { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(favoriteIds) { persistedFavorites = QuickAddRules.filterAddable(favoriteIds.mapNotNull { id -> database.productDao().getById(id, LOCAL_STORE_ID)?.toQuickAddProduct() }) }
    LaunchedEffect(Unit) {
        val recentSales = database.saleDao().getRecentSales(LOCAL_STORE_ID, 12)
        val recentLines = recentSales.map { sale -> database.saleDao().getSaleLines(sale.id).mapNotNull { line -> database.productDao().getById(line.productId, LOCAL_STORE_ID)?.toQuickAddProduct() } }
        persistedRecentlySold = QuickAddRules.filterAddable(RecentProductRules.fromSaleLines(recentLines))
    }
    LaunchedEffect(cart) {
        recoveryIssues = if (cart.isEmpty()) emptyList() else {
            val products = cart.mapNotNull { line -> database.productDao().getById(line.productId, LOCAL_STORE_ID)?.let { line.productId to it } }.toMap()
            ActiveCartRecovery.validate(cart, products)
        }
    }

    fun togglePersistentFavorite(product: QuickAddProduct) {
        scope.launch {
            if (database.favoriteProductDao().isFavorite(LOCAL_STORE_ID, product.productId)) database.favoriteProductDao().remove(LOCAL_STORE_ID, product.productId)
            else database.favoriteProductDao().add(FavoriteProductEntity(LOCAL_STORE_ID, product.productId, System.currentTimeMillis()))
            onToggleFavorite(product)
        }
    }

    val visibleFavorites = if (persistedFavorites.isNotEmpty()) persistedFavorites else favorites
    val visibleRecentlySold = if (persistedRecentlySold.isNotEmpty()) persistedRecentlySold else recentlySold
    val total = cart.sumOf { it.lineTotal }
    val itemCount = cart.sumOf { it.quantity }
    val showingSearch = query.trim().isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Sell", style = MaterialTheme.typography.titleLarge); Text("Counter 01", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home") } },
                actions = {
                    StatusPill("Ready", true)
                    VoiceBillingButton(onTranscript = onVoiceInput, onError = onVoiceError)
                    IconButton(onClick = onOpenHeldBills) { Icon(Icons.Default.MoreHoriz, contentDescription = "More selling options") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search name, barcode or SKU") },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = ""; onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                                IconButton(onClick = onOpenScanner) { Icon(Icons.Default.CameraAlt, contentDescription = "Scan barcode") }
                            }
                        }
                    )
                }
                if (recoveryIssues.isNotEmpty()) item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Bill needs attention", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer); recoveryIssues.forEach { Text("• ${ActiveCartRecovery.message(it)}", color = MaterialTheme.colorScheme.onErrorContainer) }; TextButton(onClick = onClearBill) { Text("Clear affected bill") } } }
                }
                if (!showingSearch) {
                    item { PosQuickAddSection(title = "Recently sold", products = visibleRecentlySold, onAdd = onQuickAdd, onToggleFavorite = ::togglePersistentFavorite, isFavorite = { favoriteIds.contains(it.productId) }) }
                    item { PosQuickAddSection(title = "Favorites", products = visibleFavorites, onAdd = onQuickAdd, onToggleFavorite = ::togglePersistentFavorite, isFavorite = { favoriteIds.contains(it.productId) }) }
                    if (cart.isEmpty()) item { AiInsight("For a faster bill, scan a barcode or search by product name. You can also speak the order.") }
                }
                if (showingSearch) {
                    item { Text("Products", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                    if (searchResults.isEmpty()) item { Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Icon(Icons.Default.Search, contentDescription = null); Text("No products found", fontWeight = FontWeight.SemiBold); Text("Try another name, SKU or barcode.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                    else items(searchResults, key = { it.id }) { product -> Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); if (product.brand.isNotBlank()) Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${product.stock.clean()} ${product.unit}  ·  ${money(product.sellingPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick = { onAddProduct(product) }, enabled = product.stock > 0) { Icon(Icons.Default.AddShoppingCart, contentDescription = "Add ${product.name}") } } } }
                }
                if (!showingSearch && cart.isNotEmpty()) {
                    item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Current bill", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("${cart.size} lines", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    items(cart, key = { it.productId }) { line ->
                        var quantityText by remember(line.productId, line.quantity) { mutableStateOf(displayQuantity(line.quantity)) }
                        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 1.dp) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(line.name, fontWeight = FontWeight.SemiBold); Text("${displayQuantity(line.quantity)} ${line.unit} × ${money(line.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(money(line.lineTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); IconButton(onClick = { onRemoveFromCart(line.productId) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${line.name}") } }; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) { val step = quantityStep(line.unit); OutlinedButton(onClick = { onSetCartQuantity(line, line.quantity - step) }, enabled = line.quantity - step > 0, modifier = Modifier.width(50.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Remove, contentDescription = "Decrease quantity") }; OutlinedTextField(value = quantityText, onValueChange = { quantityText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, singleLine = true, modifier = Modifier.weight(1f), label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)); Button(onClick = { onSetCartQuantity(line, line.quantity + step) }, modifier = Modifier.width(50.dp), contentPadding = PaddingValues(0.dp)) { Text("Add") }; TextButton(onClick = { quantityText.replace(',', '.').toDoubleOrNull()?.let { onSetCartQuantity(line, it) } ?: run { quantityText = displayQuantity(line.quantity) } }) { Text("Set") } } } }
                    }
                }
            }
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(money(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; Text("${itemCount.clean()} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onHoldBill, enabled = cart.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Hold") }; OutlinedButton(onClick = { showClearConfirmation = true }, enabled = cart.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Clear") }; Button(onClick = onCheckout, enabled = cart.isNotEmpty() && recoveryIssues.isEmpty(), modifier = Modifier.weight(1.8f).height(54.dp)) { Text("Charge ${money(total)}", fontWeight = FontWeight.Bold) } } } }
        }
    }
    if (showClearConfirmation) AlertDialog(onDismissRequest = { showClearConfirmation = false }, title = { Text("Clear bill?") }, text = { Text("Remove all ${cart.size} product lines from the current bill? Hold it first if you may need it later.") }, confirmButton = { TextButton(onClick = { showClearConfirmation = false; onClearBill() }) { Text("Clear bill") } }, dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") } })
}

private fun ProductEntity.toQuickAddProduct(): QuickAddProduct = QuickAddProduct(id, name, brand, unit, sellingPrice, stock)
private fun quantityStep(unit: String): Double = when (unit.trim().lowercase()) { "kg", "kilo", "kilogram", "kilograms" -> 0.05; "l", "lt", "ltr", "litre", "liter", "litres", "liters" -> 0.05; "g", "gm", "gram", "grams" -> 50.0; "ml", "millilitre", "milliliter" -> 50.0; else -> 1.0 }
private fun displayQuantity(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
