package com.retailpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.retailpos.app.ui.screens.BarcodeScannerScreen
import com.retailpos.app.ui.screens.HomeScreen
import com.retailpos.app.ui.screens.PosScreen
import com.retailpos.app.ui.screens.ProductListScreen
import com.retailpos.app.ui.screens.ProductReviewScreen
import com.retailpos.app.ui.theme.RetailPosTheme

private object Routes {
    const val HOME = "home"
    const val POS = "pos"
    const val PRODUCTS = "products"
    const val ADD_PRODUCT = "products/add"
    const val EDIT_PRODUCT = "products/edit/{productId}"
    const val BILLING_SCANNER = "scanner/billing"
    const val INVENTORY = "inventory"
    const val CUSTOMERS = "customers"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
}

private const val LOCAL_STORE_ID = "local-store"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RetailPosTheme { RetailPosApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetailPosApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewBill = { navController.navigate(Routes.POS) },
                onNavigate = navController::navigate
            )
        }
        composable(Routes.POS) {
            PosScreen(
                onBack = { navController.popBackStack() },
                onOpenScanner = { navController.navigate(Routes.BILLING_SCANNER) }
            )
        }
        composable(Routes.BILLING_SCANNER) {
            BarcodeScannerScreen(
                title = "BILLING SCANNER",
                onBack = { navController.popBackStack() },
                onBarcodeDetected = { _, _ ->
                    // Cart insertion is intentionally deferred until the V2 cart domain is implemented.
                }
            )
        }
        composable(Routes.PRODUCTS) {
            ProductListScreen(
                storeId = LOCAL_STORE_ID,
                onBack = { navController.popBackStack() },
                onAddProduct = { navController.navigate(Routes.ADD_PRODUCT) },
                onEditProduct = { productId -> navController.navigate("products/edit/$productId") }
            )
        }
        composable(Routes.ADD_PRODUCT) {
            ProductReviewScreen(
                storeId = LOCAL_STORE_ID,
                productId = null,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT_PRODUCT,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            ProductReviewScreen(
                storeId = LOCAL_STORE_ID,
                productId = backStackEntry.arguments?.getString("productId"),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.INVENTORY) {
            FoundationPlaceholder("Inventory", "Stock, batches and movements")
        }
        composable(Routes.CUSTOMERS) {
            FoundationPlaceholder("Customers", "Khata and customer accounts")
        }
        composable(Routes.ANALYTICS) {
            FoundationPlaceholder("Analytics", "Today and business reporting")
        }
        composable(Routes.SETTINGS) {
            FoundationPlaceholder("Settings", "Store and device configuration")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoundationPlaceholder(title: String, subtitle: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.padding(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(4.dp))
            Text("V2 foundation screen", style = MaterialTheme.typography.labelLarge)
        }
    }
}
