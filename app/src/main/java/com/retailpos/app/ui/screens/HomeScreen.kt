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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retailpos.app.ExpenseActivity
import com.retailpos.app.PurchaseActivity
import com.retailpos.app.ReturnActivity
import com.retailpos.app.StaffGateActivity
import com.retailpos.app.core.reconciliation.DayEndReconciliationRules
import com.retailpos.app.core.staff.StaffSessionStore
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.SaleDao
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
    val database = remember(context) { RetailDatabase.get(context) }
    val actualSaleDao = saleDao ?: database.saleDao()
    val quickActions = listOf(
        "products" to (Icons.Default.Storefront to "Products"),
        "inventory" to (Icons.Default.Inventory2 to "Inventory"),
        "purchases" to (Icons.Default.ShoppingCart to "Purchases"),
        "customers" to (Icons.Default.Person to "Customers"),
        "analytics" to (Icons.Default.Analytics to "Analytics"),
        "settings" to (Icons.Default.Settings to "Settings")
    )
    val today = LocalDate.now()
    val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val metricsState by produceState<TodayMetrics?>(initialValue = null, actualSaleDao, database, start, end) {
        val payments = actualSaleDao.getPaymentSummary(storeId, start, end).associateBy { it.paymentMethod.uppercase() }
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
    val metrics = metricsState ?: TodayMetrics(0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    var countedCash by remember { mutableStateOf("") }

    fun switchCashier() {
        StaffSessionStore.clear()
        context.startActivity(Intent(context, StaffGateActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openQuickAction(route: String) {
        when (route) {
            "purchases" -> context.startActivity(Intent(context, PurchaseActivity::class.java))
            else -> onNavigate(route)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RETAILPOS", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Shop dashboard", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { TextButton(onClick = ::switchCashier) { Text("SWITCH CASHIER") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TodayPerformanceCard(metrics) }
            item {
                Button(onClick = onNewBill, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("NEW BILL", fontWeight = FontWeight.Bold)
                }
            }
            item {
                Button(onClick = { context.startActivity(Intent(context, PurchaseActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("PURCHASE / RECEIVE STOCK", fontWeight = FontWeight.Bold)
                }
            }
            item {
                Button(onClick = { context.startActivity(Intent(context, ReturnActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("RETURNS / REFUNDS", fontWeight = FontWeight.Bold)
                }
            }
            item {
                Button(onClick = { context.startActivity(Intent(context, ExpenseActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("EXPENSES", fontWeight = FontWeight.Bold)
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DAY-END CASH RECONCILIATION", fontWeight = FontWeight.Bold)
                        Text("Expected cash: ₹${money(metrics.cash)}")
                        OutlinedTextField(value = countedCash, onValueChange = { countedCash = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cash counted") })
                        val counted = countedCash.replace(',', '.').toDoubleOrNull()
                        if (counted != null && counted >= 0.0) {
                            val difference = DayEndReconciliationRules.cashDifference(metrics.cash, counted)
                            Text("Difference: ₹${money(difference)}", fontWeight = FontWeight.Bold, color = if (kotlin.math.abs(difference) < 0.005) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item { Text("Quick access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(quickActions.size) { index ->
                val (route, action) = quickActions[index]
                OutlinedButton(onClick = { openQuickAction(route) }, modifier = Modifier.fillMaxWidth().height(52.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Icon(action.first, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(action.second, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Medium)
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
    val expenses: Double
) {
    val grossProfit: Double get() = totalSales - cogs
    val operatingResult: Double get() = grossProfit - expenses
}

@Composable
private fun TodayPerformanceCard(metrics: TodayMetrics) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("TODAY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("₹${money(metrics.totalSales)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text("${metrics.billCount} bills • ${fmt(metrics.itemsSold)} items sold")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cash ₹${money(metrics.cash)}"); Text("UPI ₹${money(metrics.upi)}") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Card ₹${money(metrics.card)}"); Text("Khata ₹${money(metrics.credit)}") }
            Text("COGS ₹${money(metrics.cogs)} • Gross profit ₹${money(metrics.grossProfit)}", fontWeight = FontWeight.Bold)
            Text("Expenses ₹${money(metrics.expenses)} • Operating result ₹${money(metrics.operatingResult)}", fontWeight = FontWeight.Black)
        }
    }
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
