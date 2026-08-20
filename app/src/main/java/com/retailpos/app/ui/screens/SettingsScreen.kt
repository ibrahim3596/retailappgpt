package com.retailpos.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.products.StoreTaxMode
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.StoreSettingsEntity
import kotlinx.coroutines.launch

typealias GstMode = StoreTaxMode

private const val PREFS = "retailpos_settings"

private object Keys {
    const val STORE_NAME = "store_name"
    const val STORE_PHONE = "store_phone"
    const val CURRENCY = "currency"
    const val TAX_RATE = "tax_rate"
    const val GST_MODE = "gst_mode"
    const val RECEIPT_HEADER = "receipt_header"
    const val RECEIPT_FOOTER = "receipt_footer"
    const val DENSITY = "density"
}

data class LocalStoreSettings(
    val storeName: String,
    val storePhone: String,
    val currency: String,
    val taxRate: String,
    val gstMode: GstMode,
    val receiptHeader: String,
    val receiptFooter: String,
    val density: String
)

private fun Context.loadStoreSettings(): LocalStoreSettings {
    val p = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return LocalStoreSettings(
        storeName = p.getString(Keys.STORE_NAME, "") ?: "",
        storePhone = p.getString(Keys.STORE_PHONE, "") ?: "",
        currency = p.getString(Keys.CURRENCY, "INR") ?: "INR",
        taxRate = p.getString(Keys.TAX_RATE, "0") ?: "0",
        gstMode = StoreTaxMode.fromStorage(p.getString(Keys.GST_MODE, StoreTaxMode.NO_GST.storageValue) ?: StoreTaxMode.NO_GST.storageValue),
        receiptHeader = p.getString(Keys.RECEIPT_HEADER, "") ?: "",
        receiptFooter = p.getString(Keys.RECEIPT_FOOTER, "Thank you for shopping with us") ?: "Thank you for shopping with us",
        density = p.getString(Keys.DENSITY, "Standard") ?: "Standard"
    )
}

private fun Context.saveStoreSettings(settings: LocalStoreSettings) {
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString(Keys.STORE_NAME, settings.storeName.trim())
        .putString(Keys.STORE_PHONE, settings.storePhone.trim())
        .putString(Keys.CURRENCY, settings.currency.trim().uppercase())
        .putString(Keys.TAX_RATE, settings.taxRate.trim())
        .putString(Keys.GST_MODE, settings.gstMode.storageValue)
        .putString(Keys.RECEIPT_HEADER, settings.receiptHeader.trim())
        .putString(Keys.RECEIPT_FOOTER, settings.receiptFooter.trim())
        .putString(Keys.DENSITY, settings.density)
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(context: Context, onBack: () -> Unit) {
    val initial = remember(context) { context.loadStoreSettings() }
    val scope = rememberCoroutineScope()
    var storeName by remember { mutableStateOf(initial.storeName) }
    var storePhone by remember { mutableStateOf(initial.storePhone) }
    var currency by remember { mutableStateOf(initial.currency) }
    var taxRate by remember { mutableStateOf(initial.taxRate) }
    var gstMode by remember { mutableStateOf(initial.gstMode) }
    var receiptHeader by remember { mutableStateOf(initial.receiptHeader) }
    var receiptFooter by remember { mutableStateOf(initial.receiptFooter) }
    var density by remember { mutableStateOf(initial.density) }
    var saved by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("SETTINGS", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Store profile", fontWeight = FontWeight.Bold)
            OutlinedTextField(storeName, { storeName = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Store name") })
            OutlinedTextField(storePhone, { storePhone = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Store phone") })

            Text("Billing", fontWeight = FontWeight.Bold)
            OutlinedTextField(currency, { currency = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Currency code") })
            Text("GST status", fontWeight = FontWeight.Bold)
            GstMode.entries.forEach { mode ->
                OutlinedButton(onClick = { gstMode = mode; saved = false; validationError = null }, modifier = Modifier.fillMaxWidth(), enabled = !saving) {
                    Text(if (gstMode == mode) "✓ ${mode.label}" else mode.label)
                }
            }
            Text(gstMode.description, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                taxRate,
                { value -> taxRate = value.filter { it.isDigit() || it == '.' || it == ',' }; saved = false; validationError = null },
                Modifier.fillMaxWidth(), singleLine = true, label = { Text("Default tax rate (%)") }, enabled = gstMode == GstMode.REGULAR && !saving
            )

            Text("Receipt", fontWeight = FontWeight.Bold)
            OutlinedTextField(receiptHeader, { receiptHeader = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Receipt header") }, enabled = !saving)
            OutlinedTextField(receiptFooter, { receiptFooter = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Receipt footer") }, enabled = !saving)

            Text("Device", fontWeight = FontWeight.Bold)
            OutlinedTextField(density, { density = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Display density") }, enabled = !saving)

            Button(
                onClick = {
                    val parsedRate = taxRate.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (!parsedRate.isFinite() || parsedRate !in 0.0..100.0) {
                        validationError = "Tax rate must be between 0 and 100."
                        saved = false
                    } else {
                        saving = true
                        val effectiveRate = if (gstMode == GstMode.REGULAR) parsedRate else 0.0
                        val normalizedSettings = LocalStoreSettings(storeName, storePhone, currency.ifBlank { "INR" }, effectiveRate.toString(), gstMode, receiptHeader, receiptFooter, density.ifBlank { "Standard" })
                        context.saveStoreSettings(normalizedSettings)
                        scope.launch {
                            runCatching {
                                RetailDatabase.get(context).storeSettingsDao().upsert(
                                    StoreSettingsEntity(
                                        storeId = "local-store",
                                        gstMode = gstMode.storageValue,
                                        defaultTaxRatePercent = effectiveRate,
                                        currency = normalizedSettings.currency,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }.onSuccess {
                                validationError = null
                                saved = true
                            }.onFailure {
                                validationError = "Settings could not be persisted locally."
                                saved = false
                            }
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(), enabled = !saving
            ) { Text(if (saving) "SAVING…" else "SAVE SETTINGS") }

            validationError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            if (saved) Text("Saved locally on this device.")
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text("BACK") }
        }
    }
}
