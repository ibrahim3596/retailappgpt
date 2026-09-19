package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.HorizontalDivider
import com.example.retailpos.data.local.entity.PurchaseEntity
import com.example.retailpos.data.local.entity.SupplierEntity
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.MetricTile
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    viewModel: MainViewModel,
    supplierId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    val supplier = remember(suppliers, supplierId) {
        suppliers.find { it.id == supplierId }
    }

    val purchases by viewModel.getPurchasesForSupplier(supplierId).collectAsStateWithLifecycle()
    val totalPurchases by viewModel.getTotalPurchasesForSupplier(supplierId).collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var contactPerson by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var gstin by remember { mutableStateOf(supplier?.gstin ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(supplier?.name ?: "Supplier Details", fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        floatingActionButton = {
            if (supplier != null && showEditDialog.not()) {
                ExtendedFloatingActionButton(
                    onClick = { showEditDialog = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    shape = Shapes.pill,
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit Supplier") },
                    text = { Text("EDIT", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            supplier?.let { supplier ->
                // Header with Summary
                Column {
                    Text("SUPPLIER PROFILE", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, color = TextSecondary, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Summary Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        label = "Total Purchases",
                        value = "${purchases.size}",
                        icon = Icons.Default.ShoppingCart,
                        iconColor = Primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Total Amount",
                        value = "₹${String.format("%,.0f", totalPurchases)}",
                        icon = Icons.Default.CurrencyRupee,
                        iconColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Current Balance",
                        value = "₹${String.format("%,.0f", supplier.currentBalance)}",
                        icon = Icons.Default.AccountBalance,
                        iconColor = if (supplier.currentBalance > 0) Error else Success,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Contact Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                ) {
                    Column(modifier = Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Primary.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Business, contentDescription = "Supplier", modifier = Modifier.size(20.dp), tint = Primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("CONTACT INFORMATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(Spacing.sm))

                        if (supplier.contactPerson.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Contact Person", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(supplier.contactPerson, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        if (supplier.phone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Phone", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(supplier.phone, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        if (supplier.email.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Email", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(supplier.email, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        if (supplier.gstin.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GSTIN", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(supplier.gstin, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                        if (supplier.address.isNotBlank()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Text("Address", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(supplier.address, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }

                // Purchase History
                if (purchases.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PURCHASE HISTORY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = TextSecondary, letterSpacing = 0.5.sp)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(purchases) { purchase ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = Shapes.card,
                                    colors = CardDefaults.cardColors(containerColor = Surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = purchase.invoiceNumber.ifEmpty { "PO #${purchase.id.takeLast(6)}" },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = sdf.format(Date(purchase.createdAt)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "₹${String.format("%,.2f", purchase.totalAmount)}",
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF2563EB),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty purchase history
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.xxxl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = SurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Purchase", modifier = Modifier.size(32.dp), tint = TextSecondary.copy(alpha = 0.2f))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No purchases yet", fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("Add purchase orders to track history", style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && supplier != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Supplier", fontWeight = FontWeight.Black) },
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
                    if (name.isNotBlank() && supplier != null) {
                        val updated = supplier.copy(
                            name = name,
                            contactPerson = contactPerson,
                            phone = phone,
                            email = email,
                            gstin = gstin,
                            address = address,
                            updatedAt = System.currentTimeMillis()
                        )
                        scope.launch { viewModel.saveSupplier(updated) }
                        showEditDialog = false
                        Toast.makeText(context, "Supplier updated", Toast.LENGTH_SHORT).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = Shapes.pill) {
                    Text("SAVE")
                }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("CANCEL") } }
        )
    }
}