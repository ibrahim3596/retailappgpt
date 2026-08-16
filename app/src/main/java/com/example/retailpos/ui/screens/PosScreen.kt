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
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.CartItemRow
import com.example.retailpos.ui.components.PosSummaryRow
import com.example.retailpos.ui.components.ProductCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

import com.example.retailpos.ui.components.EmptyCartState

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
    var isInterstate by remember { mutableStateOf(false) }
    var overallDiscount by remember { mutableStateOf(0.0) }

    var showPaymentModal by remember { mutableStateOf(false) }
    var showHeldCartsDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var discountInputText by remember { mutableStateOf("0") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var amountReceivedText by remember { mutableStateOf("") }
    var showPhotoScannerModal by remember { mutableStateOf(false) }
    val heldCarts by viewModel.heldCarts.collectAsStateWithLifecycle()

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

    fun addToCart(product: ProductEntity) {
        viewModel.addToCartDirectly(product)
        searchQuery = ""
    }

    Scaffold(
        containerColor = RetailBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BILLING", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("TERMINAL #01 • ONLINE", style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary)
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
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = RetailError)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailBackground)
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val isTablet = maxWidth > 800.dp
            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1.2f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SearchAndScanSection(searchQuery, { searchQuery = it }, onNavigateToCameraScanner, filteredProducts, { addToCart(it) })
                        CartSection(sharedCart, viewModel, onNavigateToCameraScanner, Modifier.weight(1f))
                    }
                    Surface(modifier = Modifier.weight(0.8f).fillMaxHeight(), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorderSubtle)) {
                        SummaryAndPaymentSection(cartSummary, overallDiscount, { discountInputText = overallDiscount.toString(); showDiscountDialog = true }, {
                            if (sharedCart.isEmpty()) Toast.makeText(context, "Cart empty", Toast.LENGTH_SHORT).show()
                            else { selectedPaymentMethod = PaymentMethod.CASH; amountReceivedText = String.format("%.2f", cartSummary.third); showPaymentModal = true }
                        }, { scope.launch { viewModel.holdCurrentCart(); Toast.makeText(context, "Held", Toast.LENGTH_SHORT).show() } }, true)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SearchAndScanSection(searchQuery, { searchQuery = it }, onNavigateToCameraScanner, filteredProducts, { addToCart(it) })
                    CartSection(sharedCart, viewModel, onNavigateToCameraScanner, Modifier.weight(1f))
                    SummaryAndPaymentSection(cartSummary, overallDiscount, { discountInputText = overallDiscount.toString(); showDiscountDialog = true }, {
                        if (sharedCart.isEmpty()) Toast.makeText(context, "Cart empty", Toast.LENGTH_SHORT).show()
                        else { selectedPaymentMethod = PaymentMethod.CASH; amountReceivedText = String.format("%.2f", cartSummary.third); showPaymentModal = true }
                    }, { scope.launch { viewModel.holdCurrentCart(); Toast.makeText(context, "Held", Toast.LENGTH_SHORT).show() } }, false)
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
                Button(onClick = { overallDiscount = discountInputText.toDoubleOrNull() ?: 0.0; showDiscountDialog = false }) { Text("APPLY") }
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
                        Text("Change: ₹${String.format("%.2f", changeDue)}", color = RetailSuccess)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            val invoice = viewModel.posRepo.createInvoice(store?.id ?: "STORE-001", selectedCustomer, sharedCart, selectedPaymentMethod, amountRec, overallDiscount, isInterstate)
                            viewModel.clearCart(); showPaymentModal = false; onNavigateToReceipt(invoice.id)
                        } catch (e: Exception) { Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("CONFIRM") }
            }
        )
    }
}

@Composable
fun SearchAndScanSection(searchQuery: String, onSearchQueryChange: (String) -> Unit, onScanClick: () -> Unit, filteredProducts: List<ProductEntity>, onProductSelect: (ProductEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.weight(1f), placeholder = { Text("Search...") }, shape = RoundedCornerShape(12.dp))
            Button(onClick = onScanClick, modifier = Modifier.height(56.dp)) { Icon(Icons.Default.QrCodeScanner, null); Text("SCAN") }
        }
        if (filteredProducts.isNotEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp, shape = RoundedCornerShape(12.dp)) {
                Column {
                    filteredProducts.take(5).forEach { product ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onProductSelect(product) }.padding(12.dp)) {
                            Text(product.name, modifier = Modifier.weight(1f)); Text("₹${product.sellingPrice}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartSection(sharedCart: List<CartItem>, viewModel: MainViewModel, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    if (sharedCart.isEmpty()) Box(modifier, Alignment.Center) { EmptyCartState(onScanClick) }
    else LazyColumn(modifier) {
        items(sharedCart, key = { it.product.id }) { item ->
            CartItemRow(item.product.name, item.product.brand, item.product.variant, item.product.sellingPrice, item.quantity, { viewModel.updateCartQuantity(item.product.id, 1.0) }, { viewModel.updateCartQuantity(item.product.id, -1.0) })
        }
    }
}

@Composable
fun SummaryAndPaymentSection(cartSummary: Triple<Double, Double, Double>, overallDiscount: Double, onDiscountClick: () -> Unit, onPaymentClick: () -> Unit, onHoldClick: () -> Unit, isTablet: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(if (isTablet) 16.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PosSummaryRow("Subtotal", "₹${String.format("%.2f", cartSummary.first)}")
        PosSummaryRow("GST", "₹${String.format("%.2f", cartSummary.second)}")
        if (overallDiscount > 0) PosSummaryRow("Discount", "-₹$overallDiscount")
        Divider()
        PosSummaryRow("TOTAL", "₹${String.format("%.2f", cartSummary.third)}", true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onHoldClick, Modifier.weight(1f)) { Text("HOLD") }
            OutlinedButton(onDiscountClick, Modifier.weight(1f)) { Text("OFFER") }
            Button(onPaymentClick, Modifier.weight(2f), colors = ButtonDefaults.buttonColors(RetailPrimary)) { Text("CHECKOUT") }
        }
    }
}

@Composable
fun ProductPhotoScannerModal(products: List<ProductEntity>, storeId: String, onProductAdded: (ProductEntity) -> Unit, onSaveAndAddProduct: (ProductEntity) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("AI Scanner") }, confirmButton = { Button(onClick = onDismiss) { Text("CLOSE") } })
}
