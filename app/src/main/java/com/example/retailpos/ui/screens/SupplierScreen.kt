package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.SupplierEntity
import com.example.retailpos.data.local.entity.PurchaseEntity
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.Screen
import com.example.retailpos.ui.components.MetricTile
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSupplierDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true) ||
            it.contactPerson.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("SUPPLIERS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Header
            Column {
                Text("SUPPLIERS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Manage supplier accounts and contacts", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            // Summary Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "Total Suppliers",
                    value = "${suppliers.size}",
                    icon = Icons.Default.Business,
                    iconColor = Primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name, phone or email", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search supplier", tint = TextSecondary) },
                shape = Shapes.card,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineVariant,
                    unfocusedContainerColor = Surface,
                    focusedContainerColor = Surface
                ),
                singleLine = true
            )

            // Supplier List
            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Business, contentDescription = "供应商", modifier = Modifier.size(64.dp), tint = TextSecondary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No suppliers found", fontWeight = FontWeight.Bold)
                        Text("Tap the button below to add your first supplier", style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSuppliers) { supplier ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSupplierDetail(supplier.id) }
                                .padding(16.dp),
                            color = Surface,
                            shape = Shapes.card,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(supplier.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (supplier.contactPerson.isNotBlank())
                                        Text("Contact: ${supplier.contactPerson}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    if (supplier.phone.isNotBlank())
                                        Text(supplier.phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    if (supplier.email.isNotBlank())
                                        Text(supplier.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    if (supplier.gstin.isNotBlank())
                                        Text("GSTIN: ${supplier.gstin}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Balance: ₹${String.format("%,.2f", supplier.currentBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (supplier.currentBalance > 0) Error else Success)
                                    Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Add Supplier FAB
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = Shapes.pill,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Supplier") },
                text = { Text("ADD SUPPLIER", fontWeight = FontWeight.Bold) }
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Supplier", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name *") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.medium)
                    OutlinedTextField(value = contactPerson, onValueChange = { contactPerson = it }, label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.medium)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.medium, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.medium, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email))
                    OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.medium)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = Shapes.medium, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val currentStore = store
                    if (name.isNotBlank() && currentStore != null) {
                        val newSupplier = SupplierEntity(
                            id = UUID.randomUUID().toString(),
                            storeId = currentStore.id,
                            name = name,
                            contactPerson = contactPerson,
                            phone = phone,
                            email = email,
                            gstin = gstin,
                            address = address
                        )
                        scope.launch { viewModel.saveSupplier(newSupplier) }
                        showAddDialog = false
                        Toast.makeText(context, "Supplier added", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = Shapes.pill) {
                    Text("ADD")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("CANCEL") } }
        )
    }
}