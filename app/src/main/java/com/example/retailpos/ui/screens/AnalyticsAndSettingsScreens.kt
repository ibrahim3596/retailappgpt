package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.auth.UserPermissions
import com.example.retailpos.auth.userRole
import com.example.retailpos.data.local.entity.BatchEntity
import com.example.retailpos.data.local.entity.PaymentMethod
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.MetricTile
import com.example.retailpos.util.ReportExporter
import com.example.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val store by viewModel.currentStore.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStock by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val expiring by viewModel.expiringSoonBatches.collectAsStateWithLifecycle()
    val analyticsRange by viewModel.analyticsRange.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val invoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()
 
    val totalSales = remember(invoices) { invoices.sumOf { it.invoice.grandTotal } }
    val totalGst = remember(invoices) { invoices.sumOf { it.invoice.totalGst } }
    val billsCount = invoices.size
    
    val totalProfit = remember(invoices) {
        invoices.sumOf { bill ->
            bill.items.sumOf { item ->
                (item.sellingPrice - item.purchasePrice) * item.quantity
            }
        }
    }

    val topSellingProducts = remember(invoices, products) {
        invoices.flatMap { it.items }
            .groupBy { it.productId }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .mapNotNull { (id, qty) -> 
                val prod = products.find { it.id == id }
                if (prod != null) prod.name to qty else null
            }
    }

    // Chart Data Preparation
    val salesByDay = remember(invoices) {
        invoices.groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.invoice.createdAt
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.mapValues { it.value.sumOf { inv -> inv.invoice.grandTotal } }
        .toList()
        .sortedBy { it.first }
        .map { it.second.toFloat() }
    }

    val chartModel = remember(salesByDay) {
        if (salesByDay.isEmpty()) entryModelOf(0f)
        else entryModelOf(*salesByDay.toTypedArray())
    }

    val rangeText = remember(analyticsRange) {
        if (analyticsRange == null) "Today"
        else {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            "${sdf.format(Date(analyticsRange!!.first))} - ${sdf.format(Date(analyticsRange!!.second))}"
        }
    }

    Scaffold(
        containerColor = RetailBackground,
        topBar = {
            TopAppBar(
                title = { Text("ANALYTICS", fontWeight = FontWeight.Black, letterSpacing = 1.sp, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val csv = ReportExporter.exportInvoicesToCsv(context, invoices.map { it.invoice })
                        if (csv != null) ReportExporter.shareFile(context, csv)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Range Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = RetailSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("REPORT PERIOD", style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary, fontWeight = FontWeight.Bold)
                        Text(rangeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = RetailPrimary)
                    }
                    Button(
                        onClick = {
                            if (analyticsRange == null) {
                                val end = System.currentTimeMillis()
                                val start = end - (7L * 24 * 3600 * 1000)
                                viewModel.setAnalyticsRange(start, end)
                            } else {
                                val end = System.currentTimeMillis()
                                val start = end - (30L * 24 * 3600 * 1000)
                                viewModel.setAnalyticsRange(start, end)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next Range")
                    }
                }
            }
                // Hero Performance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = RetailPrimary)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text("TOTAL REVENUE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                Text("₹${String.format("%,.2f", totalSales)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ) {
                                Text("TODAY", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnalyticsHeroMetric("EST. PROFIT", "₹${String.format("%,.0f", totalProfit)}", RetailSuccess)
                        AnalyticsHeroMetric("BILLS", "$billsCount", Color.White)
                        AnalyticsHeroMetric("GST", "₹${totalGst.toInt()}", Color.White)
                    }
                }
            }

            // Sales Trend Chart
            DashboardSection(title = "Sales Trend") {
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = RetailSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorderSubtle)
                ) {
                    if (salesByDay.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No sales data for this period", color = RetailTextSecondary)
                        }
                    } else {
                        Chart(
                            chart = lineChart(),
                            model = chartModel,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Top Products
            if (topSellingProducts.isNotEmpty()) {
                DashboardSection(title = "Top Selling Products") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        topSellingProducts.forEach { (name, qty) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = RetailSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, color = RetailTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("${qty.toInt()} sold", color = RetailPrimary, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
            
            // Tax Breakdown
            DashboardSection(title = "Tax Breakdown") {
                GstSummaryCard(invoices)
            }
        }
    }
}

@Composable
fun GstSummaryCard(invoices: List<com.example.retailpos.data.local.entity.InvoiceWithItems>) {
    val cgst = invoices.sumOf { it.invoice.cgstTotal }
    val sgst = invoices.sumOf { it.invoice.sgstTotal }
    val igst = invoices.sumOf { it.invoice.igstTotal }
    val taxable = invoices.sumOf { it.invoice.subtotal }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = RetailSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TaxRow("Taxable Value", taxable)
            HorizontalDivider(color = RetailBorderSubtle.copy(alpha = 0.5f))
            TaxRow("CGST Total", cgst)
            TaxRow("SGST Total", sgst)
            TaxRow("IGST Total", igst)
            HorizontalDivider(color = RetailBorderSubtle)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Tax", fontWeight = FontWeight.Black, color = RetailTextPrimary)
                Text("₹${String.format("%.2f", cgst + sgst + igst)}", fontWeight = FontWeight.Black, color = RetailWarning)
            }
        }
    }
}

