package com.example.retailpos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.VerificationStatus
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.ProductCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCameraScanner: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true) ||
                    it.brand.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = RetailBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PRODUCTS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = RetailTextPrimary)
                        Text("Manage your product catalogue", style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onNavigateToCameraScanner,
                        colors = ButtonDefaults.textButtonColors(contentColor = RetailPrimary)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SCAN & IDENTIFY", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToProductDetail("") },
                containerColor = RetailPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ADD PRODUCT") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search products, barcode or SKU", color = RetailTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RetailTextSecondary) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RetailPrimary,
                    unfocusedBorderColor = RetailBorderSubtle,
                    unfocusedContainerColor = RetailSurface,
                    focusedContainerColor = RetailSurface
                ),
                singleLine = true
            )

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("All Items") },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RetailPrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = null
                    )
                }
                // Optional: Add other filter chips here based on categories found in products
            }

            // Product List
            if (products.isEmpty()) {
                // Empty state: No products at all
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = RetailSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(48.dp), tint = RetailTextSecondary.copy(alpha = 0.3f))
                            }
                        }
                        Text("No products yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = RetailTextPrimary)
                        Text("Add products to start building your catalogue.", style = MaterialTheme.typography.bodyMedium, color = RetailTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { onNavigateToProductDetail("") }, shape = RoundedCornerShape(12.dp)) {
                                Text("ADD PRODUCT")
                            }
                            Button(onClick = onNavigateToCameraScanner, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = RetailPrimary)) {
                                Text("SCAN & IDENTIFY")
                            }
                        }
                    }
                }
            } else if (filteredProducts.isEmpty()) {
                // Empty state: Search results empty
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = RetailTextSecondary.copy(alpha = 0.3f))
                        Text("No products found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)
                        Text("Try another name, barcode or SKU.", style = MaterialTheme.typography.bodyMedium, color = RetailTextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onNavigateToProductDetail(product.barcode) }
                        )
                    }
                }
            }
        }
    }
}
