package com.retailpos.app.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.backup.DatabaseBackupManager
import com.retailpos.app.core.products.StoreTaxMode
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.StoreSettingsEntity
import kotlinx.coroutines.launch

typealias GstMode = StoreTaxMode

private const val PREFS = "retailpos_settings"
private const val LOCAL_STORE_ID = "local-store"
private object Keys {
    const val STORE_NAME = "store_name"
    const val STORE_PHONE = "store_phone"
    const val CURRENCY = "currency"
    const val TAX_RATE = "tax_rate"
    const val GST_MODE = "gst_mode"
    const val RECEIPT_HEADER = "receipt_header"
    const val RECEIPT_FOOTER = "receipt_footer"
    const val DENSITY = "density"
    const val UPI_VPA = "upi_vpa"
}

data class LocalStoreSettings(
    val storeName: String,
    val storePhone: String,
    val currency: String,
    val taxRate: String,
    val gstMode: GstMode,
    val receiptHeader: String,
    val receiptFooter: String,
    val density: String,
    val upiVpa: String
)

private fun Context.loadStoreSettings(): LocalStoreSettings {
    val p = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return LocalStoreSettings(
        p.getString(Keys.STORE_NAME, "") ?: "",
        p.getString(Keys.STORE_PHONE, "") ?: "",
        p.getString(Keys.CURRENCY, "INR") ?: "INR",
        p.getString(Keys.TAX_RATE, "0") ?: "0",
        StoreTaxMode.fromStorage(p.getString(Keys.GST_MODE, StoreTaxMode.NO_GST.storageValue) ?: StoreTaxMode.NO_GST.storageValue),
        p.getString(Keys.RECEIPT_HEADER, "") ?: "",
        p.getString(Keys.RECEIPT_FOOTER, "Thank you for shopping with us") ?: "Thank you for shopping with us",
        p.getString(Keys.DENSITY, "Standard") ?: "Standard",
        p.getString(Keys.UPI_VPA, "") ?: ""
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
        .putString(Keys.UPI_VPA, settings.upiVpa.trim())
        .apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(context: Context, onBack: () -> Unit) {
    val initial = remember(context) { context.loadStoreSettings() }
    val database = remember(context) { RetailDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var storeName by remember { mutableStateOf(initial.storeName) }
    var storePhone by remember { mutableStateOf(initial.storePhone) }
    var currency by remember { mutableStateOf(initial.currency) }
    var taxRate by remember { mutableStateOf(initial.taxRate) }
    var gstMode by remember { mutableStateOf(initial.gstMode) }
    var receiptHeader by remember { mutableStateOf(initial.receiptHeader) }
    var receiptFooter by remember { mutableStateOf(initial.receiptFooter) }
    var density by remember { mutableStateOf(initial.density) }
    var upiVpa by remember { mutableStateOf(initial.upiVpa) }
    var backupPassword by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(database) {
        runCatching { database.storeSettingsDao().get(LOCAL_STORE_ID) }
            .getOrNull()
            ?.let { persisted ->
                gstMode = StoreTaxMode.fromStorage(persisted.gstMode)
                taxRate = persisted.defaultTaxRatePercent.toString()
                upiVpa = persisted.upiVpa
            }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null && backupPassword.length >= 8) {
            scope.launch {
                backupStatus = "Creating encrypted backup…"
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        DatabaseBackupManager.exportEncrypted(context, output, backupPassword.toCharArray())
                    } ?: error("Could not open the selected backup destination.")
                }.onSuccess { backupStatus = "Encrypted backup exported successfully." }
                    .onFailure { backupStatus = it.message ?: "Backup export failed." }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && backupPassword.length >= 8) {
            scope.launch {
                backupStatus = "Restoring encrypted backup…"
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input -> DatabaseBackupManager.importEncrypted(context, input, backupPassword.toCharArray()) }
                        ?: error("Could not open the selected backup.")
                }.onSuccess {
                    backupStatus = "Backup restored. Restart the app before continuing to use it."
                }.onFailure { backupStatus = it.message ?: "Backup restore failed." }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("SETTINGS", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            Text(gstMode.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(taxRate, { value -> taxRate = value.filter { it.isDigit() || it == '.' || it == ',' }; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Default tax rate (%)") }, enabled = gstMode == GstMode.REGULAR && !saving)
            OutlinedTextField(upiVpa, { upiVpa = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Merchant UPI VPA") }, enabled = !saving)

            Text("Receipt", fontWeight = FontWeight.Bold)
            OutlinedTextField(receiptHeader, { receiptHeader = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Receipt header") }, enabled = !saving)
            OutlinedTextField(receiptFooter, { receiptFooter = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Receipt footer") }, enabled = !saving)

            Text("Device", fontWeight = FontWeight.Bold)
            OutlinedTextField(density, { density = it; saved = false }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Display density") }, enabled = !saving)

            Button(onClick = {
                val parsedRate = taxRate.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (!parsedRate.isFinite() || parsedRate !in 0.0..100.0) {
                    validationError = "Tax rate must be between 0 and 100."
                    saved = false
                } else {
                    saving = true
                    val effectiveRate = if (gstMode == GstMode.REGULAR) parsedRate else 0.0
                    val normalized = LocalStoreSettings(storeName, storePhone, currency.ifBlank { "INR" }, effectiveRate.toString(), gstMode, receiptHeader, receiptFooter, density.ifBlank { "Standard" }, upiVpa)
                    context.saveStoreSettings(normalized)
                    scope.launch {
                        runCatching {
                            database.storeSettingsDao().upsert(StoreSettingsEntity(LOCAL_STORE_ID, gstMode.storageValue, effectiveRate, normalized.currency, System.currentTimeMillis(), normalized.upiVpa))
                        }.onSuccess { validationError = null; saved = true }.onFailure { validationError = "Settings could not be persisted locally." }
                        saving = false
                    }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text(if (saving) "SAVING…" else "SAVE SETTINGS") }

            Text("BACKUP & RESTORE", fontWeight = FontWeight.Bold)
            OutlinedTextField(backupPassword, { backupPassword = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Backup password (8+ characters)") })
            RowButtons(
                exportEnabled = backupPassword.length >= 8,
                onExport = { exportLauncher.launch("retailpos-backup-${System.currentTimeMillis()}.rpbak") },
                onImport = { importLauncher.launch(arrayOf("application/octet-stream", "application/*")) }
            )
            Text("Backups are encrypted before leaving the device. Keep the password safe; it is required to restore the backup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            backupStatus?.let { Text(it, fontWeight = FontWeight.Bold) }

            validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            if (saved) Text("Saved locally on this device.")
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text("BACK") }
        }
    }
}

@Composable
private fun RowButtons(exportEnabled: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onExport, enabled = exportEnabled, modifier = Modifier.weight(1f)) { Text("EXPORT BACKUP") }
        OutlinedButton(onClick = onImport, enabled = exportEnabled, modifier = Modifier.weight(1f)) { Text("IMPORT BACKUP") }
    }
}