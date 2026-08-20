package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.staff.StaffRole
import com.retailpos.app.data.StaffRepository
import com.retailpos.app.data.StaffSignInResult
import kotlinx.coroutines.launch

@Composable
fun StaffAccessScreen(
    repository: StaffRepository,
    hasStaff: Boolean,
    onAuthenticated: (com.retailpos.app.core.staff.StaffSession) -> Unit
) {
    var mode by remember(hasStaff) { mutableStateOf(if (hasStaff) AccessMode.SIGN_IN else AccessMode.SETUP) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf(if (hasStaff) "" else "owner") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(hasStaff) {
        mode = if (hasStaff) AccessMode.SIGN_IN else AccessMode.SETUP
        if (hasStaff) {
            name = ""
            confirmPin = ""
        }
    }

    fun submit() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            runCatching {
                if (mode == AccessMode.SETUP) {
                    require(pin == confirmPin) { "PINs do not match." }
                    repository.createStaff("local-store", name, username, pin, StaffRole.OWNER)
                }
                repository.signIn("local-store", username, pin)
            }.onSuccess { result ->
                when (result) {
                    is StaffSignInResult.Success -> onAuthenticated(result.session)
                    StaffSignInResult.InvalidCredentials -> error = "Invalid username or PIN."
                    StaffSignInResult.Locked -> error = "Account locked for 5 minutes after repeated failed PIN attempts."
                    StaffSignInResult.Inactive -> error = "This staff account is inactive."
                }
            }.onFailure { error = it.message ?: "Unable to sign in." }
            busy = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (mode == AccessMode.SETUP) "SET UP OWNER" else "STAFF LOGIN", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (mode == AccessMode.SETUP) "Create the first local owner account. This stays on the device." else "Sign in with your shop staff PIN.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (mode == AccessMode.SETUP) {
                OutlinedTextField(name, { name = it; error = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Owner name") }, enabled = !busy)
            }
            OutlinedTextField(username, { username = it; error = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Username") }, enabled = !busy)
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8); error = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("PIN (4–8 digits)") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), enabled = !busy)
            if (mode == AccessMode.SETUP) {
                OutlinedTextField(confirmPin, { confirmPin = it.filter(Char::isDigit).take(8); error = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Confirm PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), enabled = !busy)
            }
            Button(
                onClick = ::submit,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "PLEASE WAIT…" else if (mode == AccessMode.SETUP) "CREATE OWNER" else "SIGN IN") }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

private enum class AccessMode { SETUP, SIGN_IN }
