package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.CustomerEntity
import com.example.retailpos.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }

    var selectedCustomerForPayment by remember { mutableStateOf<CustomerEntity?>(null) }
    var paymentAmountText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = RetailBackground,
        topBar = {
            TopAppBar(
                title = { Text("KHATA LEDGER", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = RetailPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("ADD CUSTOMER") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val totalCreditDue = remember(customers) { customers.sumOf { it.currentBalance } }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RetailPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL OUTSTANDING", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text("₹${String.format("%,.2f", totalCreditDue)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Text("My Customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RetailTextPrimary)

            if (customers.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = RetailTextSecondary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Khata Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = RetailTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(customers) { customer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = RetailSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = RetailTextPrimary)
                                    Text("Ph: ${customer.phone}", style = MaterialTheme.typography.bodySmall, color = RetailTextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Limit: ₹${customer.creditLimit.toInt()}", style = MaterialTheme.typography.labelSmall, color = RetailTextSecondary, fontWeight = FontWeight.Bold)
                                }

                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("₹${String.format("%.2f", customer.currentBalance)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = if (customer.currentBalance > 0) RetailError else RetailTextPrimary)
                                    OutlinedButton(
                                        onClick = { selectedCustomerForPayment = customer },
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorder)
                                    ) {
                                        Text("PAYMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Add Khata Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Mobile Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameText.isBlank() || phoneText.isBlank()) {
                            Toast.makeText(context, "Please enter customer name and phone!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val storeId = store?.id ?: "STORE-DEFAULT-001"
                        val newCust = CustomerEntity(
                            id = UUID.randomUUID().toString(),
                            storeId = storeId,
                            name = nameText,
                            phone = phoneText,
                            currentBalance = 0.0
                        )
                        scope.launch {
                            viewModel.customerRepo.saveCustomer(newCust)
                            showAddCustomerDialog = false
                            nameText = ""
                            phoneText = ""
                            Toast.makeText(context, "Customer added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (selectedCustomerForPayment != null) {
        val cust = selectedCustomerForPayment!!
        AlertDialog(
            onDismissRequest = { selectedCustomerForPayment = null },
            title = { Text("Receive Payment from ${cust.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current Outstanding Due: ₹${cust.currentBalance}", fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Payment Amount Received (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toDoubleOrNull() ?: 0.0
                        if (amount <= 0) {
                            Toast.makeText(context, "Enter a valid payment amount!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val storeId = store?.id ?: "STORE-DEFAULT-001"
                        scope.launch {
                            viewModel.customerRepo.recordPayment(storeId, cust.id, amount, "Khata Cash Payment")
                            selectedCustomerForPayment = null
                            paymentAmountText = ""
                            Toast.makeText(context, "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("RECORD PAYMENT")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCustomerForPayment = null }) { Text("CANCEL") }
            }
        )
    }
}
