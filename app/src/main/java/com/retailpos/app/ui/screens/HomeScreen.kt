package com.retailpos.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.retailpos.app.ExpenseActivity
import com.retailpos.app.PurchaseActivity
import com.retailpos.app.ReturnActivity
import com.retailpos.app.StaffGateActivity
import com.retailpos.app.core.payment.PaymentSummaryRules
import com.retailpos.app.core.permissions.NavigationPermissionRules
import com.retailpos.app.core.reconciliation.DayEndReconciliationRules
import com.retailpos.app.core.staff.StaffRole
import com.retailpos.app.core.staff.StaffSessionStore
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.SaleDao
import com.retailpos.app.ui.components.AiInsight
import com.retailpos.app.ui.components.SectionHeader
import com.retailpos.app.ui.components.StatusPill
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun HomeScreen(
    onNewBill: () -> Unit,
    onNavigate: (String) -> Unit,
    storeId: String = "local-store",
    saleDao: SaleDao? = null
) {
    val context = LocalContext.current
    val staffRole = StaffSessionStore.current()?.role ?: StaffRole.CASHIER
    val database = remember(context) { RetailDatabase.get(context) }
    val actualSaleDao = saleDao ?: database.saleDao()
    val quickActions = buildList {
        if (NavigationPermissionRules.canOpenProducts(staffRole)) add("products" to (Icons.Default.Storefront to "Products"))
        if (NavigationPermissionRules.canOpenInventory(staffRole)) add("inventory" to (Icons.Default.Inventory2 to "Inventory"))
        if (NavigationPermissionRules.canOpenInventory(staffRole)) add("purchases" to (Icons.Default.ShoppingCart to "Purchases"))
        add("customers" to (Icons.Default.Person to "Customers"))
        if (NavigationPermissionRules.canOpenAnalytics(staffRole)) add("analytics" to (Icons.Default.Analytics to "Analytics"))
        if (NavigationPermissionRules.canOpenSettings(staffRole)) add("settings" to (Icons.Default.Settings to "Settings"))
    }

    val today = LocalDate.now()
    val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val metricsState by produceState<TodayMetrics?>(initialValue = null, actualSaleDao, database, start, end) {
        val payments = PaymentSummaryRules.normalize(actualSaleDao.getPaymentSummary(storeId, start, end)).associateBy { it.paymentMethod.uppercase() }
        val total = actualSaleDao.getSalesTotal(storeId, start, end)
        value = TodayMetrics(
            totalSales = total,
            billCount = actualSaleDao.getSalesCount(storeId, start, end),
            itemsSold = actualSaleDao.getItemsSold(storeId, start, end),
            cash = payments["CASH"]?.total ?: 0.0,
            upi = payments["UPI"]?.total ?: 0.0,
            card = payments["CARD"]?.total ?: 0.0,
            credit = payments["CREDIT"]?.total ?: 0.0,
            cogs = actualSaleDao.getCogsTotal(storeId, start, end),
            expenses = database.expenseDao().totalBetween(storeId, start, end)
        )
    }
    val metrics = metricsState ?: TodayMetrics(0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    var countedCash by remember { mutableStateOf("") }

    fun switchCashier() {
        StaffSessionStore.clear()
        context.startActivity(Intent(context, StaffGateActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun openQuickAction(route: String) {
        when (route) {
            "purchases" -> if (NavigationPermissionRules.canOpenInventory(staffRole)) context.startActivity(Intent(context, PurchaseActivity::class.java))
            else -> onNavigate(route)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Overview", fontWeight = FontWeight.SemiBold); Text("Main store", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                actions = { StatusPill("Ready"); TextButton(onClick = ::switchCashier) { Text("Switch") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TODAY'S SALES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("₹${money(metrics.totalSales)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text("${metrics.billCount} bills · ${fmt(metrics.itemsSold)} items")
                        Spacer(Modifier.height(2.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cash ₹${money(metrics.cash)}"); Text("UPI ₹${money(metrics.upi)}") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Card ₹${money(metrics.card)}"); Text("Khata ₹${money(metrics.credit)}") }
                    }
                }
            }
            item {
                Button(onClick = onNewBill, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null); Spacer(Modifier.width(10.dp)); Text("New sale", fontWeight = FontWeight.Bold)
                }
            }
            item { SectionHeader("Needs attention") }
            item {
                OutlinedButton(onClick = { if (NavigationPermissionRules.canOpenInventory(staffRole)) onNavigate("inventory") }, enabled = NavigationPermissionRules.canOpenInventory(staffRole), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Inventory2, contentDescription = null); Spacer(Modifier.width(10.dp)); Text("18 low-stock products", modifier = Modifier.weight(1f)); Text("Review")
                }
            }
            item {
                OutlinedButton(onClick = { onNavigate("customers") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Person, contentDescription = null); Spacer(Modifier.width(10.dp)); Text("₹4,800 customer credit overdue", modifier = Modifier.weight(1f)); Text("Review")
                }
            }
            item {
                AiInsight("Milk, biscuits and cooking oil are moving faster than usual. Consider reviewing reorder quantities before the evening rush.", "Review inventory") { onNavigate("inventory") }
            }
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Day-end cash", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("Expected ₹${money(metrics.cash)}", style = MaterialTheme.typography.labelMedium) }
                        OutlinedTextField(value = countedCash, onValueChange = { countedCash = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cash counted") })
                        val counted = countedCash.replace(',', '.').toDoubleOrNull()
                        if (counted != null && counted >= 0.0) {
                            val difference = DayEndReconciliationRules.cashDifference(metrics.cash, counted)
                            Text("Difference ₹${money(difference)}", fontWeight = FontWeight.Bold, color = if (kotlin.math.abs(difference) < 0.005) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item { SectionHeader("Quick access") }
            items(quickActions.size) { index ->
                val (route, action) = quickActions[index]
                OutlinedButton(onClick = { openQuickAction(route) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Icon(action.first, contentDescription = null); Spacer(Modifier.width(12.dp)); Text(action.second, modifier = Modifier.weight(1f)); Text("›")
                }
            }
            if (NavigationPermissionRules.canProcessReturns(staffRole)) item { OutlinedButton(onClick = { context.startActivity(Intent(context, ReturnActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Returns & refunds") } }
            if (NavigationPermissionRules.canManageExpenses(staffRole)) item { OutlinedButton(onClick = { context.startActivity(Intent(context, ExpenseActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Expenses") } }
        }
    }
}

private data class TodayMetrics(
    val totalSales: Double, val billCount: Int, val itemsSold: Double, val cash: Double, val upi: Double, val card: Double, val credit: Double, val cogs: Double, val expenses: Double
) {
    val grossProfit: Double get() = totalSales - cogs
    val operatingResult: Double get() = grossProfit - expenses
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
