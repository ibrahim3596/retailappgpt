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
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.InventoryMovementReason
import com.retailpos.app.data.ProductRepository
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.SaleEntity
import com.retailpos.app.data.SaleLineEntity
import com.retailpos.app.ui.screens.AnalyticsScreen
import com.retailpos.app.ui.screens.BarcodeScannerScreen
import com.retailpos.app.ui.screens.CheckoutScreen
import com.retailpos.app.ui.screens.CustomerKhataScreen
import com.retailpos.app.ui.screens.CustomersScreen
import com.retailpos.app.ui.screens.HomeScreen
import com.retailpos.app.ui.screens.InventoryAdjustmentScreen
import com.retailpos.app.ui.screens.InventoryDetailScreen
import com.retailpos.app.ui.screens.InventoryReceiveScreen
import com.retailpos.app.ui.screens.InventoryScreen
import com.retailpos.app.ui.screens.PosScreen
import com.retailpos.app.ui.screens.ProductListScreen
import com.retailpos.app.ui.screens.ProductReviewScreen
import com.retailpos.app.ui.screens.ReceiptScreen
import com.retailpos.app.ui.screens.SettingsScreen
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
    const val INVENTORY_DETAIL = "inventory/detail/{productId}"
    const val INVENTORY_ADJUST = "inventory/adjust/{productId}"
    const val INVENTORY_RECEIVE = "inventory/receive/{productId}"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_KHATA = "customers/khata/{customerId}"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
}
private const val LOCAL_STORE_ID = "local-store"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { RetailPosTheme { RetailPosApp() } } }
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
    var inventoryAdjustmentError by remember { mutableStateOf<String?>(null) }
    var inventoryReceiveError by remember { mutableStateOf<String?>(null) }
    val searchResults by repository.searchProducts(LOCAL_STORE_ID, posQuery).collectAsState(initial = emptyList())
    val customers by database.customerDao().observeAll(LOCAL_STORE_ID).collectAsState(initial = emptyList())

    fun addProductToCart(product: com.retailpos.app.data.ProductEntity) {
        when (cartManager.add(product)) {
            AddToCartResult.Added -> { cart = cartManager.lines; posQuery = "" }
            AddToCartResult.OutOfStock -> cartError = "${product.name} is out of stock."
            AddToCartResult.InsufficientStock -> cartError = "Only ${product.stock} ${product.unit} of ${product.name} is available."
        }
    }
    fun openReceipt(saleId: String) { scope.launch { val sale = database.saleDao().getSale(LOCAL_STORE_ID, saleId); if (sale == null) { checkoutError = "Receipt could not be loaded."; return@launch }; receiptSale = sale; receiptLines = database.saleDao().getSaleLines(sale.id); navController.navigate(Routes.RECEIPT) } }
    fun completeSale(paymentMethod: String, customerId: String?) {
        if (checkoutProcessing || cart.isEmpty()) return
        checkoutProcessing = true; checkoutError = null
        val cartSnapshot = cart.toList(); val idempotencyKey = checkoutIdempotencyKey ?: UUID.randomUUID().toString().also { checkoutIdempotencyKey = it }
        scope.launch { try {
            val result = database.saleDao().checkout(LOCAL_STORE_ID, cartSnapshot, paymentMethod, idempotencyKey, customerId?.takeIf { it.isNotBlank() })
            cartManager.clear(); cart = emptyList(); posQuery = ""; checkoutIdempotencyKey = null; checkoutProcessing = false
            navController.navigate(Routes.HOME) { popUpTo(Routes.POS) { inclusive = true } }; completedSale = result.saleId
        } catch (error: Exception) { checkoutProcessing = false; checkoutError = error.message ?: "Sale could not be completed. No stock was deducted." } }
    }
    fun shareReceipt(receipt: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, receipt) }, "Share receipt")) }
    fun adjustInventory(productId: String, quantityDelta: Double, reason: String) { scope.launch { try { val movementReason = InventoryMovementReason.entries.firstOrNull { it.name == reason } ?: InventoryMovementReason.ADJUSTMENT; database.inventoryDao().adjustStock(LOCAL_STORE_ID, productId, quantityDelta, movementReason); inventoryAdjustmentError = null; navController.popBackStack() } catch (error: Exception) { inventoryAdjustmentError = error.message ?: "Stock adjustment failed." } } }
    fun receiveInventory(productId: String, quantity: Double, batchNumber: String?, expiryDate: Long?, purchasePrice: Double) { scope.launch { try { database.inventoryDao().receiveStock(LOCAL_STORE_ID, productId, quantity, batchNumber, expiryDate, purchasePrice); inventoryReceiveError = null; navController.popBackStack() } catch (error: Exception) { inventoryReceiveError = error.message ?: "Stock receiving failed." } } }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onNewBill = { navController.navigate(Routes.POS) }, onNavigate = navController::navigate) }
        composable(Routes.POS) { PosScreen(cart = cart, searchResults = searchResults, onSearchQueryChanged = { posQuery = it }, onAddProduct = ::addProductToCart, onRemoveFromCart = { productId -> cartManager.remove(productId).also { cart = cartManager.lines } }, onBack = { navController.popBackStack() }, onOpenScanner = { navController.navigate(Routes.BILLING_SCANNER) }, onCheckout = { if (cart.isNotEmpty()) { checkoutIdempotencyKey = checkoutIdempotencyKey ?: UUID.randomUUID().toString(); navController.navigate(Routes.CHECKOUT) } }) }
        composable(Routes.CHECKOUT) { CheckoutScreen(cart = cart, customers = customers, onBack = { navController.popBackStack() }, onComplete = ::completeSale, isProcessing = checkoutProcessing, error = checkoutError) }
        composable(Routes.RECEIPT) { receiptSale?.let { sale -> ReceiptScreen(sale = sale, lines = receiptLines, onBack = { receiptSale = null; receiptLines = emptyList(); navController.navigate(Routes.HOME) { popUpTo(Routes.RECEIPT) { inclusive = true } } }, onShare = ::shareReceipt) } }
        composable(Routes.BILLING_SCANNER) { BarcodeScannerScreen(title = "BILLING SCANNER", onBack = { navController.popBackStack() }, onBarcodeDetected = { raw, _ -> scope.launch { val barcode = repository.getByBarcode(LOCAL_STORE_ID, raw); val product = barcode?.let { repository.getById(it.productId, LOCAL_STORE_ID) }; if (product == null) unknownBarcode = raw else { val before = cartManager.lines.size; addProductToCart(product); if ((cartManager.lines.size != before || cartManager.lines.any { it.productId == product.id }) && cartError == null) navController.popBackStack() } } }) }
        composable(Routes.PRODUCTS) { ProductListScreen(storeId = LOCAL_STORE_ID, onBack = { navController.popBackStack() }, onAddProduct = { navController.navigate(Routes.ADD_PRODUCT) }, onEditProduct = { navController.navigate("products/edit/$it") }) }
        composable(Routes.ADD_PRODUCT) { ProductReviewScreen(storeId = LOCAL_STORE_ID, productId = null, onBack = { navController.popBackStack() }) }
        composable(Routes.EDIT_PRODUCT, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> ProductReviewScreen(storeId = LOCAL_STORE_ID, productId = entry.arguments?.getString("productId"), onBack = { navController.popBackStack() }) }
        composable(Routes.INVENTORY) { InventoryScreen(storeId = LOCAL_STORE_ID, repository = repository, inventoryMovements = { database.inventoryDao().getMovements(LOCAL_STORE_ID) }, onBack = { navController.popBackStack() }, onOpenProduct = { navController.navigate("inventory/detail/$it") }, onAdjustProduct = { navController.navigate("inventory/adjust/$it") }, onReceiveProduct = { navController.navigate("inventory/receive/$it") }) }
        composable(Routes.INVENTORY_DETAIL, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> val id = entry.arguments?.getString("productId"); val product by if (id == null) remember { mutableStateOf(null) } else androidx.compose.runtime.produceState<com.retailpos.app.data.ProductEntity?>(initialValue = null, id) { value = database.productDao().getById(id, LOCAL_STORE_ID) }; val p = product; if (p == null) FoundationPlaceholder("Product", "Product could not be loaded") else InventoryDetailScreen(product = p, batchesLoader = { database.inventoryDao().getAvailableBatchesFefo(LOCAL_STORE_ID, p.id) }, movementsLoader = { database.inventoryDao().getProductMovements(LOCAL_STORE_ID, p.id) }, onBack = { navController.popBackStack() }, onAdjust = { navController.navigate("inventory/adjust/${p.id}") }, onReceive = { navController.navigate("inventory/receive/${p.id}") }) }
        composable(Routes.INVENTORY_ADJUST, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> val id = entry.arguments?.getString("productId"); val product by if (id == null) remember { mutableStateOf(null) } else androidx.compose.runtime.produceState<com.retailpos.app.data.ProductEntity?>(initialValue = null, id) { value = database.productDao().getById(id, LOCAL_STORE_ID) }; val p = product; if (p == null) FoundationPlaceholder("Product", "Product could not be loaded") else InventoryAdjustmentScreen(product = p, onBack = { navController.popBackStack() }, onAdjust = { q, r -> adjustInventory(p.id, q, r) }, error = inventoryAdjustmentError) }
        composable(Routes.INVENTORY_RECEIVE, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> val id = entry.arguments?.getString("productId"); val product by if (id == null) remember { mutableStateOf(null) } else androidx.compose.runtime.produceState<com.retailpos.app.data.ProductEntity?>(initialValue = null, id) { value = database.productDao().getById(id, LOCAL_STORE_ID) }; val p = product; if (p == null) FoundationPlaceholder("Product", "Product could not be loaded") else InventoryReceiveScreen(product = p, onBack = { navController.popBackStack() }, onReceive = { q, b, e, pp -> receiveInventory(p.id, q, b, e, pp) }, error = inventoryReceiveError) }
        composable(Routes.CUSTOMERS) { CustomersScreen(storeId = LOCAL_STORE_ID, dao = database.customerDao(), khataDao = database.khataDao(), onOpenCustomer = { navController.navigate("customers/khata/${it.id}") }, onBack = { navController.popBackStack() }) }
        composable(Routes.CUSTOMER_KHATA, arguments = listOf(navArgument("customerId") { type = NavType.StringType })) { entry -> val id = entry.arguments?.getString("customerId"); val customer by if (id == null) remember { mutableStateOf(null) } else androidx.compose.runtime.produceState<CustomerEntity?>(initialValue = null, id) { value = database.customerDao().getById(id, LOCAL_STORE_ID) }; val c = customer; if (c == null) FoundationPlaceholder("Customer", "Customer could not be loaded") else CustomerKhataScreen(storeId = LOCAL_STORE_ID, customer = c, dao = database.khataDao(), onBack = { navController.popBackStack() }) }
        composable(Routes.ANALYTICS) { AnalyticsScreen(storeId = LOCAL_STORE_ID, saleDao = database.saleDao(), onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(context = context, onBack = { navController.popBackStack() }) }
    }
    unknownBarcode?.let { value -> AlertDialog(onDismissRequest = { unknownBarcode = null }, title = { Text("Product not found") }, text = { Text("No product is linked to barcode $value.") }, confirmButton = { Button(onClick = { unknownBarcode = null; navController.navigate(Routes.ADD_PRODUCT) }) { Text("ADD PRODUCT") } }, dismissButton = { TextButton(onClick = { unknownBarcode = null }) { Text("CANCEL") } }) }
    cartError?.let { message -> AlertDialog(onDismissRequest = { cartError = null }, title = { Text("Cannot add product") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { cartError = null }) { Text("OK") } }) }
    completedSale?.let { saleId -> AlertDialog(onDismissRequest = { completedSale = null }, title = { Text("Sale completed") }, text = { Text("Sale saved successfully.\nReceipt ID: $saleId") }, confirmButton = { Button(onClick = { completedSale = null; openReceipt(saleId) }) { Text("VIEW RECEIPT") } }, dismissButton = { TextButton(onClick = { completedSale = null }) { Text("DONE") } }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoundationPlaceholder(title: String, subtitle: String) { Scaffold(topBar = { TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }) }) { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Spacer(Modifier.padding(4.dp)); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.padding(4.dp)); Text("V2 foundation screen", style = MaterialTheme.typography.labelLarge) } } }
