package com.example.retailpos.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.auth.UserPermissions
import com.example.retailpos.auth.userRole
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.StoreEntity
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.MetricTile
import com.example.retailpos.ui.components.PrimaryActionCard
import com.example.retailpos.ui.components.QuickNavButton
import com.example.ui.theme.*

import com.example.retailpos.ui.components.NewBillButton
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val store by viewModel.currentStore.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val lowStockItems by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val unresolvedConflicts by viewModel.unresolvedConflicts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val todayInvoices = remember(invoices) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis
        
        invoices.filter { it.invoice.createdAt >= startOfDay && it.invoice.createdAt < endOfDay }
    }

    val todaySales = remember(todayInvoices) {
        todayInvoices.sumOf { it.invoice.grandTotal.toDouble() }
    }
    
    val itemsSold = remember(todayInvoices) {
        todayInvoices.sumOf { invoiceWithItems -> invoiceWithItems.items.sumOf { item -> item.quantity.toDouble() } }
    }

    val currentDate = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    Scaffold(
        containerColor = RetailBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = store?.name ?: "RetailPOS Mart",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = RetailTextPrimary
                        )
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = RetailTextSecondary
                        )
                    }
                },
                actions = {
                    if (unresolvedConflicts.isNotEmpty()) {
                        IconButton(onClick = onNavigateToSync) {
                            BadgedBox(badge = { Badge { Text(unresolvedConflicts.size.toString()) } }) {
                                Icon(Icons.Default.SyncProblem, contentDescription = "Conflicts", tint = RetailError)
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = RetailSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Settings", tint = RetailPrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Metrics Summary Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Today's Performance",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = RetailTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            label = "Revenue",
                            value = "₹${String.format("%.0f", todaySales)}",
                            icon = Icons.Default.Payments,
                            iconColor = RetailSuccess,
                            modifier = Modifier.weight(1.1f)
                        )
                        MetricTile(
                            label = "Bills",
                            value = "${todayInvoices.size}",
                            icon = Icons.Default.ReceiptLong,
                            iconColor = RetailPrimary,
                            modifier = Modifier.weight(0.9f)
                        )
                        MetricTile(
                            label = "Items",
                            value = "${itemsSold.toInt()}",
                            icon = Icons.Default.Inventory,
                            iconColor = Color(0xFFD97706),
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }

            // PRIMARY ACTION: NEW BILL
            item {
                NewBillButton(onClick = onNavigateToPos)
            }

            // Quick Access Navigation Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Store Management",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = RetailTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickNavButton(
                            title = "Products",
                            icon = Icons.Default.Inventory2,
                            onClick = onNavigateToProducts,
                            modifier = Modifier.weight(1f)
                        )
                        if (UserPermissions.canAccessInventory(currentUser.userRole)) {
                            QuickNavButton(
                                title = "Inventory",
                                icon = Icons.Default.Warehouse,
                                onClick = onNavigateToInventory,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        QuickNavButton(
                            title = "Customers",
                            icon = Icons.Default.Group,
                            onClick = onNavigateToCustomers,
                            modifier = Modifier.weight(1f)
                        )
                        if (UserPermissions.canAccessAnalytics(currentUser.userRole)) {
                            QuickNavButton(
                                title = "Analytics",
                                icon = Icons.Default.InsertChartOutlined,
                                onClick = onNavigateToAnalytics,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Alerts / Notifications section
            if (lowStockItems.isNotEmpty()) {
                item {
                    Surface(
                        color = RetailError.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RetailError.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RetailError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Low Stock Alerts",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = RetailError
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            lowStockItems.take(2).forEach { product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(product.name, style = MaterialTheme.typography.bodyMedium, color = RetailTextPrimary)
                                    Text(
                                        "${product.currentStock.toInt()} ${product.unit} left",
                                        color = RetailError,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Activity Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = RetailTextPrimary
                    )
                    if (invoices.isNotEmpty() && UserPermissions.canAccessAnalytics(currentUser.userRole)) {
                        TextButton(onClick = onNavigateToAnalytics) {
                            Text("View All", color = RetailPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (invoices.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = RetailSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(32.dp), tint = RetailTextSecondary.copy(alpha = 0.5f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No transactions today",
                            color = RetailTextSecondary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Start your first bill to see activity here",
                            color = RetailTextSecondary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                items(invoices.take(3)) { invoice ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = invoice.invoice.invoiceNumber,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = RetailTextPrimary
                                )
                                Text(
                                    text = "${invoice.invoice.customerName} • ${invoice.invoice.paymentMethod.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RetailTextSecondary
                                )
                            }
                            Text(
                                text = "₹${String.format("%.0f", invoice.invoice.grandTotal)}",
                                fontWeight = FontWeight.Black,
                                color = RetailPrimary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