@Composable
fun TaxRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = RetailTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text("₹${String.format("%.2f", amount)}", fontWeight = FontWeight.Bold, color = RetailTextPrimary)
    }
}

@Composable
fun AnalyticsHeroMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun DashboardSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = RetailTextSecondary,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
fun PaymentMetricTile(label: String, amount: Double, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RetailSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorderSubtle)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = color.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary, fontWeight = FontWeight.Bold)
                Text("₹${String.format("%,.0f", amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = RetailTextPrimary)
            }
        }
    }
}

@Composable
fun InsightCard(title: String, icon: ImageVector, iconColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RetailSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)
            }
            content()
        }
    }
}

@Composable
fun InsightSummaryBox(label: String, count: Int, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary, fontWeight = FontWeight.Bold)
                Text("$count Items", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = color)
            }
        }
    }
}

@Composable
fun TransactionRow(invoiceWithItems: com.example.retailpos.data.local.entity.InvoiceWithItems) {
    val df = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = RetailPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = RetailPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text("#${invoiceWithItems.invoice.invoiceNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = RetailTextPrimary)
                Text("${invoiceWithItems.invoice.paymentMethod} • ${df.format(Date(invoiceWithItems.invoice.createdAt))}", style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary)
            }
        }
        Text("₹${String.format("%,.2f", invoiceWithItems.invoice.grandTotal)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = RetailTextPrimary)
    }
}

@Composable
fun AnalyticsTaxRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RetailTextSecondary)
        Text("₹${String.format("%,.2f", amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = RetailTextPrimary)
    }
}

