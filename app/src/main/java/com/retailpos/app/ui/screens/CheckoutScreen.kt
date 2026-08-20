package com.retailpos.app.ui.screens

import android.content.Intent
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
import com.retailpos.app.core.payment.PaymentSettlementRules
import com.retailpos.app.core.payment.PendingPaymentStore
import com.retailpos.app.core.payment.SplitPaymentPart
import com.retailpos.app.core.payment.SplitPaymentRules
import com.retailpos.app.core.payment.UpiPaymentIntent
import com.retailpos.app.core.permissions.StaffPermission
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.pos.CartLinePricingRules
import com.retailpos.app.core.staff.StaffRole
import com.retailpos.app.core.products.CheckoutPricingPreview
import com.retailpos.app.core.products.CheckoutPricingPreviewCalculator
import com.retailpos.app.core.products.StoreTaxMode
import com.retailpos.app.data.CartLine
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.RetailDatabase
import java.util.Locale
import java.util.UUID

private val PAYMENT_METHODS = listOf("CASH", "UPI", "CARD", "CREDIT", "SPLIT")
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
    error: String?,
    staffRole: StaffRole = StaffRole.CASHIER,
    onUpdateCartLine: (CartLine) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var paymentMethod by remember { mutableStateOf(PAYMENT_METHODS.first()) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var discountMode by remember { mutableStateOf(DiscountMode.AMOUNT) }
    var discountInput by remember { mutableStateOf("") }
    var cashTenderedInput by remember { mutableStateOf(PendingPaymentStore.get()?.let(::moneyValue) ?: "") }
    var splitSecondMethod by remember { mutableStateOf("UPI") }
    var splitFirstAmount by remember { mutableStateOf("") }
    var splitSecondAmount by remember { mutableStateOf("") }
    var pricingPreview by remember { mutableStateOf<CheckoutPricingPreview?>(null) }
    var pricingError by remember { mutableStateOf<String?>(null) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var upiError by remember { mutableStateOf<String?>(null) }
    var editingLine by remember { mutableStateOf<CartLine?>(null) }
    val creditWithoutCustomer = paymentMethod == "CREDIT" && selectedCustomer == null
    val canItemDiscount = StaffPermissionRules.hasPermission(staffRole, StaffPermission.APPLY_ITEM_DISCOUNT)
    val canPriceOverride = StaffPermissionRules.hasPermission(staffRole, StaffPermission.OVERRIDE_SELLING_PRICE)

    LaunchedEffect(cart, discountMode, discountInput) {
        if (cart.isEmpty()) { pricingPreview = null; return@LaunchedEffect }
        pricingError = null
        pricingPreview = null
        runCatching {
            val database = RetailDatabase.get(context)
            val settings = database.storeSettingsDao().get(LOCAL_STORE_ID)
            val taxMode = StoreTaxMode.fromStorage(settings?.gstMode ?: StoreTaxMode.NO_GST.storageValue)
            val rates = cart.associate { line -> line.productId to (database.productMetadataDao().get(line.productId, LOCAL_STORE_ID)?.taxRatePercent ?: 0.0) }
            val subtotal = cart.sumOf { it.lineTotal }
            val raw = discountInput.replace(',', '.').toDoubleOrNull() ?: 0.0
            require(raw.isFinite() && raw >= 0.0) { "Discount must be a non-negative number." }
            val discountAmount = when (discountMode) {
                DiscountMode.AMOUNT -> raw
                DiscountMode.PERCENT -> { require(raw <= 100.0) { "Discount percentage cannot exceed 100%." }; subtotal * raw / 100.0 }
            }
            CheckoutPricingPreviewCalculator.calculate(cart, taxMode.toTaxTreatment(), rates, discountAmount)
        }.onSuccess { pricingPreview = it }.onFailure { pricingError = it.message ?: "Unable to calculate checkout pricing." }
    }

    val total = pricingPreview?.total ?: cart.sumOf { it.lineTotal }
    val subtotal = pricingPreview?.subtotal ?: cart.sumOf { it.lineTotal }
    val discount = pricingPreview?.discountAmount ?: 0.0
    val tax = pricingPreview?.taxAmount ?: 0.0
    val tendered = cashTenderedInput.replace(',', '.').toDoubleOrNull()
    val splitAmount1 = splitFirstAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val splitAmount2 = splitSecondAmount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val splitEncoded = if (paymentMethod == "SPLIT") {
        val parts = listOf(SplitPaymentPart("CASH", splitAmount1), SplitPaymentPart(splitSecondMethod, splitAmount2))
        SplitPaymentRules.validate(total, parts)?.also { paymentError = it }
        if (SplitPaymentRules.validate(total, parts) == null) SplitPaymentRules.encode(parts) else null
    } else null
    val settlementMethod = splitEncoded ?: paymentMethod
    val paymentPreview = runCatching { PaymentSettlementRules.settle(settlementMethod, total, if (paymentMethod == "CASH") tendered else null) }.getOrNull()
    val change = paymentPreview?.change ?: 0.0

    LaunchedEffect(paymentMethod, cashTenderedInput, total, splitFirstAmount, splitSecondAmount, splitSecondMethod) {
        paymentError = runCatching {
            val method = if (paymentMethod == "SPLIT") {
                val parts = listOf(SplitPaymentPart("CASH", splitAmount1), SplitPaymentPart(splitSecondMethod, splitAmount2))
                SplitPaymentRules.validate(total, parts)?.let { throw IllegalArgumentException(it) }
                SplitPaymentRules.encode(parts)
            } else paymentMethod
            PaymentSettlementRules.settle(method, total, if (paymentMethod == "CASH") tendered else null)
        }.exceptionOrNull()?.message
        if (paymentMethod != "SPLIT") upiError = null
    }

    fun openUpiApp() {
        try {
            val settings = RetailDatabase.get(context).storeSettingsDao().get(LOCAL_STORE_ID)
            val vpa = settings?.upiVpa.orEmpty()
            if (vpa.isBlank()) {
                upiError = "Set your merchant UPI VPA in Settings first."
                return
            }
            val uri = UpiPaymentIntent.build(vpa = vpa, payeeName = "RetailPOS Store", amount = total, transactionRef = UUID.randomUUID().toString())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(context.packageManager) == null) upiError = "No installed UPI app can handle payment requests on this device."
            else { context.startActivity(intent); upiError = null }
        } catch (e: Exception) { upiError = e.message ?: "UPI app could not be opened." }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("CHECKOUT", fontWeight = FontWeight.Black) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("BILL SUMMARY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            items(cart, key = { it.productId }) { line ->
                val previewLine = pricingPreview?.lines?.firstOrNull { it.productId == line.productId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(line.name, fontWeight = FontWeight.Bold)
                                Text("${line.quantity.clean()} ${line.unit} × ${money(line.effectiveUnitPrice)}")
                                if (line.overrideUnitPrice != null) Text("Base price ${money(line.unitPrice)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (line.itemDiscountAmount > 0.0) Text("Item discount −${money(line.itemDiscountAmount)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(money(previewLine?.total ?: line.lineTotal), fontWeight = FontWeight.Bold)
                                if (canItemDiscount || canPriceOverride) TextButton(onClick = { editingLine = line }, enabled = !isProcessing) { Text("EDIT") }
                            }
                        }
                        if (previewLine != null && previewLine.taxAmount > 0.0) Text("GST ${cleanRate(previewLine.taxRatePercent)}% • ${money(previewLine.taxAmount)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        OutlinedTextField(discountInput, { discountInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(if (discountMode == DiscountMode.AMOUNT) "Discount amount" else "Discount percentage") })
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
                        Text("PAYMENT", fontWeight = FontWeight.Bold)
                        PAYMENT_METHODS.forEach { method ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RadioButton(selected = paymentMethod == method, onClick = { paymentMethod = method; paymentError = null }, enabled = !isProcessing)
                                Text(method, modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                        when (paymentMethod) {
                            "CASH" -> {
                                OutlinedTextField(cashTenderedInput, { cashTenderedInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; paymentError = null }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cash received") })
                                if (change > 0.0) Text("Change: ${money(change)}", fontWeight = FontWeight.Bold)
                            }
                            "UPI" -> {
                                Text("Collect exactly ${money(total)} using UPI.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(onClick = ::openUpiApp, enabled = !isProcessing, modifier = Modifier.fillMaxWidth()) { Text("OPEN UPI APP") }
                                Text("After completing payment, return here and press COMPLETE SALE.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                upiError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                            }
                            "CARD" -> Text("Collect exactly ${money(total)} using CARD.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            "CREDIT" -> Text("Credit sale will be added to this customer's Khata.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            "SPLIT" -> {
                                Text("Split between CASH and a second tender.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(splitFirstAmount, { splitFirstAmount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Cash part") })
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("UPI", "CARD").forEach { method ->
                                        OutlinedButton(onClick = { splitSecondMethod = method }, enabled = !isProcessing, modifier = Modifier.weight(1f)) { Text(if (splitSecondMethod == method) "✓ $method" else method) }
                                    }
                                }
                                OutlinedTextField(splitSecondAmount, { splitSecondAmount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("$splitSecondMethod part") })
                                Text("Remaining: ${money((total - splitAmount1 - splitAmount2).coerceAtLeast(0.0))}", fontWeight = FontWeight.Bold)
                            }
                        }
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
            pricingError?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }
            paymentError?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }
            if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, enabled = !isProcessing, modifier = Modifier.weight(1f).height(56.dp)) { Text("BACK") }
                    Button(onClick = { PendingPaymentStore.set(if (paymentMethod == "CASH") tendered else null); onComplete(settlementMethod, selectedCustomer?.id, discount) }, enabled = cart.isNotEmpty() && !isProcessing && !creditWithoutCustomer && pricingPreview != null && pricingError == null && paymentError == null, modifier = Modifier.weight(1.4f).height(56.dp)) { Text(if (isProcessing) "PROCESSING…" else "COMPLETE SALE", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    if (showCustomerPicker) CustomerPickerDialog(customers, selectedCustomer?.id, { selectedCustomer = it; showCustomerPicker = false }, { selectedCustomer = null; showCustomerPicker = false }, { showCustomerPicker = false })
    editingLine?.let { line ->
        ItemPricingDialog(
            line = line,
            allowDiscount = canItemDiscount,
            allowPriceOverride = canPriceOverride,
            staffRole = staffRole,
            onDismiss = { editingLine = null },
            onSave = { updated -> onUpdateCartLine(updated); editingLine = null }
        )
    }
}

@Composable
private fun ItemPricingDialog(
    line: CartLine,
    allowDiscount: Boolean,
    allowPriceOverride: Boolean,
    staffRole: StaffRole,
    onDismiss: () -> Unit,
    onSave: (CartLine) -> Unit
) {
    var overrideInput by remember(line.productId, line.overrideUnitPrice) { mutableStateOf(line.overrideUnitPrice?.let { moneyValue(it) } ?: "") }
    var discountInput by remember(line.productId, line.itemDiscountAmount) { mutableStateOf(if (line.itemDiscountAmount == 0.0) "" else moneyValue(line.itemDiscountAmount)) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EDIT ${line.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Base selling price: ${money(line.unitPrice)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (allowPriceOverride) {
                    OutlinedTextField(overrideInput, { overrideInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Override unit price (blank = base)") })
                }
                if (allowDiscount) {
                    OutlinedTextField(discountInput, { discountInput = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Item discount amount") })
                }
                Text("Role: ${staffRole.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val override = if (allowPriceOverride && overrideInput.isNotBlank()) overrideInput.replace(',', '.').toDoubleOrNull() else null
                    val discount = if (allowDiscount) discountInput.replace(',', '.').toDoubleOrNull() ?: 0.0 else 0.0
                    val updated = CartLinePricingRules.apply(line, override, discount, staffRole)
                    onSave(updated)
                } catch (e: Exception) { error = e.message ?: "Pricing change is invalid." }
            }) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun CustomerPickerDialog(customers: List<CustomerEntity>, selectedId: String?, onSelect: (CustomerEntity) -> Unit, onWalkIn: () -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = customers.filter { it.name.contains(query, true) || it.phone.contains(query, true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select customer") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("Search") }); TextButton(onClick = onWalkIn, modifier = Modifier.fillMaxWidth()) { Text("WALK-IN CUSTOMER") }; if (filtered.isEmpty()) Text("No matching customers", color = MaterialTheme.colorScheme.onSurfaceVariant) else filtered.take(8).forEach { customer -> TextButton(onClick = { onSelect(customer) }, modifier = Modifier.fillMaxWidth()) { Text(if (customer.id == selectedId) "✓ ${customer.name}" else customer.name) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
}

private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
private fun moneyValue(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun cleanRate(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
private fun Double.clean(): String = if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.2f", this)
