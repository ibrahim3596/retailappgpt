package com.retailpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.retailpos.app.core.permissions.NavigationPermissionRules
import com.retailpos.app.core.staff.StaffSessionStore
import com.retailpos.app.data.ProductRepository
import com.retailpos.app.data.PurchaseRepository
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.ui.screens.PurchaseEntryScreen
import com.retailpos.app.ui.screens.SupplierPurchaseHistoryScreen
import com.retailpos.app.ui.theme.RetailPosTheme

class PurchaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = StaffSessionStore.current()?.role
        if (role == null || !NavigationPermissionRules.canOpenInventory(role)) {
            finish()
            return
        }
        setContent {
            RetailPosTheme {
                val context = LocalContext.current
                val database = remember(context) { RetailDatabase.get(context) }
                val repository = remember(database) { ProductRepository(database.productDao(), database.productBarcodeDao()) }
                val purchaseRepository = remember(database) { PurchaseRepository(database) }
                var showHistory by remember { mutableStateOf(false) }
                if (showHistory) {
                    SupplierPurchaseHistoryScreen(
                        storeId = "local-store",
                        supplierDao = database.supplierDao(),
                        purchaseDao = database.purchaseDao(),
                        supplierLedgerDao = database.supplierLedgerDao(),
                        onBack = { showHistory = false },
                        onNewPurchase = { showHistory = false }
                    )
                } else {
                    PurchaseEntryScreen(
                        storeId = "local-store",
                        repository = repository,
                        supplierDao = database.supplierDao(),
                        purchaseRepository = purchaseRepository,
                        onBack = { finish() },
                        onSaved = { showHistory = true },
                        onOpenHistory = { showHistory = true }
                    )
                }
            }
        }
    }
}
