package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.customer.KhataRules
import com.retailpos.app.data.CustomerDao
import com.retailpos.app.data.CustomerEntity
import com.retailpos.app.data.KhataDao
import com.retailpos.app.data.RetailDatabase
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Composable
fun CustomersScreen(storeId: String, onBack: () -> Unit, onOpenKhata: (String) -> Unit) {
    val context = LocalContext.current
    val database = remember(context) { RetailDatabase.get(context) }
    CustomersScreen(
        storeId = storeId,
        dao = database.customerDao(),
        khataDao = database.khataDao(),
        onOpenCustomer = { onOpenKhata(it.id) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    storeId: String,
    dao: CustomerDao,
    khataDao: KhataDao,
    onOpenCustomer: (CustomerEntity) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var deleteCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val customers by dao.search(storeId, query.trim()).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CUSTOMERS", fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } },
                actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "Add customer") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            actionError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search name or phone") }
                )
            }
            if (customers.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Text("No customers found", fontWeight = FontWeight.Bold)
                            Text("Add a customer to keep their details ready for billing and Khata.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showAdd = true }) { Text("ADD CUSTOMER") }
                        }
                    }
                }
            } else {
                items(customers, key = { it.id }) { customer ->
                    val balance by khataDao.observeBalance(storeId, customer.id).collectAsState(initial = 0.0)
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (customer.phone.isNotBlank()) Text(customer.phone)
                                Text(
                                    when (KhataRules.displayState(balance)) {
                                        com.retailpos.app.core.customer.KhataState.DUE -> "Due ${money(balance)}"
                                        com.retailpos.app.core.customer.KhataState.CREDIT -> "Credit ${money(-balance)}"
                                        com.retailpos.app.core.customer.KhataState.SETTLED -> "Settled"
                                        com.retailpos.app.core.customer.KhataState.INVALID -> "Invalid balance"
                                    },
                                    color = if (balance > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(onClick = { onOpenCustomer(customer) }) { Text("OPEN KHATA") }
                            }
                            IconButton(onClick = { deleteCustomer = customer; actionError = null }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${customer.name}")
                            }
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }

    if (showAdd) AddCustomerDialog(storeId, dao, { showAdd = false })
    deleteCustomer?.let { customer ->
        val balance by khataDao.observeBalance(storeId, customer.id).collectAsState(initial = 0.0)
        AlertDialog(
            onDismissRequest = { deleteCustomer = null },
            title = { Text("Delete customer?") },
            text = {
                Text(
                    if (balance == 0.0) "Remove ${customer.name} from this store's customer list?"
                    else "${customer.name} has an outstanding Khata balance of ${money(balance)}. Settle the balance before deleting this customer."
                )
            },
            confirmButton = {
                Button(
                    enabled = balance == 0.0,
                    onClick = {
                        scope.launch {
                            dao.delete(customer.id, storeId)
                            deleteCustomer = null
                        }
                    }
                ) { Text("DELETE") }
            },
            dismissButton = { TextButton(onClick = { deleteCustomer = null }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun AddCustomerDialog(storeId: String, dao: CustomerDao, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, singleLine = true, label = { Text("Phone (optional)") })
                OutlinedTextField(value = address, onValueChange = { address = it }, minLines = 2, label = { Text("Address (optional)") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cleanName = name.trim()
                val cleanPhone = phone.trim()
                if (cleanName.isBlank()) {
                    error = "Customer name is required."
                } else {
                    scope.launch {
                        try {
                            if (cleanPhone.isNotBlank() && dao.getByPhone(storeId, cleanPhone) != null) {
                                error = "A customer with this phone number already exists."
                            } else {
                                val now = System.currentTimeMillis()
                                dao.upsert(CustomerEntity(UUID.randomUUID().toString(), storeId, cleanName, cleanPhone, address.trim(), now, now))
                                onDismiss()
                            }
                        } catch (exception: Exception) {
                            error = exception.message ?: "Customer could not be saved."
                        }
                    }
                }
            }) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
