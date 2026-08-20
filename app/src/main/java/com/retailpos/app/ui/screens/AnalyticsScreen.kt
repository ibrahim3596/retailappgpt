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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.PaymentSummary
import com.retailpos.app.data.ReceiptFormatter
import com.retailpos.app.data.SaleDao
import com.retailpos.app.data.SaleEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    storeId: String,
    saleDao: SaleDao,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var salesTotal by remember { mutableStateOf(0.0) }
    var salesCount by remember { mutableStateOf(0) }
    var itemsSold by remember { mutableStateOf(0.0) }
    var paymentSummary by remember { mutableStateOf<List<PaymentSummary>>(emptyList()) }
    var recentSales by remember { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var reprintError by remember { mutableStateOf<String?>(null) }
    val currency = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

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
        salesTotal = saleDao.getSalesTotal(storeId, start, end)
        salesCount = saleDao.getSalesCount(storeId, start, end)
        itemsSold = saleDao.getItemsSold(storeId, start, end)
        paymentSummary = saleDao.getPaymentSummary(storeId, start, end)
        recentSales = saleDao.getRecentSales(storeId, 10)
    }

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

    Scaffold(topBar = { TopAppBar(title = { Text("ANALYTICS", fontWeight = FontWeight.Black) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsStat("SALES", currency.format(salesTotal), Modifier.weight(1f))
                    AnalyticsStat("BILLS", salesCount.toString(), Modifier.weight(1f))
                    AnalyticsStat("ITEMS", String.format(Locale.getDefault(), "%.0f", itemsSold), Modifier.weight(1f))
                }
            }
            item { Text("Payment mix", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
            if (paymentSummary.isEmpty()) {
                item { Text("No sales recorded today.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(paymentSummary, key = { it.paymentMethod }) { summary ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(summary.paymentMethod, fontWeight = FontWeight.Bold)
                                Text("${summary.transactionCount} bill${if (summary.transactionCount == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(currency.format(summary.total), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            item { Text("Recent sales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp)) }
            if (recentSales.isEmpty()) {
                item { Text("No sales yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(recentSales, key = { it.id }) { sale ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(sale.paymentMethod, fontWeight = FontWeight.Bold)
                                    Text(timeFormatter.format(sale.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(currency.format(sale.total), fontWeight = FontWeight.Black)
                            }
                            OutlinedButton(onClick = { reprint(sale) }, modifier = Modifier.fillMaxWidth()) { Text("REPRINT / SHARE RECEIPT") }
                        }
                    }
                }
            }
            reprintError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }
}

private fun shareReceipt(context: Context, receipt: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, receipt)
    }, "Share / Reprint receipt"))
}

@Composable
private fun AnalyticsStat(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}
