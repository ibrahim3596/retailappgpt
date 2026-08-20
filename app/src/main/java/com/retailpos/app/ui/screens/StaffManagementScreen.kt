package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.permissions.StaffPermissionRules
import com.retailpos.app.core.permissions.StaffRole
import com.retailpos.app.data.StaffEntity
import com.retailpos.app.data.StaffManagementRepository
import com.retailpos.app.data.StaffRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    storeId: String,
    manager: StaffManagementRepository,
    staffRepository: StaffRepository,
    canManage: Boolean,
    onBack: () -> Unit
) {
    var staff by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() { staff = manager.list(storeId) }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("STAFF", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!canManage) {
                Text("Only the owner can manage staff.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            } else {
                Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) { Text("ADD STAFF") }
            }
            if (message != null) Text(message!!, color = MaterialTheme.colorScheme.primary)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(staff, key = { it.id }) { member ->
                    var actionBusy by remember(member.id) { mutableStateOf(false) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(member.name, fontWeight = FontWeight.Bold)
                                    Text("@${member.username} • ${member.role}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(if (member.active) "ACTIVE" else "INACTIVE", color = if (member.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            if (canManage && !actionBusy) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        actionBusy = true
                                        androidx.compose.runtime.LaunchedEffect(member.active) { }
                                    }) { Text(if (member.active) "DISABLE" else "ENABLE") }
                                    OutlinedButton(onClick = {
                                        actionBusy = true
                                    }) { Text("RESET PIN") }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StaffRole.entries.forEach { role ->
                                        TextButton(enabled = role.name != member.role, onClick = {
                                            actionBusy = true
                                        }) { Text(role.name) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddStaffDialog(
            onDismiss = { showAdd = false },
            onCreate = { name, username, pin, role ->
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
                    runCatching { staffRepository.createStaff(storeId, name, username, pin, role) }
                        .onSuccess { showAdd = false; message = "Staff account created." }
                        .onFailure { message = it.message ?: "Unable to create staff." }
                    staff = manager.list(storeId)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, StaffRole) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add staff") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, Modifier.fillMaxWidth(), label = { Text("PIN (4–8 digits)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = role.name, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), label = { Text("Role") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        StaffRole.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = { role = option; expanded = false }) }
                    }
                }
                Text("${role.name} discount limit: ${StaffPermissionRules.billDiscountAuthorization(role).maxPercent}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, username, pin, role) }) { Text("CREATE") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
