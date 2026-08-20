package com.retailpos.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.retailpos.app.core.products.VoiceOrderParser
import com.retailpos.app.core.products.VoiceSaleCommandParser
import com.retailpos.app.core.staff.StaffSession
import com.retailpos.app.data.AddToCartResult
import com.retailpos.app.data.CartLine
import com.retailpos.app.data.CartManager
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.HeldBillRepository
import com.retailpos.app.data.HeldBillSnapshot
import com.retailpos.app.data.InventoryMovementReason
import com.retailpos.app.data.ProductEntity
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
import com.retailpos.app.ui.screens.ProductMetadataScreen
import com.retailpos.app.ui.screens.ProductReviewScreen
import com.retailpos.app.ui.screens.ReceiptScreen
import com.retailpos.app.ui.screens.SettingsScreen
import com.retailpos.app.ui.theme.RetailPosTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

private object Routes {
    const val HOME = "home"
    const val POS = "pos"
    const val CHECKOUT = "checkout"
    const val RECEIPT = "receipt"
    const val PRODUCTS = "products"
    const val ADD_PRODUCT = "products/add?barcode={barcode}&identify={identify}&returnToBilling={returnToBilling}"
    const val EDIT_PRODUCT = "products/edit/{productId}"
    const val PRODUCT_DETAILS = "products/details/{productId}"
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RetailPosTheme { StaffGatedRetailPosApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailPosApp(staffSession: StaffSession) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { RetailDatabase.get(context) }
    val repository = remember(database) { ProductRepository(database.productDao(), database.productBarcodeDao()) }
    val heldBillRepository = remember(database) { HeldBillRepository(database) }
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
    var showHeldBills by remember { mutableStateOf(false) }
    var heldBills by remember { mutableStateOf<List<HeldBillSnapshot>>(emptyList()) }
    val searchResults by repository.searchProducts(LOCAL_STORE_ID, posQuery).collectAsState(initial = emptyList())
    val customers by database.customerDao().observeAll(LOCAL_STORE_ID).collectAsState(initial = emptyList())

    fun refreshHeldBills() {
        scope.launch { heldBills = heldBillRepository.list(LOCAL_STORE_ID) }
    }

    fun replaceCart(lines: List<CartLine>) {
        cartManager.replace(lines)
        cart = cartManager.lines
    }

    fun addProductToCart(product: ProductEntity, quantity: Double = 1.0) {
        when (cartManager.addQuantity(product, quantity)) {
            AddToCartResult.Added -> { cart = cartManager.lines; posQuery = ""; cartError = null }
            AddToCartResult.OutOfStock -> cartError = "${product.name} is out of stock."
            AddToCartResult.InsufficientStock -> cartError = "Only ${product.stock} ${product.unit} of ${product.name} is available."
            AddToCartResult.InvalidQuantity -> cartError = "The requested quantity is not valid."
        }
    }

    fun setCartQuantity(line: CartLine, quantity: Double) {
        scope.launch {
            val availableStock = database.productDao().getById(line.productId, LOCAL_STORE_ID)?.stock ?: 0.0
            when (cartManager.setQuantity(line.productId, quantity, availableStock)) {
                AddToCartResult.Added -> { cart = cartManager.lines; cartError = null }
                AddToCartResult.OutOfStock -> cartError = "${line.name} is out of stock."
                AddToCartResult.InsufficientStock -> cartError = "Only ${availableStock.clean()} ${line.unit} of ${line.name} is available."
                AddToCartResult.InvalidQuantity -> cartError = "Enter a quantity greater than zero."
            }
        }
    }

    fun handleVoiceInput(spoken: String) {
        scope.launch {
            val commands = VoiceOrderParser.parse(spoken)
            if (commands == null) { cartError = "I heard ‘$spoken’, but could not parse the order. Try ‘aadha kilo shakkar’ or ‘aadha kilo shakkar aur 1 litre tel’."; return@launch }
            val resolved = mutableListOf<Pair<ProductEntity, Double>>()
            for (command in commands) {
                val matches = repository.searchProducts(LOCAL_STORE_ID, command.productQuery).first()
                if (matches.isEmpty()) { posQuery = command.productQuery; cartError = "No product matched ‘${command.productQuery}’. Add the product to the catalog first. Nothing was added to the cart."; return@launch }
                if (matches.size > 1) { posQuery = command.productQuery; cartError = "Multiple products matched ‘${command.productQuery}’. Select the exact product before using this multi-item voice order. Nothing was added to the cart."; return@launch }
                val product = matches.first()
                val normalizedQuantity = VoiceSaleCommandParser.toBaseQuantity(command.quantity, command.unit, product.unit)
                if (normalizedQuantity == null) { cartError = "‘${command.unit}’ does not match ${product.name}'s selling unit ‘${product.unit}’. Nothing was added to the cart."; return@launch }
                if (normalizedQuantity <= 0.0) { cartError = "The requested quantity for ${product.name} is invalid. Nothing was added to the cart."; return@launch }
                resolved += product to normalizedQuantity
            }
            val requestedByProduct = resolved.groupBy { it.first.id }.mapValues { (_, entries) -> entries.sumOf { it.second } }
            for ((productId, requestedQuantity) in requestedByProduct) {
                val product = resolved.first { it.first.id == productId }.first
                if (requestedQuantity > product.stock) { cartError = "Requested ${requestedQuantity.clean()} ${product.unit} of ${product.name}, but only ${product.stock.clean()} ${product.unit} is available. Nothing was added to the cart."; return@launch }
            }
            resolved.forEach { (product, quantity) -> addProductToCart(product, quantity) }
        }
    }

    fun holdCurrentBill() {
        if (cart.isEmpty()) return
        scope.launch {
            try {
                heldBillRepository.hold(LOCAL_STORE_ID, cart)
                cartManager.clear(); cart = emptyList(); posQuery = ""; cartError = "Bill held successfully."; refreshHeldBills()
            } catch (error: Exception) { cartError = error.message ?: "Bill could not be held." }
        }
    }

    fun restoreHeldBill(snapshot: HeldBillSnapshot) {
        if (cart.isNotEmpty()) { cartError = "Clear or hold the current bill before resuming another bill."; return }
        scope.launch {
            runCatching {
                val restored = snapshot.lines.map { line ->
                    val current = database.productDao().getById(line.productId, LOCAL_STORE_ID) ?: error("${line.name} no longer exists")
                    if (line.quantity > current.stock + 1e-9) error("${line.name} has only ${current.stock.clean()} ${current.unit} available")
                    line.copy(unitPrice = current.sellingPrice)
                }
                replaceCart(restored)
                heldBillRepository.take(LOCAL_STORE_ID, snapshot.id) ?: error("Held bill is no longer available")
                refreshHeldBills(); showHeldBills = false; cartError = "Held bill resumed."
            }.onFailure { cartError = it.message ?: "Held bill could not be resumed." }
        }
    }

    fun openReceipt(saleId: String) {
        scope.launch {
            val sale = database.saleDao().getSale(LOCAL_STORE_ID, saleId)
            if (sale == null) { checkoutError = "Receipt could not be loaded."; return@launch }
            receiptSale = sale
            receiptLines = database.saleDao().getSaleLines(sale.id)
            navController.navigate(Routes.RECEIPT)
        }
    }

    fun completeSale(paymentMethod: String, customerId: String?, billDiscountAmount: Double) {
        if (checkoutProcessing || cart.isEmpty()) return
        checkoutProcessing = true; checkoutError = null
        val cartSnapshot = cart.toList()
        val idempotencyKey = checkoutIdempotencyKey ?: UUID.randomUUID().toString().also { checkoutIdempotencyKey = it }
        scope.launch {
            try {
                val result = database.saleDao().checkout(LOCAL_STORE_ID, cartSnapshot, paymentMethod, idempotencyKey, customerId?.takeIf { it.isNotBlank() }, billDiscountAmount = billDiscountAmount, staffRole = staffSession.role)
                cartManager.clear(); cart = emptyList(); posQuery = ""; checkoutIdempotencyKey = null; checkoutProcessing = false
                navController.navigate(Routes.HOME) { popUpTo(Routes.POS) { inclusive = true } }
                completedSale = result.saleId
            } catch (error: Exception) { checkoutProcessing = false; checkoutError = error.message ?: "Sale could not be completed. No stock was deducted." }
        }
    }

    fun shareReceipt(receipt: String) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, receipt) }, "Share receipt")) }

    fun adjustInventory(productId: String, quantityDelta: Double, reason: String) {
        scope.launch { try { val movementReason = InventoryMovementReason.entries.firstOrNull { it.name == reason } ?: InventoryMovementReason.ADJUSTMENT; database.inventoryDao().adjustStock(LOCAL_STORE_ID, productId, quantityDelta, movementReason); inventoryAdjustmentError = null; navController.popBackStack() } catch (error: Exception) { inventoryAdjustmentError = error.message ?: "Stock adjustment failed." } }
    }

    fun receiveInventory(productId: String, quantity: Double, batchNumber: String?, expiryDate: Long?, purchasePrice: Double) {
        scope.launch { try { database.inventoryDao().receiveStock(LOCAL_STORE_ID, productId, quantity, batchNumber, expiryDate, purchasePrice); inventoryReceiveError = null; navController.popBackStack() } catch (error: Exception) { inventoryReceiveError = error.message ?: "Stock receiving failed." } }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onNewBill = { navController.navigate(Routes.POS) }, onNavigate = navController::navigate) }
        composable(Routes.POS) { PosScreen(cart = cart, searchResults = searchResults, onSearchQueryChanged = { posQuery = it }, onAddProduct = { addProductToCart(it) }, onVoiceInput = ::handleVoiceInput, onVoiceError = { cartError = it }, onSetCartQuantity = ::setCartQuantity, onRemoveFromCart = { productId -> cartManager.remove(productId).also { cart = cartManager.lines } }, onBack = { navController.popBackStack() }, onOpenScanner = { navController.navigate(Routes.BILLING_SCANNER) }, onCheckout = { if (cart.isNotEmpty()) { checkoutIdempotencyKey = checkoutIdempotencyKey ?: UUID.randomUUID().toString(); navController.navigate(Routes.CHECKOUT) } }, onHoldBill = ::holdCurrentBill, onOpenHeldBills = { refreshHeldBills(); showHeldBills = true }, onClearBill = { cartManager.clear(); cart = emptyList(); posQuery = ""; cartError = "Bill cleared." }) }
        composable(Routes.CHECKOUT) { CheckoutScreen(cart = cart, customers = customers, onBack = { navController.popBackStack() }, onComplete = ::completeSale, isProcessing = checkoutProcessing, error = checkoutError) }
        composable(Routes.RECEIPT) { receiptSale?.let { sale -> ReceiptScreen(sale = sale, lines = receiptLines, onBack = { receiptSale = null; receiptLines = emptyList(); navController.navigate(Routes.HOME) { popUpTo(Routes.RECEIPT) { inclusive = true } } }, onShare = ::shareReceipt) } }
        composable(Routes.BILLING_SCANNER) { BarcodeScannerScreen(title = "BILLING SCANNER", onBack = { navController.popBackStack() }) { raw, _ -> scope.launch { val barcode = repository.getByBarcode(LOCAL_STORE_ID, raw); val product = barcode?.let { repository.getById(it.productId, LOCAL_STORE_ID) }; if (product == null) unknownBarcode = raw else { addProductToCart(product); if (cartError == null) navController.popBackStack() } } } }
        composable(Routes.PRODUCTS) { ProductListScreen(storeId = LOCAL_STORE_ID, onBack = { navController.popBackStack() }, onAddProduct = { navController.navigate("products/add?barcode=&identify=false&returnToBilling=false") }, onIntelligentCapture = { navController.navigate("products/add?barcode=&identify=true&returnToBilling=false") }, onEditProduct = { navController.navigate("products/edit/$it") }, onEditDetails = { navController.navigate("products/details/$it") }) }
        composable(Routes.ADD_PRODUCT, arguments = listOf(navArgument("barcode") { type = NavType.StringType; defaultValue = "" }, navArgument("identify") { type = NavType.BoolType; defaultValue = false }, navArgument("returnToBilling") { type = NavType.BoolType; defaultValue = false })) { entry -> val initialBarcode = entry.arguments?.getString("barcode").orEmpty(); val returnToBilling = entry.arguments?.getBoolean("returnToBilling") ?: false; ProductReviewScreen(storeId = LOCAL_STORE_ID, productId = null, initialBarcode = initialBarcode, autoIdentify = entry.arguments?.getBoolean("identify") ?: false, onBack = { navController.popBackStack() }, onSaved = if (returnToBilling) ({ savedProductId -> scope.launch { val product = database.productDao().getById(savedProductId, LOCAL_STORE_ID); if (product != null) addProductToCart(product) else cartError = "The new product was saved, but it could not be loaded back into the bill."; navController.popBackStack(Routes.POS, inclusive = false) } }) else null, onExistingProductSelected = { existingProductId -> scope.launch { val product = database.productDao().getById(existingProductId, LOCAL_STORE_ID); if (product != null && returnToBilling) { addProductToCart(product); navController.popBackStack(Routes.POS, inclusive = false) } else if (product != null) navController.navigate("products/edit/$existingProductId") } }) }
        composable(Routes.EDIT_PRODUCT, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> ProductReviewScreen(storeId = LOCAL_STORE_ID, productId = entry.arguments?.getString("productId"), onBack = { navController.popBackStack() }) }
        composable(Routes.PRODUCT_DETAILS, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> ProductMetadataScreen(storeId = LOCAL_STORE_ID, productId = entry.arguments?.getString("productId").orEmpty(), onBack = { navController.popBackStack() }) }
        composable(Routes.INVENTORY) { InventoryScreen(storeId = LOCAL_STORE_ID, repository = repository, inventoryMovements = { database.inventoryDao().getMovements(LOCAL_STORE_ID) }, onBack = { navController.popBackStack() }, onOpenProduct = { navController.navigate("inventory/detail/$it") }, onAdjustProduct = { navController.navigate("inventory/adjust/$it") }, onReceiveProduct = { navController.navigate("inventory/receive/$it") }) }
        composable(Routes.INVENTORY_DETAIL, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> val id = entry.arguments?.getString("productId"); val product by if (id == null) remember { mutableStateOf(null) } else androidx.compose.runtime.produceState<ProductEntity?>(initialValue = null, id) { value = database.productDao().getById(id, LOCAL_STORE_ID) }; val p = product; if (p != null) InventoryDetailScreen(product = p, movements = database.inventoryDao().observeMovements(LOCAL_STORE_ID, p.id).collectAsState(initial = emptyList()).value, onBack = { navController.popBackStack() }, onAdjust = { navController.navigate("inventory/adjust/${p.id}") }, onReceive = { navController.navigate("inventory/receive/${p.id}") }) }
        composable(Routes.INVENTORY_ADJUST, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> InventoryAdjustmentScreen(productId = entry.arguments?.getString("productId").orEmpty(), onBack = { navController.popBackStack() }, error = inventoryAdjustmentError, onSubmit = { productId, delta, reason -> adjustInventory(productId, delta, reason) }) }
        composable(Routes.INVENTORY_RECEIVE, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry -> InventoryReceiveScreen(productId = entry.arguments?.getString("productId").orEmpty(), onBack = { navController.popBackStack() }, error = inventoryReceiveError, onSubmit = { productId, quantity, batch, expiry, purchasePrice -> receiveInventory(productId, quantity, batch, expiry, purchasePrice) }) }
        composable(Routes.CUSTOMERS) { CustomersScreen(storeId = LOCAL_STORE_ID, dao = database.customerDao(), khataDao = database.khataDao(), onOpenCustomer = { navController.navigate("customers/khata/${it.id}") }, onBack = { navController.popBackStack() }) }
        composable(Routes.CUSTOMER_KHATA, arguments = listOf(navArgument("customerId") { type = NavType.StringType })) { entry -> val customerId = entry.arguments?.getString("customerId").orEmpty(); val customer by androidx.compose.runtime.produceState<CustomerEntity?>(initialValue = null, customerId) { value = database.customerDao().getById(customerId, LOCAL_STORE_ID) }; customer?.let { CustomerKhataScreen(storeId = LOCAL_STORE_ID, customer = it, dao = database.khataDao(), onBack = { navController.popBackStack() }) } }
        composable(Routes.ANALYTICS) { AnalyticsScreen(storeId = LOCAL_STORE_ID, saleDao = database.saleDao(), inventoryDao = database.inventoryDao(), onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(context, onBack = { navController.popBackStack() }) }
    }

    if (unknownBarcode != null) AlertDialog(onDismissRequest = { unknownBarcode = null }, title = { Text("UNKNOWN PRODUCT") }, text = { Text("Barcode ${unknownBarcode.orEmpty()} was not found. Identify the product or add it manually.") }, confirmButton = { Button(onClick = { navController.navigate("products/add?barcode=${Uri.encode(unknownBarcode.orEmpty())}&identify=true&returnToBilling=true"); unknownBarcode = null }) { Text("IDENTIFY PRODUCT") } }, dismissButton = { TextButton(onClick = { navController.navigate("products/add?barcode=${Uri.encode(unknownBarcode.orEmpty())}&identify=false&returnToBilling=true"); unknownBarcode = null }) { Text("ADD MANUALLY") } })

    if (showHeldBills) AlertDialog(onDismissRequest = { showHeldBills = false }, title = { Text("HELD BILLS") }, text = { if (heldBills.isEmpty()) Text("No held bills.") else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { heldBills.forEachIndexed { index, bill -> TextButton(onClick = { restoreHeldBill(bill) }) { Text("${index + 1}. ${bill.lines.size} item line(s) • ${money(bill.lines.sumOf { it.lineTotal })}") } } } }, confirmButton = { TextButton(onClick = { showHeldBills = false }) { Text("CLOSE") } })

    if (completedSale != null) AlertDialog(onDismissRequest = { completedSale = null }, title = { Text("SALE COMPLETE") }, text = { Text("Sale ${completedSale.orEmpty()} was recorded successfully.") }, confirmButton = { TextButton(onClick = { val id = completedSale.orEmpty(); completedSale = null; openReceipt(id) }) { Text("VIEW RECEIPT") } }, dismissButton = { TextButton(onClick = { completedSale = null }) { Text("DONE") } })
}

private fun money(value: Double): String = String.format(java.util.Locale.US, "₹%.2f", value)
private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(java.util.Locale.US, "%.2f", this)
