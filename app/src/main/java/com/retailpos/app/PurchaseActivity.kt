package com.retailpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.retailpos.app.data.ProductRepository
import com.retailpos.app.data.PurchaseRepository
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.ui.screens.PurchaseEntryScreen
import com.retailpos.app.ui.theme.RetailPosTheme

class PurchaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetailPosTheme {
                val context = LocalContext.current
                val database = remember(context) { RetailDatabase.get(context) }
                PurchaseEntryScreen(
                    storeId = "local-store",
                    repository = remember(database) { ProductRepository(database.productDao(), database.productBarcodeDao()) },
                    supplierDao = database.supplierDao(),
                    purchaseRepository = remember(database) { PurchaseRepository(database) },
                    onBack = { finish() },
                    onSaved = { finish() }
                )
            }
        }
    }
}