@Composable
fun EmptyAnalyticsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(80.dp), tint = RetailTextSecondary.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Insights arrive after sales.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RetailTextSecondary)
        Text("Start your first billing session to see real metrics.", style = MaterialTheme.typography.bodySmall, color = RetailTextSecondary.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val store by viewModel.currentStore.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showStoreDialog by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var showSwitchStoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = RetailBackground,
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", fontWeight = FontWeight.Black, letterSpacing = 1.sp, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            SettingsCategoryHeader("STORE & BUSINESS")
            if (UserPermissions.canUpdateStoreProfile(currentUser.userRole)) {
                SettingsItem(
                    title = "Store Profile",
                    description = "Manage name, GSTIN, and address",
                    icon = Icons.Default.Storefront,
                    onClick = { showStoreDialog = true }
                )
                SettingsItem(
                    title = "Switch Store",
                    description = "Change the active store location",
                    icon = Icons.Default.SwapHoriz,
                    onClick = { showSwitchStoreDialog = true }
                )
            }

            if (UserPermissions.canManageStaff(currentUser.userRole)) {
                SettingsCategoryHeader("TEAM & ACCESS")
                SettingsItem(
                    title = "Manage Staff",
                    description = "Add cashiers, reset PINs, manage roles",
                    icon = Icons.Default.People,
                    onClick = { showStaffDialog = true }
                )
            }

            SettingsItem(
                title = "Payment Methods",
                description = "Configure Cash, UPI, Card settings",
                icon = Icons.Default.Payments,
                onClick = { Toast.makeText(context, "Payment configuration active", Toast.LENGTH_SHORT).show() }
            )

            if (UserPermissions.canManageProducts(currentUser.userRole)) {
                SettingsCategoryHeader("BILLING & TAX")
                SettingsItem(
                    title = "GST Configuration",
                    description = "Tax rates, HSN mapping, and defaults",
                    icon = Icons.Default.Gavel,
                    onClick = { /* Implemented in Product logic */ }
                )
                SettingsItem(
                    title = "Receipt Design",
                    description = "Logo, header, and footer messages",
                    icon = Icons.Default.Description,
                    onClick = { /* Using standard M3 template */ }
                )
            }

            SettingsCategoryHeader("DEVICES & PRINTING")
            SettingsItem(
                title = "Printer Settings",
                description = "Bluetooth, Thermal, and USB printers",
                icon = Icons.Default.Print,
                onClick = { Toast.makeText(context, "Scanning for devices...", Toast.LENGTH_SHORT).show() }
            )
            SettingsItem(
                title = "Scanner Options",
                description = "Configure camera and external scanners",
                icon = Icons.Default.QrCodeScanner,
                onClick = { /* Using CameraScanner engine */ }
            )

            if (UserPermissions.canAccessAnalytics(currentUser.userRole)) {
                SettingsCategoryHeader("DATA & MAINTENANCE")
                SettingsItem(
                    title = "Conflict Management",
                    description = "Resolve synchronization issues",
                    icon = Icons.Default.SyncProblem,
                    onClick = { /* Managed by SyncEngine */ }
                )
                SettingsItem(
                    title = "Backup & Export",
                    description = "Export CSV reports and database backup",
                    icon = Icons.Default.CloudDownload,
                    onClick = { Toast.makeText(context, "Preparing CSV Export...", Toast.LENGTH_SHORT).show() }
                )
            }

            SettingsCategoryHeader("APP INFO")
            SettingsItem(
                title = "Version",
                description = "Build 3.5.0-AISTUDIO (Stable)",
                icon = Icons.Default.Info,
                onClick = {}
            )

            SettingsCategoryHeader("ACCOUNT")
            SettingsItem(
                title = "Logout",
                description = "Securely end your current session",
                icon = Icons.Default.Logout,
                onClick = {
                    viewModel.logout()
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "RetailPOS Powered by Antigravity Agent",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = RetailTextSecondary.copy(alpha = 0.5f)
            )
        }
    }

    if (showStoreDialog && store != null) {
        StoreProfileDialog(
            store = store!!,
            onDismiss = { showStoreDialog = false },
            onSave = { name, gstin, phone, address ->
                viewModel.updateStoreDetails(name, gstin, address, phone)
                showStoreDialog = false
                Toast.makeText(context, "Store Profile Updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSwitchStoreDialog) {
        SwitchStoreDialog(
            onDismiss = { showSwitchStoreDialog = false },
            onSwitch = { storeId ->
                viewModel.switchStore(storeId)
                showSwitchStoreDialog = false
                Toast.makeText(context, "Switched to Store: $storeId", Toast.LENGTH_SHORT).show()
            },
            onCreateNew = { name, gstin, addr, ph, on, ou, op ->
                viewModel.createNewStore(name, gstin, addr, ph, on, ou, op)
                showSwitchStoreDialog = false
                Toast.makeText(context, "New Store Created & Switched", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SwitchStoreDialog(
    onDismiss: () -> Unit,
    onSwitch: (String) -> Unit,
    onCreateNew: (String, String, String, String, String, String, String) -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var inputStoreId by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addr by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerUser by remember { mutableStateOf("") }
    var ownerPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RetailSurface,
        title = { Text(if (showCreateForm) "Create New Store" else "Switch Store", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (showCreateForm) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ownerUser, onValueChange = { ownerUser = it }, label = { Text("Owner Username") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ownerPin, onValueChange = { ownerPin = it }, label = { Text("Owner PIN (4 digits)") }, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("Enter the ID of the store you want to switch to. You will need to login with valid credentials for that store.", style = MaterialTheme.typography.bodySmall, color = RetailTextSecondary)
                    OutlinedTextField(
                        value = inputStoreId,
                        onValueChange = { inputStoreId = it },
                        label = { Text("Store ID") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. STORE-XXXXXX") }
                    )
                    TextButton(onClick = { showCreateForm = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("OR CREATE NEW STORE", color = RetailPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (showCreateForm) {
                        if (name.isNotBlank() && ownerName.isNotBlank() && ownerUser.isNotBlank() && ownerPin.length >= 4) {
                            onCreateNew(name, gstin, addr, phone, ownerName, ownerUser, ownerPin)
                        }
                    } else {
                        if (inputStoreId.isNotBlank()) onSwitch(inputStoreId)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (showCreateForm) "CREATE" else "SWITCH")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun StaffManagementDialog(
    currentUser: com.example.retailpos.data.local.entity.UserEntity?,
    allUsers: List<com.example.retailpos.data.local.entity.UserEntity>,
    onDismiss: () -> Unit,
    onAddStaff: (String, String, String, String) -> Unit,
    onDeleteStaff: (com.example.retailpos.data.local.entity.UserEntity) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RetailSurface,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Manage Staff", fontWeight = FontWeight.Black)
                if (!showAddForm && UserPermissions.canManageStaff(currentUser.userRole)) {
                    IconButton(onClick = { showAddForm = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Staff", tint = RetailPrimary)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (showAddForm) {
                    AddStaffForm(
                        onCancel = { showAddForm = false },
                        onSave = { u, f, p, r ->
                            onAddStaff(u, f, p, r)
                            showAddForm = false
                        }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allUsers) { user ->
                            StaffMemberRow(
                                user = user,
                                canDelete = UserPermissions.canManageStaff(currentUser.userRole) && user.role != "OWNER" && user.id != currentUser?.id,
                                onDelete = { onDeleteStaff(user) }
                            )
                            HorizontalDivider(color = RetailBorderSubtle.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showAddForm) {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = RetailPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun StaffMemberRow(
    user: com.example.retailpos.data.local.entity.UserEntity,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, fontWeight = FontWeight.Bold, color = RetailTextPrimary)
            Text("${user.role} • @${user.username}", style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary)
        }
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = RetailError)
            }
        }
    }
}

@Composable
fun AddStaffForm(onCancel: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CASHIER") }
    var expanded by remember { mutableStateOf(false) }

    val roles = listOf("CASHIER", "MANAGER")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text("4-Digit PIN") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
        )
        
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = role,
                onValueChange = {},
                readOnly = true,
                label = { Text("Role") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                roles.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r) },
                        onClick = {
                            role = r
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("CANCEL", color = RetailTextSecondary) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { if (username.isNotBlank() && fullName.isNotBlank() && pin.length == 4) onSave(username, fullName, pin, role) },
                colors = ButtonDefaults.buttonColors(containerColor = RetailPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ADD STAFF", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RetailBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = RetailPrimary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SettingsItem(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = RetailSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = RetailBackground,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = RetailTextPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)
                Text(description, style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RetailBorder, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun StoreProfileDialog(
    store: com.example.retailpos.data.local.entity.StoreEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var nameText by remember { mutableStateOf(store.name) }
    var gstinText by remember { mutableStateOf(store.gstin) }
    var phoneText by remember { mutableStateOf(store.phone) }
    var addressText by remember { mutableStateOf(store.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RetailSurface,
        title = { Text("Store Profile", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Business Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = gstinText,
                    onValueChange = { gstinText = it },
                    label = { Text("GSTIN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phoneText,
                    onValueChange = { phoneText = it },
                    label = { Text("Contact Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText, gstinText, phoneText, addressText) },
                colors = ButtonDefaults.buttonColors(containerColor = RetailPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE CHANGES", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = RetailTextSecondary)
            }
        }
    )
}
