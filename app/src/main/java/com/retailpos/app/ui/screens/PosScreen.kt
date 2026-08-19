package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.CartLine
import com.retailpos.app.data.ProductEntity
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    cart: List<CartLine>,
    searchResults: List<ProductEntity>,
    onSearchQueryChanged: (String) -> Unit,
    onAddProduct: (ProductEntity) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onBack: () -> Unit,
    onOpenScanner: () -> Unit,
    onCheckout: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val total = cart.sumOf { it.lineTotal }
    val itemCount = cart.sumOf { it.quantity }
    val showingSearch = query.trim().isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BILLING", fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } },
                actions = { IconButton(onClick = onOpenScanner) { Icon(Icons.Default.CameraAlt, contentDescription = "Scan barcode") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(56.dp)) { Text("HOLD BILL") }
                    Button(onClick = onCheckout, modifier = Modifier.weight(1.35f).height(56.dp), enabled = cart.isNotEmpty()) {
                        Text("CHECKOUT ${money(total)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; onSearchQueryChanged(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Search products, barcode or SKU") }
                    )
                    IconButton(onClick = onOpenScanner, modifier = Modifier.height(56.dp)) { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") }
                }
            }
            if (showingSearch) {
                item { Text("PRODUCTS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                if (searchResults.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(Modifier.height(8.dp))
                                Text("No products found", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text("Try a different name, SKU or barcode.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(searchResults, key = { it.id }) { product ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    if (product.brand.isNotBlank()) Text(product.brand, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${product.stock.clean()} ${product.unit} available • ${money(product.sellingPrice)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onAddProduct(product) }, enabled = product.stock > 0.0) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Add ${product.name}")
                                }
                            }
                        }
                    }
                }
            } else if (cart.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(20.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.ShoppingCart, contentDescription = null); Text("CART", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(12.dp))
                            Text("Your bill is empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Search for a product or scan its barcode to add it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                item { Text("CURRENT BILL", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                items(cart, key = { it.productId }) { line ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(line.name, fontWeight = FontWeight.Bold)
                                Text("${line.quantity.clean()} ${line.unit} × ${money(line.unitPrice)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(money(line.lineTotal), fontWeight = FontWeight.Black)
                            }
                            IconButton(onClick = { onRemoveFromCart(line.productId) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${line.name}") }
                        }
                    }
                }
                item { Text("${itemCount.clean()} items", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
