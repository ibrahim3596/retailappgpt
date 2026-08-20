package com.retailpos.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.retailpos.app.core.staff.StaffSession
import com.retailpos.app.core.staff.StaffSessionStore
import com.retailpos.app.data.RetailDatabase
import com.retailpos.app.data.StaffRepository
import com.retailpos.app.ui.screens.StaffAccessScreen
import com.retailpos.app.ui.theme.RetailPosTheme

private const val LOCAL_STORE_ID = "local-store"

class StaffGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RetailPosTheme {
                val context = LocalContext.current
                val database = remember(context) { RetailDatabase.get(context) }
                val repository = remember(database) { StaffRepository(database.staffDao()) }
                var hasStaff by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    hasStaff = database.staffDao().list(LOCAL_STORE_ID).isNotEmpty()
                }

                hasStaff?.let { existingStaff ->
                    StaffAccessScreen(
                        repository = repository,
                        hasStaff = existingStaff,
                        onAuthenticated = ::openMainApp
                    )
                }
            }
        }
    }

    private fun openMainApp(session: StaffSession) {
        StaffSessionStore.set(session)
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        finish()
    }
}
