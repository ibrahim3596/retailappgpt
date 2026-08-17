package com.retailpos.app

import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.retailpos.app.data.AddToCartResult
import com.retailpos.app.data.CartManager
import com.retailpos.app.data.ProductRepository
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.SaleEntity
import com.retailpos.app.data.SaleLineEntity
import com.retailpos.app.ui.screens.BarcodeScannerScreen
import com.retailpos.app.ui.screens.CheckoutScreen
import com.retailpos.app.ui.screens.HomeScreen
import com.retailpos.app.ui.screens.PosScreen
import com.retailpos.app.ui.screens.ProductListScreen
import com.retailpos.app.ui.screens.ProductReviewScreen
import com.retailpos.app.ui.screens.ReceiptScreen
import com.retailpos.app.ui.theme.RetailPosTheme
import kotlinx.coroutines.launch
import java.util.UUID

private object Routes {
    const val HOME = "home"
    const val POS = "pos"
    const val CHECKOUT = "checkout"
    const val RECEIPT = "receipt"
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { RetailDatabase.get(context) }
    val repository = remember(database) { ProductRepository(database.productDao(), database.productBarcodeDao()) }
    val cartManager = remember { CartManager() }
    var cart by remember { mutableStateOf(cartManager.lines) }
    var posQuery by remember { mutableStateOf("") }
    var unknownBarcode by remember { mutableStateOf<String?>(null) }
    var cartError by remember { mutableStateOf<String?>(null) }
    var checkoutError by remember { mutableStateOf<String?>(null) }
    var checkoutProcessing by remember { mutableStateOf(false) }
    var completedSale by remember { mutableStateOf<String?>(null) }
    var checkoutIdempotencyKey by remember { mutableStateOf<String?>(null) }
    var receiptSale by remember { mutableStateOf<SaleEntity?>(null) }
    var receiptLines by remember { mutableStateOf<List<SaleLineEntity>>(emptyList()) }
    val searchResults by repository.searchProducts(LOCAL_STORE_ID, posQuery).collectAsState(initial = emptyList())

    fun addProductToCart(product: com.retailpos.app.data.ProductEntity) {
        when (cartManager.add(product)) {
            AddToCartResult.Added -> {
                cart = cartManager.lines
                posQuery = ""
            }
            AddToCartResult.OutOfStock -> cartError = "${product.name} is out of stock."
            AddToCartResult.InsufficientStock -> cartError = "Only ${product.stock} ${product.unit} of ${product.name} is available."
        }
    }

    fun openReceipt(saleId: String) {
        scope.launch {
            val sale = database.saleDao().getSale(LOCAL_STORE_ID, saleId)
            if (sale == null) {
                checkoutError = "Receipt could not be loaded."
                return@launch
            }
            receiptSale = sale
            receiptLines = database.saleDao().getSaleLines(sale.id)
            navController.navigate(Routes.RECEIPT)
        }
    }

    fun completeSale(paymentMethod: String) {
        if (checkoutProcessing || cart.isEmpty()) return
        checkoutProcessing = true
        checkoutError = null
        val cartSnapshot = cart.toList()
        val idempotencyKey = checkoutIdempotencyKey ?: UUID.randomUUID().toString().also { checkoutIdempotencyKey = it }
        scope.launch {
            try {
                val result = database.saleDao().checkout(
                    storeId = LOCAL_STORE_ID,
                    cart = cartSnapshot,
                    paymentMethod = paymentMethod,
                    idempotencyKey = idempotencyKey
                )
                cartManager.clear()
                cart = emptyList()
                posQuery = ""
                checkoutIdempotencyKey = null
                checkoutProcessing = false
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.POS) { inclusive = true }
                }
                completedSale = result.saleId
            } catch (error: Exception) {
                checkoutProcessing = false
                checkoutError = error.message ?: "Sale could not be completed. No stock was deducted."
            }
        }
    }

    fun shareReceipt(receipt: String) {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, receipt)
                },
                "Share receipt"
            )
        )
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
                searchResults = searchResults,
                onSearchQueryChanged = { posQuery = it },
                onAddProduct = ::addProductToCart,
                onRemoveFromCart = { productId ->
                    cartManager.remove(productId)
                    cart = cartManager.lines
                },
                onBack = { navController.popBackStack() },
                onOpenScanner = { navController.navigate(Routes.BILLING_SCANNER) },
                onCheckout = {
                    if (cart.isNotEmpty()) {
                        checkoutIdempotencyKey = checkoutIdempotencyKey ?: UUID.randomUUID().toString()
                        navController.navigate(Routes.CHECKOUT)
                    }
                }
            )
        }
        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                cart = cart,
                onBack = { navController.popBackStack() },
                onComplete = ::completeSale,
                isProcessing = checkoutProcessing,
                error = checkoutError
            )
        }
        composable(Routes.RECEIPT) {
            receiptSale?.let { sale ->
                ReceiptScreen(
                    sale = sale,
                    lines = receiptLines,
                    onBack = {
                        receiptSale = null
                        receiptLines = emptyList()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.RECEIPT) { inclusive = true }
                        }
                    },
                    onShare = ::shareReceipt
                )
            }
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
                        } else {
                            val before = cartManager.lines.size
                            addProductToCart(product)
                            if (cartManager.lines.size != before || cartManager.lines.any { it.productId == product.id }) {
                                if (cartError == null) navController.popBackStack()
                            }
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
            ProductReviewScreen(storeId = LOCAL_STORE_ID, productId = null, onBack = { navController.popBackStack() })
        }
        composable(Routes.EDIT_PRODUCT, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
            ProductReviewScreen(
                storeId = LOCAL_STORE_ID,
                productId = entry.arguments?.getString("productId"),
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
            dismissButton = { TextButton(onClick = { unknownBarcode = null }) { Text("CANCEL") } }
        )
    }

    cartError?.let { message ->
        AlertDialog(
            onDismissRequest = { cartError = null },
            title = { Text("Cannot add product") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { cartError = null }) { Text("OK") } }
        )
    }

    completedSale?.let { saleId ->
        AlertDialog(
            onDismissRequest = { completedSale = null },
            title = { Text("Sale completed") },
            text = { Text("Sale saved successfully.\nReceipt ID: $saleId") },
            confirmButton = {
                Button(onClick = {
                    completedSale = null
                    openReceipt(saleId)
                }) { Text("VIEW RECEIPT") }
            },
            dismissButton = { TextButton(onClick = { completedSale = null }) { Text("DONE") } }
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
