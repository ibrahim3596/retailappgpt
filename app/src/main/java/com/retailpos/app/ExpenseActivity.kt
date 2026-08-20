package com.retailpos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.ui.screens.ExpenseScreen
import com.retailpos.app.ui.theme.RetailPosTheme

class ExpenseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetailPosTheme {
                val context = LocalContext.current
                val database = remember(context) { RetailDatabase.get(context) }
                ExpenseScreen(
                    storeId = "local-store",
                    expenseDao = database.expenseDao(),
                    onBack = { finish() }
                )
            }
        }
    }
}
