package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.theme.Spacing
import com.example.ui.theme.IconSizes
import com.example.ui.theme.Shapes
import com.example.ui.theme.Elevation
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
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("KHATA LEDGER", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = Shapes.pill,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer") },
                text = { Text("ADD CUSTOMER", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            val totalCreditDue = remember(customers) { customers.sumOf { it.currentBalance } }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Primary)
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.xxl),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL OUTSTANDING", style = MaterialTheme.typography.labelSmall, color = OnPrimary.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text("₹${String.format("%,.2f", totalCreditDue)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = OnPrimary)
                    }
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = OnPrimary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Account Balance", tint = OnPrimary)
                        }
                    }
                }
            }

            Text("My Customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

            if (customers.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = "Customer List", modifier = Modifier.size(64.dp), tint = TextTertiary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text("No Khata Records", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(customers) { customer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.card,
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = Elevation.level1
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.cardPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("Ph: ${customer.phone}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    Text("Limit: ₹${customer.creditLimit.toInt()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                                }

                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                    Text("₹${String.format("%.2f", customer.currentBalance)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = if (customer.currentBalance > 0) Error else TextPrimary)
                                    OutlinedButton(
                                        onClick = { selectedCustomerForPayment = customer },
                                        contentPadding = PaddingValues(horizontal = Spacing.md),
                                        modifier = Modifier.height(32.dp),
                                        shape = Shapes.pill,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                                    ) {
                                        Text("PAYMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
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
            title = { Text("Add Khata Customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
                    )
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("Mobile Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
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
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    shape = Shapes.pill
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    if (selectedCustomerForPayment != null) {
        val cust = selectedCustomerForPayment!!
        AlertDialog(
            onDismissRequest = { selectedCustomerForPayment = null },
            title = { Text("Receive Payment from ${cust.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text("Current Outstanding Due: ₹${cust.currentBalance}", fontWeight = FontWeight.Bold, color = Warning, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Payment Amount Received (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
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
                        viewModel.recordKhataPaymentWithAuth(
                            customerId = cust.id,
                            amount = amount,
                            paymentMethod = com.example.retailpos.data.local.entity.PaymentMethod.CASH,
                            notes = "Khata Cash Payment",
                            onResult = { success ->
                                if (success) {
                                    selectedCustomerForPayment = null
                                    paymentAmountText = ""
                                    Toast.makeText(context, "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Payment rejected or unauthorized", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    shape = Shapes.pill
                ) {
                    Text("RECORD PAYMENT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCustomerForPayment = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }
}
