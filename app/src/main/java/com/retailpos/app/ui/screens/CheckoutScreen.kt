package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.CartLine
import java.util.Locale

private val PAYMENT_METHODS = listOf("CASH", "UPI", "CARD")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cart: List<CartLine>,
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    isProcessing: Boolean,
    error: String?
) {
    var paymentMethod by remember { mutableStateOf(PAYMENT_METHODS.first()) }
    val total = cart.sumOf { it.lineTotal }

    Scaffold(
        topBar = { TopAppBar(title = { Text("CHECKOUT", fontWeight = FontWeight.Black) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("BILL SUMMARY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            items(cart, key = { it.productId }) { line ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(line.name, fontWeight = FontWeight.Bold)
                            Text("${line.quantity.clean()} ${line.unit} × ${money(line.unitPrice)}")
                        }
                        Text(money(line.lineTotal), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("PAYMENT METHOD", fontWeight = FontWeight.Bold)
                        PAYMENT_METHODS.forEach { method ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RadioButton(selected = paymentMethod == method, onClick = { paymentMethod = method })
                                Text(method, modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                }
            }
            item {
                Text("TOTAL  ${money(total)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            if (error != null) {
                item { Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, enabled = !isProcessing, modifier = Modifier.weight(1f).height(56.dp)) { Text("BACK") }
                    Button(
                        onClick = { onComplete(paymentMethod) },
                        enabled = cart.isNotEmpty() && !isProcessing,
                        modifier = Modifier.weight(1.4f).height(56.dp)
                    ) { Text(if (isProcessing) "PROCESSING…" else "COMPLETE SALE", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
