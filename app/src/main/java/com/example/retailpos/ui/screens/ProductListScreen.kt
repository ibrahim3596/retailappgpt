package com.example.retailpos.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.VerificationStatus
import com.example.retailpos.engine.barcode.BarcodeNormalizer
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.ProductCard
import com.example.ui.theme.*
import com.example.ui.theme.Spacing
import com.example.ui.theme.IconSizes
import com.example.ui.theme.Shapes

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
        else {
            val normalized = BarcodeNormalizer.normalize(searchQuery).canonicalGtin
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.barcode == searchQuery ||
                        it.normalizedBarcode == normalized ||
                        it.sku.contains(searchQuery, ignoreCase = true) ||
                        it.brand.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PRODUCTS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Manage your product catalogue", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
                        colors = ButtonDefaults.textButtonColors(contentColor = Primary)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan & Identify", modifier = Modifier.size(IconSizes.sm))
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("SCAN & IDENTIFY", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToProductDetail("") },
                containerColor = Primary,
                contentColor = Color.White,
                shape = Shapes.pill,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Product") },
                text = { Text("ADD PRODUCT", fontWeight = FontWeight.Bold) }
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search products, barcode or SKU", color = TextSecondary) },
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

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(bottom = Spacing.sm)
            ) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("All Items") },
                        shape = Shapes.pill,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = SurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Inventory, contentDescription = "Inventory", modifier = Modifier.size(IconSizes.xxxl), tint = TextTertiary.copy(alpha = 0.3f))
                            }
                        }
                        Text("No products yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text("Add products to start building your catalogue.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = Spacing.xxxl))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                            OutlinedButton(onClick = { onNavigateToProductDetail("") }, shape = Shapes.pill) {
                                Text("ADD PRODUCT")
                            }
                            Button(onClick = onNavigateToCameraScanner, shape = Shapes.pill, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                                Text("SCAN & IDENTIFY")
                            }
                        }
                    }
                }
            } else if (filteredProducts.isEmpty()) {
                // Empty state: Search results empty
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Icon(Icons.Default.SearchOff, contentDescription = "No search results", modifier = Modifier.size(64.dp), tint = TextTertiary.copy(alpha = 0.3f))
                        Text("No products found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Try another name, barcode or SKU.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
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
