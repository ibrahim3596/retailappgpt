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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retailpos.app.StaffGateActivity
import com.retailpos.app.core.staff.StaffSessionStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewBill: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val quickActions = listOf(
        "products" to (Icons.Default.Storefront to "Products"),
        "inventory" to (Icons.Default.Inventory2 to "Inventory"),
        "purchases" to (Icons.Default.ShoppingCart to "Purchases"),
        "customers" to (Icons.Default.Person to "Customers"),
        "analytics" to (Icons.Default.Analytics to "Analytics"),
        "settings" to (Icons.Default.Settings to "Settings")
    )

    fun switchCashier() {
        StaffSessionStore.clear()
        context.startActivity(Intent(context, StaffGateActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
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
            item { TodayPerformanceCard() }
            item {
                Button(onClick = onNewBill, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("NEW BILL", fontWeight = FontWeight.Bold)
                }
            }
            item { Text("Quick access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(quickActions.size) { index ->
                val (route, action) = quickActions[index]
                OutlinedButton(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth().height(52.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Icon(action.first, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(action.second, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TodayPerformanceCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text("TODAY'S PERFORMANCE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("₹0.00", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("0 bills", style = MaterialTheme.typography.bodyMedium)
                Text("  •  ", style = MaterialTheme.typography.bodyMedium)
                Text("0 items sold", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
