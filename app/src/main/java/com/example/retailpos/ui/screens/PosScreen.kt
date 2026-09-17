package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.CustomerEntity
import com.example.retailpos.data.local.entity.PaymentMethod
import com.example.retailpos.data.local.entity.ProductEntity
import com.example.retailpos.data.local.entity.TaxType
import com.example.retailpos.data.local.entity.VerificationStatus
import com.example.retailpos.engine.ai.GeminiVisionFallback
import com.example.retailpos.engine.barcode.BarcodeNormalizer
import com.example.retailpos.engine.ocr.PackagingOcrParser
import com.example.retailpos.repository.CartItem
import com.example.retailpos.auth.userRole
import com.example.retailpos.domain.permissions.StaffPermissionRules
import com.example.retailpos.domain.permissions.StaffRole
import com.example.retailpos.domain.permissions.toStaffRole
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.CartItemRow
import com.example.retailpos.ui.components.EmptyCartState
import com.example.retailpos.ui.components.PosSummaryRow
import com.example.retailpos.ui.components.ProductCard
import com.example.ui.theme.*
import com.example.ui.theme.Spacing
import com.example.ui.theme.IconSizes
import com.example.ui.theme.Shapes
import com.example.ui.theme.Elevation
import kotlinx.coroutines.launch
import java.util.UUID

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCameraScanner: () -> Unit = {},
    onNavigateToReceipt: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()
    val sharedCart by viewModel.sharedCartItems.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var isInterstate by remember { mutableStateOf(false) }
    var overallDiscount by remember { mutableStateOf(0.0) }

    var showPaymentModal by remember { mutableStateOf(false) }
    var showHeldCartsDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var discountInputText by remember { mutableStateOf("0") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountReceivedText by remember { mutableStateOf("") }
    var showPhotoScannerModal by remember { mutableStateOf(false) }
    var isProcessingSale by remember { mutableStateOf(false) }
    val heldCarts by viewModel.heldCarts.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val normalized = BarcodeNormalizer.normalize(searchQuery).canonicalGtin
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.barcode == searchQuery ||
                        it.normalizedBarcode == normalized ||
                        it.sku.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var customerQuery by remember { mutableStateOf("") }
    val customerSuggestions = remember(customerQuery, customers) {
        if (customerQuery.isBlank()) emptyList()
        else customers.filter {
            it.name.contains(customerQuery, ignoreCase = true) ||
                    it.phone.contains(customerQuery, ignoreCase = true)
        }
    }

    val cartSummary = remember(sharedCart, isInterstate, overallDiscount) {
        var subtotal = 0.0
        var totalGst = 0.0
        for (item in sharedCart) {
            val gst = item.calculateGst(isInterstate)
            subtotal += gst.assessableValue
            totalGst += gst.totalGst
        }
        val grandTotal = (subtotal + totalGst - overallDiscount).coerceAtLeast(0.0)
        Triple(subtotal, totalGst, grandTotal)
    }

    var showStockWarning by remember { mutableStateOf(false) }
    var stockWarningProduct by remember { mutableStateOf<String?>(null) }
    var stockWarningCurrentQty by remember { mutableStateOf(0.0) }
    var stockWarningRequestedQty by remember { mutableStateOf(0.0) }

    fun addToCart(product: ProductEntity) {
        val cartQty = sharedCart.find { it.product.id == product.id }?.quantity ?: 0.0
        if (product.currentStock > 0 && cartQty + 1.0 > product.currentStock) {
            stockWarningProduct = product.name
            stockWarningCurrentQty = product.currentStock
            stockWarningRequestedQty = cartQty + 1.0
            showStockWarning = true
            return
        }
        viewModel.addToCartDirectly(product)
        searchQuery = ""
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BILLING", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("TERMINAL #01 • ONLINE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (heldCarts.isNotEmpty()) {
                        IconButton(onClick = { showHeldCartsDialog = true }) {
                            BadgedBox(badge = { Badge { Text(heldCarts.size.toString()) } }) {
                                Icon(Icons.Default.Bookmarks, contentDescription = "Held Carts")
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.clearCart() }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = Error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val isTablet = maxWidth > 800.dp
            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1.2f).fillMaxHeight().padding(Spacing.screenPadding), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        SearchAndScanSection(searchQuery, { searchQuery = it }, onNavigateToCameraScanner, filteredProducts, { addToCart(it) })
                        CartSection(sharedCart, viewModel, onNavigateToCameraScanner, Modifier.weight(1f))
                    }
                    Surface(modifier = Modifier.weight(0.8f).fillMaxHeight(), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)) {
                        SummaryAndPaymentSection(cartSummary, overallDiscount, { discountInputText = overallDiscount.toString(); showDiscountDialog = true }, {
                            if (sharedCart.isEmpty()) {
                                Toast.makeText(context, "Cart empty", Toast.LENGTH_SHORT).show()
                            } else if (selectedPaymentMethod == PaymentMethod.CREDIT && selectedCustomer == null) {
                                Toast.makeText(context, "Select a customer for credit sales", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedPaymentMethod = PaymentMethod.CASH
                                amountReceivedText = String.format("%.2f", cartSummary.third)
                                showPaymentModal = true
                            }
                        }, { scope.launch { viewModel.holdCurrentCart(); Toast.makeText(context, "Held", Toast.LENGTH_SHORT).show() } }, true, selectedCustomer, { showCustomerPicker = !showCustomerPicker })
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(Spacing.screenPadding), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    SearchAndScanSection(searchQuery, { searchQuery = it }, onNavigateToCameraScanner, filteredProducts, { addToCart(it) })
                    CartSection(sharedCart, viewModel, onNavigateToCameraScanner, Modifier.weight(1f))
                    SummaryAndPaymentSection(cartSummary, overallDiscount, { discountInputText = overallDiscount.toString(); showDiscountDialog = true }, {
                        if (sharedCart.isEmpty()) Toast.makeText(context, "Cart empty", Toast.LENGTH_SHORT).show()
                        else {
                            if (selectedPaymentMethod == PaymentMethod.CREDIT && selectedCustomer == null) {
                                Toast.makeText(context, "Select a customer for credit sales", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedPaymentMethod = PaymentMethod.CASH; amountReceivedText = String.format("%.2f", cartSummary.third); showPaymentModal = true
                            }
                        }
                    }, { scope.launch { viewModel.holdCurrentCart(); Toast.makeText(context, "Held", Toast.LENGTH_SHORT).show() } }, false, selectedCustomer, { showCustomerPicker = !showCustomerPicker })
                }
            }
        }
    }

    if (showDiscountDialog) {
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Apply Discount") },
            text = {
                OutlinedTextField(value = discountInputText, onValueChange = { discountInputText = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    val requested = discountInputText.toDoubleOrNull()
                    val preDiscountTotal = cartSummary.first + cartSummary.second
                    val rejection = when {
                        requested == null || requested < 0.0 ->
                            "Enter a valid non-negative discount amount."
                        requested > preDiscountTotal + 1e-9 ->
                            "Discount cannot exceed the bill total."
                        else -> StaffPermissionRules.validateBillDiscount(
                            currentUser?.userRole?.toStaffRole() ?: StaffRole.CASHIER,
                            preDiscountTotal,
                            requested
                        )
                    }
                    if (rejection != null) {
                        Toast.makeText(context, rejection, Toast.LENGTH_SHORT).show()
                    } else {
                        overallDiscount = requested!!
                        showDiscountDialog = false
                    }
                }) { Text("APPLY") }
            }
        )
    }

    if (showHeldCartsDialog) {
        AlertDialog(
            onDismissRequest = { showHeldCartsDialog = false },
            title = { Text("Held Carts") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(heldCarts) { held ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.restoreHeldCart(held.id); showHeldCartsDialog = false }.padding(8.dp)) {
                            Text("${held.note} - ₹${held.items.sumOf { it.effectivePrice * it.quantity }}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHeldCartsDialog = false }) { Text("CLOSE") } }
        )
    }

    if (showPaymentModal) {
        val grandTotal = cartSummary.third
        val amountRec = amountReceivedText.toDoubleOrNull() ?: grandTotal
        val changeDue = (amountRec - grandTotal).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showPaymentModal = false },
            title = { Text("Checkout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TOTAL: ₹${String.format("%.2f", grandTotal)}", fontWeight = FontWeight.Black)
                    Row {
                        PaymentMethod.values().forEach { method ->
                            FilterChip(selected = selectedPaymentMethod == method, onClick = { selectedPaymentMethod = method }, label = { Text(method.name) })
                        }
                    }
                    if (selectedPaymentMethod == PaymentMethod.CASH) {
                        OutlinedTextField(value = amountReceivedText, onValueChange = { amountReceivedText = it }, label = { Text("Received") }, modifier = Modifier.fillMaxWidth())
                        Text("Change: ₹${String.format("%.2f", changeDue)}", color = Success)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isProcessingSale) return@Button
                        // Payment must settle the bill: only credit sales may be
                        // received short. Anything else is a till error.
                        if (selectedPaymentMethod != PaymentMethod.CREDIT && amountRec + 1e-9 < grandTotal) {
                            Toast.makeText(context, "Amount received is less than the bill total", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val discountRejection = StaffPermissionRules.validateBillDiscount(
                            currentUser?.userRole?.toStaffRole() ?: StaffRole.CASHIER,
                            cartSummary.first + cartSummary.second,
                            overallDiscount
                        )
                        if (discountRejection != null) {
                            Toast.makeText(context, discountRejection, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isProcessingSale = true
                        scope.launch {
                            try {
                                val invoice = viewModel.posRepo.createInvoice(store?.id ?: "STORE-001", selectedCustomer, sharedCart, selectedPaymentMethod, amountRec, overallDiscount, isInterstate)
                                viewModel.clearCart(); showPaymentModal = false; onNavigateToReceipt(invoice.id)
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            } finally {
                                isProcessingSale = false
                            }
                        }
                    },
                    enabled = !isProcessingSale,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isProcessingSale) "PROCESSING…" else "CONFIRM") }
            }
        )
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            query = customerQuery,
            onQueryChange = { customerQuery = it },
            selectedCustomer = selectedCustomer,
            onConfirm = { selectedCustomer = it; showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false }
        )
    }

    if (showStockWarning && stockWarningProduct != null) {
        StockWarningDialog(
            productName = stockWarningProduct!!,
            currentStock = stockWarningCurrentQty,
            requestedQty = stockWarningRequestedQty,
            onDismiss = { showStockWarning = false }
        )
    }
}

