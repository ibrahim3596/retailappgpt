package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.retailpos.app.core.products.CheckoutPricingPreview
import com.retailpos.app.core.products.CheckoutPricingPreviewCalculator
import com.retailpos.app.core.products.StoreTaxMode
import com.retailpos.app.data.CartLine
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.RetailDatabase
import java.util.Locale

private val PAYMENT_METHODS = listOf("CASH", "UPI", "CARD", "CREDIT")
private const val LOCAL_STORE_ID = "local-store"

private enum class DiscountMode { AMOUNT, PERCENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cart: List<CartLine>,
    customers: List<CustomerEntity>,
    onBack: () -> Unit,
    onComplete: (String, String?, Double) -> Unit,
    isProcessing: Boolean,
    error: String?
) {
    val context = LocalContext.current
    var paymentMethod by remember { mutableStateOf(PAYMENT_METHODS.first()) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var discountMode by remember { mutableStateOf(DiscountMode.AMOUNT) }
    var discountInput by remember { mutableStateOf("") }
    var pricingPreview by remember { mutableStateOf<CheckoutPricingPreview?>(null) }
    var pricingError by remember { mutableStateOf<String?>(null) }
    val creditWithoutCustomer = paymentMethod == "CREDIT" && selectedCustomer == null

    LaunchedEffect(cart, discountMode, discountInput) {
        if (cart.isEmpty()) {
            pricingPreview = null
            return@LaunchedEffect
        }
        pricingError = null
        pricingPreview = null
        runCatching {
            val database = RetailDatabase.get(context)
            val settings = database.storeSettingsDao().get(LOCAL_STORE_ID)
            val taxMode = StoreTaxMode.fromStorage(settings?.gstMode ?: StoreTaxMode.NO_GST.storageValue)
            val rates = cart.associate { line ->
                line.productId to (database.productMetadataDao().get(line.productId, LOCAL_STORE_ID)?.taxRatePercent ?: 0.0)
            }
            val subtotal = cart.sumOf { it.lineTotal }
            val raw = discountInput.replace(',', '.').toDoubleOrNull() ?: 0.0
            require(raw.isFinite() && raw >= 0.0) { "Discount must be a non-negative number." }
            val discountAmount = when (discountMode) {
                DiscountMode.AMOUNT -> raw
                DiscountMode.PERCENT -> {
                    require(raw <= 100.0) { "Discount percentage cannot exceed 100%." }
                    subtotal * raw / 100.0
                }
            }
            CheckoutPricingPreviewCalculator.calculate(cart, taxMode.toTaxTreatment(), rates, discountAmount)
        }.onSuccess { pricingPreview = it }
            .onFailure { pricingError = it.message ?: "Unable to calculate checkout pricing. The sale cannot be completed until pricing is available." }
    }

    val total = pricingPreview?.total ?: cart.sumOf { it.lineTotal }
    val subtotal = pricingPreview?.subtotal ?: cart.sumOf { it.lineTotal }
    val discount = pricingPreview?.discountAmount ?: 0.0
    val tax = pricingPreview?.taxAmount ?: 0.0

    Scaffold(topBar = { TopAppBar(title = { Text("CHECKOUT", fontWeight = FontWeight.Black) }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("BILL SUMMARY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            items(cart, key = { it.productId }) { line ->
                val previewLine = pricingPreview?.lines?.firstOrNull { it.productId == line.productId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(line.name, fontWeight = FontWeight.Bold)
                                Text("${line.quantity.clean()} ${line.unit} × ${money(line.unitPrice)}")
                            }
                            Text(money(previewLine?.total ?: line.lineTotal), fontWeight = FontWeight.Bold)
                        }
                        if (previewLine != null && previewLine.taxAmount > 0.0) {
                            Text("GST ${cleanRate(previewLine.taxRatePercent)}% • ${money(previewLine.taxAmount)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DISCOUNT", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { discountMode = DiscountMode.AMOUNT }, enabled = !isProcessing) { Text(if (discountMode == DiscountMode.AMOUNT) "✓ ₹ Amount" else "₹ Amount") }
                            OutlinedButton(onClick = { discountMode = DiscountMode.PERCENT }, enabled = !isProcessing) { Text(if (discountMode == DiscountMode.PERCENT) "✓ % Percent" else "% Percent") }
                        }
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { value ->
                                discountInput = value.filter { it.isDigit() || it == '.' || it == ',' }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(if (discountMode == DiscountMode.AMOUNT) "Discount amount" else "Discount percentage") },
                            supportingText = { Text(if (discountMode == DiscountMode.AMOUNT) "Maximum: ${money(subtotal)}" else "Maximum: 100%") }
                        )
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TOTALS", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subtotal"); Text(money(subtotal)) }
                        if (discount > 0.0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Discount"); Text("−${money(discount)}") }
                        if (tax > 0.0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("GST"); Text(money(tax)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Payable", fontWeight = FontWeight.Black); Text(money(total), fontWeight = FontWeight.Black) }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CUSTOMER", fontWeight = FontWeight.Bold)
                        Text(selectedCustomer?.let { "${it.name}${if (it.phone.isNotBlank()) " • ${it.phone}" else ""}" } ?: "Walk-in customer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showCustomerPicker = true }, enabled = !isProcessing) { Text("SELECT CUSTOMER") }
                            if (selectedCustomer != null) TextButton(onClick = { selectedCustomer = null }, enabled = !isProcessing) { Text("CLEAR") }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("PAYMENT METHOD", fontWeight = FontWeight.Bold)
                        PAYMENT_METHODS.forEach { method ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RadioButton(selected = paymentMethod == method, onClick = { paymentMethod = method }, enabled = !isProcessing)
                                Text(method, modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                }
            }
            item { if (paymentMethod == "CREDIT") Text("Credit sale will be added to this customer's Khata.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            pricingError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }
            if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, enabled = !isProcessing, modifier = Modifier.weight(1f).height(56.dp)) { Text("BACK") }
                    Button(
                        onClick = { onComplete(paymentMethod, selectedCustomer?.id, discount) },
                        enabled = cart.isNotEmpty() && !isProcessing && !creditWithoutCustomer && pricingPreview != null && pricingError == null,
                        modifier = Modifier.weight(1.4f).height(56.dp)
                    ) { Text(if (isProcessing) "PROCESSING…" else "COMPLETE SALE", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    if (showCustomerPicker) CustomerPickerDialog(customers, selectedCustomer?.id, { selectedCustomer = it; showCustomerPicker = false }, { selectedCustomer = null; showCustomerPicker = false }, { showCustomerPicker = false })
}

@Composable
private fun CustomerPickerDialog(customers: List<CustomerEntity>, selectedId: String?, onSelect: (CustomerEntity) -> Unit, onWalkIn: () -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = customers.filter { it.name.contains(query, true) || it.phone.contains(query, true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select customer") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Search") })
            TextButton(onClick = onWalkIn, modifier = Modifier.fillMaxWidth()) { Text("WALK-IN CUSTOMER") }
            if (filtered.isEmpty()) Text("No matching customers", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else filtered.take(8).forEach { customer -> TextButton(onClick = { onSelect(customer) }, modifier = Modifier.fillMaxWidth()) { Text(if (customer.id == selectedId) "✓ ${customer.name}" else customer.name) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
private fun cleanRate(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
