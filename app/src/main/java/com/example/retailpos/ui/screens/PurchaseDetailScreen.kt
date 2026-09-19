package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.MetricTile
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    viewModel: MainViewModel,
    purchaseId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val purchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    val purchase = remember(purchases, purchaseId) {
        purchases.find { it.id == purchaseId }
    }

    val purchaseItems by viewModel.getPurchaseItems(purchaseId).collectAsStateWithLifecycle()

    val sdf = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(purchase?.invoiceNumber.ifEmpty { "Purchase Order" } ?: "Purchase Details",
                        fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
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
            purchase?.let { purchase ->
                // Header
                Column {
                    Text("PURCHASE ORDER", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, color = TextSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Summary Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        label = "Supplier",
                        value = purchase.supplierName,
                        icon = Icons.Default.Business,
                        iconColor = Primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Items",
                        value = "${purchaseItems.size}",
                        icon = Icons.Default.Inventory2,
                        iconColor = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Total",
                        value = "₹${String.format("%,.0f", purchase.totalAmount)}",
                        icon = Icons.Default.AttachMoney,
                        iconColor = Success,
                        modifier = Modifier.weight(1f)
                    )
                }

                // PO Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                ) {
                    Column(modifier = Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Primary.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = "PO", modifier = Modifier.size(20.dp), tint = Primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("ORDER DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PO Number", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(purchase.invoiceNumber.ifEmpty { "#${purchase.id.takeLast(6)}" }, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Date", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(sdf.format(Date(purchase.createdAt)), fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        if (purchase.notes.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Notes", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(purchase.notes, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Items List
                if (purchaseItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ITEMS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = TextSecondary, letterSpacing = 0.5.sp)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(purchaseItems) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = Shapes.card,
                                    colors = CardDefaults.cardColors(containerColor = Surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.productName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("Batch: ${item.batchNumber} • Expiry: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.expiryDate))}",
                                                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Qty: ${item.quantity.toInt()}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                                                Text("₹${String.format("%,.2f", item.purchasePrice)} each", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                                Text("₹${String.format("%,.2f", item.itemTotal)}", fontWeight = FontWeight.Black, color = Primary, style = MaterialTheme.typography.titleSmall)
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Selling: ₹${String.format("%,.2f", item.sellingPrice)} | MRP: ₹${String.format("%,.2f", item.mrp)}",
                                                style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty items
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.xxxl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = SurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Inventory2, contentDescription = "Items", modifier = Modifier.size(32.dp), tint = TextSecondary.copy(alpha = 0.2f))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No items in this purchase order", fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("Items will appear after receipt", style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(alpha = 0.7f))
                        }
                    }
                }

                // Totals
                if (purchaseItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                Text("₹${String.format("%,.2f", purchaseItems.sumOf { it.itemTotal })}", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (purchase.gstTotal > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("GST Total", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                    Text("₹${String.format("%,.2f", purchase.gstTotal)}", fontWeight = FontWeight.Bold, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            androidx.compose.foundation.layout.HorizontalDivider(color = OutlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL", fontWeight = FontWeight.Black, color = Primary, style = MaterialTheme.typography.titleMedium)
                                Text("₹${String.format("%,.2f", purchase.totalAmount)}", fontWeight = FontWeight.Black, color = Primary, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}