@Composable
fun SearchAndScanSection(searchQuery: String, onSearchQueryChange: (String) -> Unit, onScanClick: () -> Unit, filteredProducts: List<ProductEntity>, onProductSelect: (ProductEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search...") },
                shape = Shapes.medium,
                singleLine = true
            )
            Button(
                onClick = onScanClick,
                modifier = Modifier.height(56.dp),
                shape = Shapes.pill
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("SCAN", fontWeight = FontWeight.Bold)
            }
        }
        if (filteredProducts.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                shadowElevation = Elevation.level3
            ) {
                Column {
                    filteredProducts.take(5).forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelect(product) }
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(product.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("₹${product.sellingPrice}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartSection(sharedCart: List<CartItem>, viewModel: MainViewModel, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    if (sharedCart.isEmpty()) {
        Box(modifier, Alignment.Center) { EmptyCartState(onScanClick) }
    } else {
        LazyColumn(modifier) {
            items(sharedCart, key = { it.product.id }) { item ->
                CartItemRow(
                    name = item.product.name,
                    brand = item.product.brand,
                    packSize = item.product.variant,
                    price = item.product.sellingPrice,
                    quantity = item.quantity,
                    onIncrease = { viewModel.updateCartQuantity(item.product.id, 1.0) },
                    onDecrease = { viewModel.updateCartQuantity(item.product.id, -1.0) }
                )
            }
        }
    }
}

@Composable
fun SummaryAndPaymentSection(cartSummary: Triple<Double, Double, Double>, overallDiscount: Double, onDiscountClick: () -> Unit, onPaymentClick: () -> Unit, onHoldClick: () -> Unit, isTablet: Boolean, selectedCustomer: CustomerEntity?, onSelectCustomer: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(if (isTablet) Spacing.lg else 0.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        PosSummaryRow("Subtotal", "₹${String.format("%.2f", cartSummary.first)}")
        PosSummaryRow("GST", "₹${String.format("%.2f", cartSummary.second)}")
        if (overallDiscount > 0) PosSummaryRow("Discount", "-₹${String.format("%.2f", overallDiscount)}")
        Divider()
        PosSummaryRow("TOTAL", "₹${String.format("%.2f", cartSummary.third)}", true)
        // Customer selection row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Customer:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextSecondary)
            if (selectedCustomer != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedCustomer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    TextButton(onClick = onSelectCustomer) { Text("Change", fontSize = 12.sp) }
                }
            } else {
                TextButton(onClick = onSelectCustomer) { Text("+ Add Customer", fontSize = 12.sp, color = Primary) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(onClick = onHoldClick, modifier = Modifier.weight(1f)) { Text("HOLD") }
            OutlinedButton(onClick = onDiscountClick, modifier = Modifier.weight(1f)) { Text("OFFER") }
            Button(onClick = onPaymentClick, modifier = Modifier.weight(2f), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("CHECKOUT") }
        }
    }
}

@Composable
fun ProductPhotoScannerModal(products: List<ProductEntity>, storeId: String, onProductAdded: (ProductEntity) -> Unit, onSaveAndAddProduct: (ProductEntity) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("AI Scanner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }, confirmButton = { Button(onClick = onDismiss) { Text("CLOSE") } })
}

@Composable
fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCustomer: CustomerEntity?,
    onConfirm: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredCustomers = remember(query, customers) {
        if (query.isBlank()) customers
        else customers.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name or phone...") },
                    shape = Shapes.medium
                )
                if (filteredCustomers.isEmpty()) {
                    Text("No customers found", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.heightIn(max = 200.dp)) {
                        items(filteredCustomers) { customer ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConfirm(customer) }
                                    .padding(Spacing.md),
                                color = if (selectedCustomer?.id == customer.id) Primary.copy(alpha = 0.1f) else Surface,
                                shape = Shapes.medium,
                                border = if (selectedCustomer?.id == customer.id) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(customer.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        if (customer.phone.isNotBlank()) Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                    Text("₹${String.format("%.0f", customer.currentBalance)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = if (customer.currentBalance > 0) Error else Success)
                                }
                            }
                        }
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConfirm(null) }
                                    .padding(Spacing.md),
                                color = if (selectedCustomer == null) Primary.copy(alpha = 0.1f) else Surface,
                                shape = Shapes.medium,
                                border = if (selectedCustomer == null) androidx.compose.foundation.BorderStroke(1.dp, Primary) else null
                            ) {
                                Text("Walk-in Customer", fontWeight = FontWeight.Bold, color = if (selectedCustomer == null) Primary else TextPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) } },
        dismissButton = { TextButton(onClick = { onConfirm(selectedCustomer) }) { Text("CONFIRM", fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun StockWarningDialog(productName: String, currentStock: Double, requestedQty: Double, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insufficient Stock", color = Error, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
        text = { Text("The requested quantity exceeds available stock.\n$productName: $currentStock available, $requestedQty requested", color = TextPrimary, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Error)) { Text("OK", color = Color.White) } }
    )
}
