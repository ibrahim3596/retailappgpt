package com.retailpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.retailpos.app.data.CartLine
import com.retailpos.app.data.ProductRepository
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.ui.screens.BarcodeScannerScreen
import com.retailpos.app.ui.screens.HomeScreen
import com.retailpos.app.ui.screens.PosScreen
import com.retailpos.app.ui.screens.ProductListScreen
import com.retailpos.app.ui.screens.ProductReviewScreen
import com.retailpos.app.ui.theme.RetailPosTheme
import kotlinx.coroutines.launch

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) {
        val db = RetailDatabase.get(context)
        ProductRepository(db.productDao(), db.productBarcodeDao())
    }
    var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
    var unknownBarcode by remember { mutableStateOf<String?>(null) }

    fun addProductToCart(product: com.retailpos.app.data.ProductEntity) {
        val existing = cart.firstOrNull { it.productId == product.id }
        cart = if (existing == null) {
            cart + CartLine(
                productId = product.id,
                name = product.name,
                sku = product.sku,
                unit = product.unit,
                unitPrice = product.sellingPrice
            )
        } else {
            cart.map {
                if (it.productId == product.id) it.copy(quantity = it.quantity + 1.0) else it
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewBill = { navController.navigate(Routes.POS) },
                onNavigate = navController::navigate
            )
        }
        composable(Routes.POS) {
            PosScreen(
                cart = cart,
                onRemoveFromCart = { productId -> cart = cart.filterNot { it.productId == productId } },
                onBack = { navController.popBackStack() },
                onOpenScanner = { navController.navigate(Routes.BILLING_SCANNER) }
            )
        }
        composable(Routes.BILLING_SCANNER) {
            BarcodeScannerScreen(
                title = "BILLING SCANNER",
                onBack = { navController.popBackStack() },
                onBarcodeDetected = { raw, _ ->
                    scope.launch {
                        val barcode = repository.getByBarcode(LOCAL_STORE_ID, raw)
                        val product = barcode?.let { repository.getById(it.productId, LOCAL_STORE_ID) }
                        if (product == null) {
                            unknownBarcode = raw
                        } else if (product.stock > 0.0) {
                            addProductToCart(product)
                            navController.popBackStack()
                        } else {
                            unknownBarcode = raw
                        }
                    }
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
        composable(Routes.INVENTORY) { FoundationPlaceholder("Inventory", "Stock, batches and movements") }
        composable(Routes.CUSTOMERS) { FoundationPlaceholder("Customers", "Khata and customer accounts") }
        composable(Routes.ANALYTICS) { FoundationPlaceholder("Analytics", "Today and business reporting") }
        composable(Routes.SETTINGS) { FoundationPlaceholder("Settings", "Store and device configuration") }
    }

    unknownBarcode?.let { value ->
        AlertDialog(
            onDismissRequest = { unknownBarcode = null },
            title = { Text("Product not found") },
            text = { Text("No product is linked to barcode $value.") },
            confirmButton = {
                Button(onClick = {
                    unknownBarcode = null
                    navController.navigate(Routes.ADD_PRODUCT)
                }) { Text("ADD PRODUCT") }
            },
            dismissButton = {
                TextButton(onClick = { unknownBarcode = null }) { Text("CANCEL") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoundationPlaceholder(title: String, subtitle: String) {
    Scaffold(topBar = { TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
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
