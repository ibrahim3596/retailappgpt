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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val PREFS = "retailpos_settings"

private object Keys {
    const val STORE_NAME = "store_name"
    const val STORE_PHONE = "store_phone"
    const val CURRENCY = "currency"
    const val TAX_RATE = "tax_rate"
    const val RECEIPT_HEADER = "receipt_header"
    const val RECEIPT_FOOTER = "receipt_footer"
    const val DENSITY = "density"
}

data class LocalStoreSettings(
    val storeName: String,
    val storePhone: String,
    val currency: String,
    val taxRate: String,
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
        .putString(Keys.RECEIPT_HEADER, settings.receiptHeader.trim())
        .putString(Keys.RECEIPT_FOOTER, settings.receiptFooter.trim())
        .putString(Keys.DENSITY, settings.density)
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(context: Context, onBack: () -> Unit) {
    var storeName by remember { mutableStateOf(context.loadStoreSettings().storeName) }
    var storePhone by remember { mutableStateOf(context.loadStoreSettings().storePhone) }
    var currency by remember { mutableStateOf(context.loadStoreSettings().currency) }
    var taxRate by remember { mutableStateOf(context.loadStoreSettings().taxRate) }
    var receiptHeader by remember { mutableStateOf(context.loadStoreSettings().receiptHeader) }
    var receiptFooter by remember { mutableStateOf(context.loadStoreSettings().receiptFooter) }
    var density by remember { mutableStateOf(context.loadStoreSettings().density) }
    var saved by remember { mutableStateOf(false) }

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
            OutlinedTextField(taxRate, { taxRate = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Default tax rate (%)") })

            Text("Receipt", fontWeight = FontWeight.Bold)
            OutlinedTextField(receiptHeader, { receiptHeader = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Receipt header") })
            OutlinedTextField(receiptFooter, { receiptFooter = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Receipt footer") })

            Text("Device", fontWeight = FontWeight.Bold)
            OutlinedTextField(density, { density = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Display density") })

            Button(
                onClick = {
                    context.saveStoreSettings(LocalStoreSettings(storeName, storePhone, currency.ifBlank { "INR" }, taxRate.ifBlank { "0" }, receiptHeader, receiptFooter, density.ifBlank { "Standard" }))
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("SAVE SETTINGS") }

            if (saved) Text("Saved locally on this device.")
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") }
        }
    }
}
