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
import androidx.compose.material3.Surface
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
    CustomersScreen(storeId, database.customerDao(), database.khataDao(), onOpenKhata, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomersScreen(storeId: String, dao: CustomerDao, khataDao: KhataDao, onOpenCustomer: (CustomerEntity) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var deleteCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val customers by dao.search(storeId, query.trim()).collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Column { Text("Customers", fontWeight = FontWeight.SemiBold); Text("Profiles & Khata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } }, actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "Add customer") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actionError?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Search name or phone") }) }
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text("Khata first", fontWeight = FontWeight.SemiBold); Text("Outstanding balances stay visible while you work.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (customers.isEmpty()) item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Person, null); Text("No customers found", fontWeight = FontWeight.SemiBold); Text(if (query.isBlank()) "Add the customer once; they can be selected at billing." else "Try another name or phone number.", color = MaterialTheme.colorScheme.onSurfaceVariant); if (query.isBlank()) Button(onClick = { showAdd = true }) { Text("ADD CUSTOMER") } }
                }
            } else items(customers, key = { it.id }) { customer ->
                val balance by khataDao.observeBalance(storeId, customer.id).collectAsState(initial = 0.0)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); if (customer.phone.isNotBlank()) Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(if (balance > 0.0) money(balance) else if (balance < 0.0) "Credit ${money(-balance)}" else "Settled", fontWeight = FontWeight.Bold, color = when (KhataRules.displayState(balance)) { com.retailpos.app.core.customer.KhataState.DUE -> MaterialTheme.colorScheme.error; com.retailpos.app.core.customer.KhataState.CREDIT -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurfaceVariant })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onOpenCustomer(customer) }, modifier = Modifier.weight(1f)) { Text("OPEN KHATA") }; IconButton(onClick = { deleteCustomer = customer; actionError = null }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${customer.name}") } }
                    }
                }
            }
            item { OutlinedButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("BACK") } }
        }
    }

    if (showAdd) AddCustomerDialog(storeId, dao) { showAdd = false }
    deleteCustomer?.let { customer ->
        val balance by khataDao.observeBalance(storeId, customer.id).collectAsState(initial = 0.0)
        AlertDialog(onDismissRequest = { deleteCustomer = null }, title = { Text("Delete customer?") }, text = { Text(if (balance == 0.0) "Remove ${customer.name} from this store's customer list?" else "${customer.name} has ${money(balance)} outstanding. Settle the balance before deleting this customer.") }, confirmButton = { Button(enabled = balance == 0.0, onClick = { scope.launch { dao.delete(customer.id, storeId); deleteCustomer = null } }) { Text("DELETE") } }, dismissButton = { TextButton(onClick = { deleteCustomer = null }) { Text("CANCEL") } })
    }
}

@Composable
private fun AddCustomerDialog(storeId: String, dao: CustomerDao, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope(); var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add customer") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true); OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") }, singleLine = true); OutlinedTextField(address, { address = it }, label = { Text("Address (optional)") }, minLines = 2); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } }, confirmButton = { Button(onClick = { val n=name.trim(); val p=phone.trim(); if(n.isBlank()) error="Customer name is required." else scope.launch { if(p.isNotBlank() && dao.getByPhone(storeId,p)!=null) error="A customer with this phone number already exists." else { val now=System.currentTimeMillis(); dao.upsert(CustomerEntity(UUID.randomUUID().toString(),storeId,n,p,address.trim(),now,now)); onDismiss() } } }) { Text("SAVE") } }, dismissButton = { TextButton(onClick=onDismiss){Text("CANCEL")} })
}
private fun money(value: Double): String = String.format(Locale.US, "₹%.2f", value)
