package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.ExpenseEntity
import com.example.retailpos.ui.MainViewModel
import com.example.retailpos.ui.components.MetricTile
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val opResult by viewModel.expenseOpResult.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Surface operation results as toasts, then clear.
    LaunchedEffect(opResult) {
        opResult?.let { ok ->
            Toast.makeText(context, if (ok) "Saved" else "Could not save expense", Toast.LENGTH_SHORT).show()
            viewModel.clearExpenseOpResult()
        }
    }

    val categories = remember(expenses) {
        expenses.map { it.category }.distinct().sorted()
    }

    val filteredExpenses = remember(expenses, searchQuery, categoryFilter) {
        expenses.filter { e ->
            (categoryFilter == null || e.category == categoryFilter) &&
                (searchQuery.isBlank() ||
                    e.category.contains(searchQuery, ignoreCase = true) ||
                    e.notes.contains(searchQuery, ignoreCase = true))
        }
    }

    val totalThisMonth = remember(expenses) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        expenses.filter { it.date >= cal.timeInMillis }.sumOf { it.amount }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("EXPENSES", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
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
            Column {
                Text("EXPENSES", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Track shop operating costs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "This Month",
                    value = "₹${String.format("%,.0f", totalThisMonth)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = Error,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Total Entries",
                    value = "${expenses.size}",
                    icon = Icons.Default.ReceiptLong,
                    iconColor = Primary,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by category or notes", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search expenses", tint = TextSecondary) },
                shape = Shapes.card,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OutlineVariant,
                    unfocusedContainerColor = Surface,
                    focusedContainerColor = Surface
                ),
                singleLine = true
            )

            // Category filter chips
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { categoryFilter = null },
                        label = { Text("All") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                            label = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "No expenses", modifier = Modifier.size(64.dp), tint = TextSecondary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (expenses.isEmpty()) "No expenses recorded" else "No matching expenses",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (expenses.isEmpty()) "Tap the button below to record your first expense"
                            else "Try a different search or filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredExpenses) { expense ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Surface,
                            shape = Shapes.card,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                expense.category.lowercase().replaceFirstChar { it.uppercase() },
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (expense.syncStatus == "PENDING") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = Warning.copy(alpha = 0.15f)) {
                                                    Text(
                                                        "OFFLINE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Warning,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(dateFormat.format(Date(expense.date)), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        if (expense.notes.isNotBlank())
                                            Text(expense.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
                                        Text(expense.paymentMethod, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "-₹${String.format("%,.2f", expense.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            color = Error
                                        )
                                        if (expense.syncStatus != "SYNCED") {
                                            Row {
                                                IconButton(onClick = { editingExpense = expense }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit expense", tint = Primary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(onClick = {
                                                    scope.launch { viewModel.deleteUnsyncedExpense(expense.id) }
                                                }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete unsynced expense", tint = Error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = Shapes.pill,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                text = { Text("ADD EXPENSE", fontWeight = FontWeight.Bold) }
            )
        }
    }

    if (showAddDialog || editingExpense != null) {
        val existing = editingExpense
        ExpenseDialog(
            existing = existing,
            onDismiss = {
                showAddDialog = false
                editingExpense = null
            },
            onSave = { category, amount, date, paymentMethod, notes ->
                if (existing != null) {
                    viewModel.updateExpense(existing.copy(category = category, amount = amount, date = date, paymentMethod = paymentMethod, notes = notes))
                } else {
                    viewModel.addExpense(category, amount, date, paymentMethod, notes)
                }
                showAddDialog = false
                editingExpense = null
            }
        )
    }
}

@Composable
private fun ExpenseDialog(
    existing: ExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double, date: Long, paymentMethod: String, notes: String) -> Unit
) {
    val categories = listOf("RENT", "ELECTRICITY", "TRANSPORT", "SALARIES", "REPAIRS", "PACKAGING", "SUPPLIES", "MARKETING", "MISC")
    val paymentMethods = listOf("CASH", "UPI", "CARD", "OTHER")

    var category by remember { mutableStateOf(existing?.category ?: "MISC") }
    var amountText by remember { mutableStateOf(existing?.amount?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: "CASH") }
    var expanded by remember { mutableStateOf(false) }
    var pmExpanded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull()
    val isValid = category.isNotBlank() && amount != null && amount > 0 && amount.isFinite()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Expense" else "Edit Expense", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = category.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .then(Modifier.clip(RoundedCornerShape(Shapes.medium)))
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { category = cat; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotEmpty() && !isValid,
                    supportingText = if (amountText.isNotEmpty() && !isValid) {
                        { Text("Enter a positive amount") }
                    } else null
                )
                ExposedDropdownMenuBox(expanded = pmExpanded, onExpandedChange = { pmExpanded = it }) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid via") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pmExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().clip(RoundedCornerShape(Shapes.medium))
                    )
                    ExposedDropdownMenu(expanded = pmExpanded, onDismissRequest = { pmExpanded = false }) {
                        paymentMethods.forEach { pm ->
                            DropdownMenuItem(text = { Text(pm) }, onClick = { paymentMethod = pm; pmExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(category, amount!!, System.currentTimeMillis(), paymentMethod, notes) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = Shapes.pill
            ) {
                Text(if (existing == null) "ADD" else "SAVE")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
