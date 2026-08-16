package com.retailpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.retailpos.app.ui.theme.RetailPosTheme

private object Routes {
    const val HOME = "home"
    const val POS = "pos"
    const val PRODUCTS = "products"
    const val INVENTORY = "inventory"
    const val CUSTOMERS = "customers"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RetailPosTheme { RetailPosApp() } }
    }
}

@Composable
private fun RetailPosApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onNewBill = { navController.navigate(Routes.POS) }, onNavigate = navController::navigate) }
        composable(Routes.POS) { PlaceholderScreen("Billing", "Fast checkout terminal") }
        composable(Routes.PRODUCTS) { PlaceholderScreen("Products", "Catalogue and product management") }
        composable(Routes.INVENTORY) { PlaceholderScreen("Inventory", "Stock, batches and movements") }
        composable(Routes.CUSTOMERS) { PlaceholderScreen("Customers", "Khata and customer accounts") }
        composable(Routes.ANALYTICS) { PlaceholderScreen("Analytics", "Today and business reporting") }
        composable(Routes.SETTINGS) { PlaceholderScreen("Settings", "Store and device configuration") }
    }
}

@Composable
private fun HomeScreen(onNewBill: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RETAILPOS", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("Shop dashboard", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("TODAY'S PERFORMANCE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("₹0.00", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text("0 bills  •  0 items sold", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Button(
                    onClick = onNewBill,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("NEW BILL", fontWeight = FontWeight.Bold)
                }
            }
            item { SectionTitle("Quick access") }
            items(
                listOf(
                    Routes.PRODUCTS to (Icons.Default.Storefront to "Products"),
                    Routes.INVENTORY to (Icons.Default.Inventory2 to "Inventory"),
                    Routes.CUSTOMERS to (Icons.Default.Person to "Customers"),
                    Routes.ANALYTICS to (Icons.Default.Analytics to "Analytics"),
                    Routes.SETTINGS to (Icons.Default.Settings to "Settings")
                )
            ) { (route, info) ->
                OutlinedButton(
                    onClick = { onNavigate(route) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(info.first, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(info.second, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("V2 foundation screen", style = MaterialTheme.typography.labelLarge)
        }
    }
}
