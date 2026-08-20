package com.retailpos.app

import androidx.compose.runtime.Composable
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

private const val LOCAL_STORE_ID = "local-store"

@Composable
fun StaffGatedRetailPosApp() {
    val context = LocalContext.current
    val database = remember(context) { RetailDatabase.get(context) }
    val repository = remember(database) { StaffRepository(database.staffDao()) }
    var session by remember { mutableStateOf(StaffSessionStore.current()) }
    var hasStaff by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(database) {
        val existing = StaffSessionStore.current()
        if (existing != null) {
            session = existing
            hasStaff = true
        } else {
            hasStaff = database.staffDao().list(LOCAL_STORE_ID).isNotEmpty()
        }
    }

    when (val current = session) {
        null -> if (hasStaff == null) {
            StaffAccessLoadingScreen()
        } else {
            StaffAccessScreen(
                repository = repository,
                hasStaff = hasStaff == true,
                onAuthenticated = { authenticated ->
                    StaffSessionStore.set(authenticated)
                    session = authenticated
                }
            )
        }
        else -> RetailPosApp(staffSession = current)
    }
}

@Composable
private fun StaffAccessLoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}
