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
import com.example.retailpos.data.local.entity.PaymentMethod
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.PurchaseEntity
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
    onNavigateToSuppliers: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReturns: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val store by viewModel.currentStore.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val lowStockItems by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val outOfStockItems by viewModel.outOfStockProducts.collectAsStateWithLifecycle()
    val unresolvedConflicts by viewModel.unresolvedConflicts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val totalOutstandingCredit by viewModel.totalOutstandingCredit.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val recentPurchases by viewModel.recentPurchases.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.recentExpenses.collectAsStateWithLifecycle()

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

    val paymentBreakdown = remember(todayInvoices) {
        todayInvoices.groupBy { it.invoice.paymentMethod }
            .mapValues { (_, list) -> list.sumOf { it.invoice.grandTotal.toDouble() } }
    }

    val currentDate = remember {
        SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = store?.name ?: "RetailPOS Mart",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    if (unresolvedConflicts.isNotEmpty() || pendingSyncCount > 0) {
                        IconButton(onClick = onNavigateToSync) {
                            BadgedBox(badge = { Badge { Text((unresolvedConflicts.size + pendingSyncCount).toString()) } }) {
                                Icon(Icons.Default.SyncProblem, contentDescription = "Sync Issues", tint = Error)
                            }
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = SurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Settings", tint = Primary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
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
            // Metrics Summary Section - Row 1: Today's Performance
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Today's Performance",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
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
                            iconColor = Success,
                            modifier = Modifier.weight(1.1f)
                        )
                        MetricTile(
                            label = "Bills",
                            value = "${todayInvoices.size}",
                            icon = Icons.Default.ReceiptLong,
                            iconColor = Primary,
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

            // Metrics Summary Section - Row 2: Key Indicators
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Key Indicators",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            label = "Outstanding Khata",
                            value = "₹${String.format("%.0f", totalOutstandingCredit)}",
                            icon = Icons.Default.AccountBalance,
                            iconColor = if (totalOutstandingCredit > 0) Error else Success,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Low Stock",
                            value = "${lowStockItems.size}",
                            icon = Icons.Default.WarningAmber,
                            iconColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Out of Stock",
                            value = "${outOfStockItems.size}",
                            icon = Icons.Default.Inventory2,
                            iconColor = Error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Metrics Summary Section - Row 3: Payment Breakdown (if any sales today)
            if (todayInvoices.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Payment Breakdown",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            paymentBreakdown.entries.forEach { entry ->
                                val (method, amount) = entry
                                val (icon, color) = when (method) {
                                    PaymentMethod.CASH -> Icons.Default.AttachMoney to Color(0xFF059669)
                                    PaymentMethod.UPI -> Icons.Default.QrCode to Color(0xFF7C3AED)
                                    PaymentMethod.CARD -> Icons.Default.CreditCard to Color(0xFF2563EB)
                                    PaymentMethod.CREDIT -> Icons.Default.AccountBalance to Color(0xFFDC2626)
                                    else -> Icons.Default.Payments to Primary
                                }
                                MetricTile(
                                    label = method.name,
                                    value = "₹${String.format("%.0f", amount)}",
                                    icon = icon,
                                    iconColor = color,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
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
                        color = TextSecondary,
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
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickNavButton(
                            title = "Suppliers",
                            icon = Icons.Default.Business,
                            onClick = onNavigateToSuppliers,
                            modifier = Modifier.weight(1f)
                        )
                        QuickNavButton(
                            title = "Purchases",
                            icon = Icons.Default.ShoppingCart,
                            onClick = onNavigateToPurchases,
                            modifier = Modifier.weight(1f)
                        )
                        QuickNavButton(
                            title = "Expenses",
                            icon = Icons.Default.AccountBalanceWallet,
                            onClick = onNavigateToExpenses,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (UserPermissions.canAccessAnalytics(currentUser.userRole)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickNavButton(
                                title = "Returns",
                                icon = Icons.Default.AssignmentReturn,
                                onClick = onNavigateToReturns,
                                modifier = Modifier.weight(1f)
                            )
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

            // Alerts / Notifications section - Low Stock
            if (lowStockItems.isNotEmpty()) {
                item {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WarningAmber, contentDescription = "Warning", tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Low Stock Alerts (${lowStockItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            lowStockItems.take(3).forEach { product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(product.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text(
                                        "${product.currentStock.toInt()} ${product.unit} left",
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (lowStockItems.size > 3) {
                                Text(
                                    "And ${lowStockItems.size - 3} more...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Alerts / Notifications section - Out of Stock
            if (outOfStockItems.isNotEmpty()) {
                item {
                    Surface(
                        color = Error.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, contentDescription = "Out of Stock", tint = Error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Out of Stock (${outOfStockItems.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Error
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            outOfStockItems.take(3).forEach { product ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(product.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text(
                                        "0 ${product.unit} left",
                                        color = Error,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (outOfStockItems.size > 3) {
                                Text(
                                    "And ${outOfStockItems.size - 3} more...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pending Sync Alert
            if (pendingSyncCount > 0 || unresolvedConflicts.isNotEmpty()) {
                item {
                    Surface(
                        color = Primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudOff, contentDescription = "Sync Pending", tint = Primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pending Sync (${pendingSyncCount + unresolvedConflicts.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap to resolve conflicts or retry sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Recent Purchases Section
            if (recentPurchases.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Purchases",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                }
                items(recentPurchases) { purchase ->
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
                                    text = purchase.invoiceNumber,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${purchase.supplierName} • recent order",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "₹${String.format("%.0f", purchase.totalAmount)}",
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2563EB),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            // Recent Expenses Section
            if (recentExpenses.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                }
                items(recentExpenses) { expense ->
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
                                    text = expense.category,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${expense.paymentMethod} • ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.date))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "₹${String.format("%.0f", expense.amount)}",
                                fontWeight = FontWeight.Black,
                                color = Error,
                                style = MaterialTheme.typography.titleLarge
                            )
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
                        color = TextPrimary
                    )
                    if (invoices.isNotEmpty() && UserPermissions.canAccessAnalytics(currentUser.userRole)) {
                        TextButton(onClick = onNavigateToAnalytics) {
                            Text("View All", color = Primary, fontWeight = FontWeight.Bold)
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
                            color = SurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.History, contentDescription = "历史记录", modifier = Modifier.size(32.dp), tint = TextSecondary.copy(alpha = 0.5f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No transactions today",
                            color = TextSecondary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Start your first bill to see activity here",
                            color = TextSecondary.copy(alpha = 0.7f),
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
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${invoice.invoice.customerName} • ${invoice.invoice.paymentMethod.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "₹${String.format("%.0f", invoice.invoice.grandTotal)}",
                                fontWeight = FontWeight.Black,
                                color = Primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
    }
}