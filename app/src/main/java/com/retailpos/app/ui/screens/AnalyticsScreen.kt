package com.retailpos.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.retailpos.app.core.payment.PaymentSummaryRules
import com.retailpos.app.data.PaymentSummary
import com.retailpos.app.data.ReceiptFormatter
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.SaleDao
import com.retailpos.app.data.SaleEntity
import com.retailpos.app.data.TopProductSales
import com.retailpos.app.ui.components.AiInsight
import com.retailpos.app.ui.components.MetricLine
import com.retailpos.app.ui.components.SectionHeader
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    storeId: String,
    saleDao: SaleDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { RetailDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var salesTotal by remember { mutableStateOf(0.0) }
    var salesCount by remember { mutableStateOf(0) }
    var itemsSold by remember { mutableStateOf(0.0) }
    var cogs by remember { mutableStateOf(0.0) }
    var expenses by remember { mutableStateOf(0.0) }
    var receivables by remember { mutableStateOf(0.0) }
    var payables by remember { mutableStateOf(0.0) }
    var paymentSummary by remember { mutableStateOf<List<PaymentSummary>>(emptyList()) }
    var topProducts by remember { mutableStateOf<List<TopProductSales>>(emptyList()) }
    var recentSales by remember { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var reprintError by remember { mutableStateOf<String?>(null) }

    val currency = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()) }

    LaunchedEffect(storeId) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis

        val grossSales = saleDao.getSalesTotal(storeId, start, end)
        val returnedRevenue = database.returnDao().getRefundTotal(storeId, start, end)
        salesTotal = (grossSales - returnedRevenue).coerceAtLeast(0.0)
        salesCount = saleDao.getSalesCount(storeId, start, end)

        val grossItems = saleDao.getItemsSold(storeId, start, end)
        val returnedItems = database.returnDao().getReturnedItemsTotal(storeId, start, end)
        itemsSold = (grossItems - returnedItems).coerceAtLeast(0.0)

        val grossCogs = saleDao.getCogsTotal(storeId, start, end)
        val restoredCost = database.returnDao().getRestoredCostTotal(storeId, start, end)
        cogs = (grossCogs - restoredCost).coerceAtLeast(0.0)
        expenses = database.expenseDao().totalBetween(storeId, start, end)
        receivables = database.khataDao().totalReceivables(storeId).coerceAtLeast(0.0)
        payables = database.supplierLedgerDao().totalPayables(storeId).coerceAtLeast(0.0)

        val grossPaymentSummary = PaymentSummaryRules.normalize(saleDao.getPaymentSummary(storeId, start, end))
        val refundSummary = database.returnDao().getRefundSummary(storeId, start, end)
        val refundByMethod = refundSummary.associate { it.refundMethod to it.total }
        paymentSummary = grossPaymentSummary.map { summary ->
            summary.copy(total = (summary.total - (refundByMethod[summary.paymentMethod] ?: 0.0)).coerceAtLeast(0.0))
        }.filter { it.total > 0.0 }

        val grossTopProducts = saleDao.getTopProducts(storeId, start, end, 20)
        val returnedProducts = database.returnDao().getReturnedProducts(storeId, start, end).associateBy { it.productId }
        topProducts = grossTopProducts.map { product ->
            val returned = returnedProducts[product.productId]
            product.copy(
                quantity = (product.quantity - (returned?.quantity ?: 0.0)).coerceAtLeast(0.0),
                revenue = (product.revenue - (returned?.revenue ?: 0.0)).coerceAtLeast(0.0)
            )
        }.filter { it.quantity > 0.0 || it.revenue > 0.0 }
            .sortedWith(compareByDescending<TopProductSales> { it.revenue }.thenByDescending { it.quantity })
            .take(5)

        recentSales = saleDao.getRecentSales(storeId, 6)
    }

    val grossProfit = salesTotal - cogs
    val operatingResult = grossProfit - expenses
    val marginPct = if (salesTotal > 0.0) (grossProfit / salesTotal) * 100.0 else 0.0

    fun reprint(sale: SaleEntity) {
        scope.launch {
            runCatching {
                val lines = saleDao.getSaleLines(sale.id)
                require(lines.isNotEmpty()) { "This sale has no line items." }
                ReceiptFormatter.format(sale, lines)
            }.onSuccess { receipt ->
                reprintError = null
                shareReceipt(context, receipt)
            }.onFailure {
                reprintError = it.message ?: "Receipt could not be generated."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Today · Store performance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Net sales", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currency.format(salesTotal), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("$salesCount bills · ${fmt(itemsSold)} items sold", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryTile("Avg bill", if (salesCount == 0) currency.format(0) else currency.format(salesTotal / salesCount), Modifier.weight(1f))
                    SummaryTile("Gross margin", "${String.format(Locale.getDefault(), "%.1f", marginPct)}%", Modifier.weight(1f))
                    SummaryTile("Operating", currency.format(operatingResult), Modifier.weight(1f))
                }
            }
            item {
                SectionHeader("Profitability")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        MetricLine("COGS", currency.format(cogs))
                        MetricLine("Gross profit", currency.format(grossProfit))
                        MetricLine("Expenses", currency.format(expenses))
                        MetricLine("Operating result", currency.format(operatingResult), if (operatingResult < 0.0) "Loss after expenses" else "After expenses")
                    }
                }
            }
            item {
                SectionHeader("Cash & credit")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        MetricLine("Customer receivables", currency.format(receivables), if (receivables > 0.0) "Outstanding Khata" else "No outstanding receivables")
                        MetricLine("Supplier payables", currency.format(payables), if (payables > 0.0) "Outstanding supplier balance" else "No supplier balance")
                    }
                }
            }
            item {
                SectionHeader("RetailGPT")
                AiInsight(
                    when {
                        salesTotal <= 0.0 -> "There are no net sales yet today. Keep this view focused on activity once the first bills are recorded."
                        marginPct < 10.0 -> "Sales are coming in, but gross margin is under 10%. Review high-volume, low-margin products before the next reorder."
                        receivables > 0.0 -> "Customer credit is still outstanding. Prioritize the oldest balances before extending more Khata."
                        else -> "Sales and margin are in a healthy range. Review your top products to decide what to keep readily available."
                    }
                )
            }
            item {
                SectionHeader("Payment mix")
                if (paymentSummary.isEmpty()) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text("No net payments recorded today.", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(paymentSummary, key = { it.paymentMethod }) { summary ->
                Card(Modifier.fillMaxWidth()) {
                    MetricLine(summary.paymentMethod, currency.format(summary.total), "${summary.transactionCount} gross bill${if (summary.transactionCount == 1) "" else "s"}")
                }
            }
            item {
                SectionHeader("Top products", if (topProducts.isNotEmpty()) "Top 5" else null)
                if (topProducts.isEmpty()) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text("No net product sales yet today.", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(topProducts, key = { it.productId }) { top ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(top.name, fontWeight = FontWeight.SemiBold)
                            Text("${fmt(top.quantity)} units", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(currency.format(top.revenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                SectionHeader("Recent sales")
            }
            if (recentSales.isEmpty()) {
                item { Text("No sales yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(recentSales, key = { it.id }) { sale ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(if (sale.paymentMethod.startsWith("SPLIT:")) "Split payment" else sale.paymentMethod, fontWeight = FontWeight.SemiBold)
                                    Text(timeFormatter.format(sale.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(currency.format(sale.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = { reprint(sale) }, modifier = Modifier.fillMaxWidth()) { Text("Share receipt") }
                        }
                    }
                }
            }
            reprintError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) } }
            item {
                val difference = operatingResult
                if (abs(difference) > 0.0 && salesTotal > 0.0) {
                    Text(
                        if (difference >= 0.0) "Today is currently operating above expenses." else "Today is currently below expenses.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private fun shareReceipt(context: Context, receipt: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, receipt)
    }, "Share receipt"))
}

private fun fmt(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.getDefault(), "%.2f", value)
