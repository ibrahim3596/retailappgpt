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
import com.retailpos.app.ui.components.MetricLine
import com.retailpos.app.ui.components.SectionHeader
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
    val today = LocalDate.now()
    val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val metricsState by produceState<TodayMetrics?>(initialValue = null, actualSaleDao, database, start, end) {
        val payments = PaymentSummaryRules.normalize(actualSaleDao.getPaymentSummary(storeId, start, end)).associateBy { it.paymentMethod.uppercase() }
        val sales = actualSaleDao.getSalesTotal(storeId, start, end)
        val cogs = actualSaleDao.getCogsTotal(storeId, start, end)
        val expenses = database.expenseDao().totalBetween(storeId, start, end)
        val outstandingReceivables = database.khataDao().totalReceivables(storeId).coerceAtLeast(0.0)
        val outOfStock = database.productDao().getOutOfStockCount(storeId)
        val lowStock = database.productDao().getLowStockCount(storeId)
        value = TodayMetrics(
            totalSales = sales,
            billCount = actualSaleDao.getSalesCount(storeId, start, end),
            itemsSold = actualSaleDao.getItemsSold(storeId, start, end),
            cash = payments["CASH"]?.total ?: 0.0,
            upi = payments["UPI"]?.total ?: 0.0,
            card = payments["CARD"]?.total ?: 0.0,
            credit = payments["CREDIT"]?.total ?: 0.0,
            cogs = cogs,
            expenses = expenses,
            outstandingReceivables = outstandingReceivables,
            outOfStock = outOfStock,
            lowStock = lowStock
        )
    }
    val metrics = metricsState ?: TodayMetrics(
        totalSales = 0.0,
        billCount = 0,
        itemsSold = 0.0,
        cash = 0.0,
        upi = 0.0,
        card = 0.0,
        credit = 0.0,
        cogs = 0.0,
        expenses = 0.0,
        outstandingReceivables = 0.0,
        outOfStock = 0,
        lowStock = 0
    )
    var countedCash by remember { mutableStateOf("") }

    fun switchCashier() {
        StaffSessionStore.clear()
        context.startActivity(Intent(context, StaffGateActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openQuickAction(route: String) {
        when (route) {
            "purchases" -> if (NavigationPermissionRules.canOpenInventory(staffRole)) context.startActivity(Intent(context, PurchaseActivity::class.java))
            else -> onNavigate(route)
        }
    }

    val quickActions = buildList {
        if (NavigationPermissionRules.canOpenProducts(staffRole)) add("products" to (Icons.Default.Storefront to "Products"))
        if (NavigationPermissionRules.canOpenInventory(staffRole)) add("inventory" to (Icons.Default.Inventory2 to "Inventory"))
        if (NavigationPermissionRules.canOpenInventory(staffRole)) add("purchases" to (Icons.Default.ShoppingCart to "Purchases"))
        add("customers" to (Icons.Default.Person to "Customers"))
        if (NavigationPermissionRules.canOpenAnalytics(staffRole)) add("analytics" to (Icons.Default.Analytics to "Analytics"))
        if (NavigationPermissionRules.canOpenSettings(staffRole)) add("settings" to (Icons.Default.Settings to "Settings"))
    }

    val grossProfit = (metrics.totalSales - metrics.cogs).coerceAtLeast(0.0)
    val marginPercent = if (metrics.totalSales > 0.0) grossProfit / metrics.totalSales * 100.0 else 0.0
    val priorityText = when {
        metrics.outOfStock > 0 -> "${metrics.outOfStock} product${if (metrics.outOfStock == 1) "" else "s"} out of stock. Replenish before the next sale."
        metrics.lowStock > 0 -> "${metrics.lowStock} product${if (metrics.lowStock == 1) "" else "s"} running low on stock. Review reorder needs."
        metrics.outstandingReceivables > 0.0 -> "₹${money(metrics.outstandingReceivables)} is currently outstanding in Khata."
        metrics.totalSales > 0.0 && marginPercent < 15.0 -> "Today's gross margin is ${fmtPercent(marginPercent)}. Review low-margin products in Analytics."
        else -> "No urgent store-level issue detected from today's local data."
    }
    val priorityRoute = when {
        metrics.outOfStock > 0 || metrics.lowStock > 0 -> "inventory"
        metrics.outstandingReceivables > 0.0 -> "customers"
        metrics.totalSales > 0.0 && marginPercent < 15.0 -> "analytics"
        else -> null
    }
    val priorityActionAllowed = when (priorityRoute) {
        "inventory" -> NavigationPermissionRules.canOpenInventory(staffRole)
        "analytics" -> NavigationPermissionRules.canOpenAnalytics(staffRole)
        "customers" -> true
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Main counter · Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { TextButton(onClick = ::switchCashier) { Text("SWITCH") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Today's sales", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${money(metrics.totalSales)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("${metrics.billCount} bills · ${fmt(metrics.itemsSold)} items sold", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item {
                Button(onClick = onNewBill, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("NEW BILL", fontWeight = FontWeight.Bold)
                }
            }
            item {
                SectionHeader("Payment mix")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        MetricLine("Cash", "₹${money(metrics.cash)}")
                        MetricLine("UPI", "₹${money(metrics.upi)}")
                        MetricLine("Card", "₹${money(metrics.card)}")
                        MetricLine("Khata", "₹${money(metrics.credit)}")
                    }
                }
            }
            item {
                SectionHeader("Needs attention")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        MetricLine(
                            "Stock",
                            if (metrics.outOfStock > 0) "${metrics.outOfStock} out" else "${metrics.lowStock} low",
                            when {
                                metrics.outOfStock > 0 -> "Items unavailable for sale"
                                metrics.lowStock > 0 -> "Items at or below their low-stock threshold"
                                else -> "No low-stock items"
                            }
                        )
                        MetricLine(
                            "Khata receivables",
                            "₹${money(metrics.outstandingReceivables)}",
                            "Current outstanding customer credit"
                        )
                    }
                }
            }
            item {
                AiInsight(
                    priorityText,
                    if (priorityActionAllowed) {
                        when (priorityRoute) {
                            "inventory" -> "Open inventory"
                            "customers" -> "Open customers"
                            "analytics" -> "Open analytics"
                            else -> null
                        }
                    } else null,
                    onAction = {
                        if (priorityActionAllowed && priorityRoute != null) onNavigate(priorityRoute)
                    }
                )
            }
            if (NavigationPermissionRules.canOpenInventory(staffRole)) {
                item {
                    OutlinedButton(onClick = { context.startActivity(Intent(context, PurchaseActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("PURCHASE / RECEIVE STOCK")
                    }
                }
            }
            if (NavigationPermissionRules.canProcessReturns(staffRole)) {
                item {
                    OutlinedButton(onClick = { context.startActivity(Intent(context, ReturnActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("RETURNS / REFUNDS")
                    }
                }
            }
            if (NavigationPermissionRules.canManageExpenses(staffRole)) {
                item {
                    OutlinedButton(onClick = { context.startActivity(Intent(context, ExpenseActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("EXPENSES")
                    }
                }
            }
            item {
                SectionHeader("Day-end cash")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Expected cash · ₹${money(metrics.cash)}", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(value = countedCash, onValueChange = { countedCash = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cash counted") })
                        countedCash.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { counted ->
                            val difference = DayEndReconciliationRules.cashDifference(metrics.cash, counted)
                            Text("Difference · ₹${money(difference)}", fontWeight = FontWeight.SemiBold, color = if (kotlin.math.abs(difference) < 0.005) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item {
                SectionHeader("Quick access")
                quickActions.forEach { (route, action) ->
                    OutlinedButton(onClick = { openQuickAction(route) }, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                        Icon(action.first, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(action.second, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("›", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

private data class TodayMetrics(
    val totalSales: Double,
    val billCount: Int,
    val itemsSold: Double,
    val cash: Double,
    val upi: Double,
    val card: Double,
    val credit: Double,
    val cogs: Double,
    val expenses: Double,
    val outstandingReceivables: Double,
    val outOfStock: Int,
    val lowStock: Int
)

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
private fun fmtPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)
