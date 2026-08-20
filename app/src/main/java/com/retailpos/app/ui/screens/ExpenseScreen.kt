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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.core.expenses.ExpenseRules
import com.retailpos.app.data.ExpenseDao
import com.retailpos.app.data.ExpenseEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun ExpenseScreen(
    storeId: String,
    expenseDao: ExpenseDao,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var expenses by remember { mutableStateOf<List<ExpenseEntity>>(emptyList()) }
    var showEditor by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    suspend fun reload() { expenses = expenseDao.recent(storeId, 100) }
    LaunchedEffect(Unit) { reload() }

    val total = expenses.sumOf { it.amount }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("EXPENSES", fontWeight = FontWeight.Black) },
            navigationIcon = { TextButton(onClick = onBack) { Text("BACK") } }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("RECENT EXPENSES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("₹${money(total)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Showing the latest ${expenses.size} entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Button(onClick = { showEditor = true }, modifier = Modifier.fillMaxWidth()) { Text("ADD EXPENSE") }
            }
            if (expenses.isEmpty()) {
                item { Text("No expenses recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(expense.category, fontWeight = FontWeight.Bold)
                                Text("₹${money(expense.amount)}", fontWeight = FontWeight.Black)
                            }
                            Text("${expense.paymentMethod} • ${expense.note.ifBlank { "No note" }}")
                            Text(formatter.format(Date(expense.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ExpenseEditorDialog(
            onDismiss = { showEditor = false },
            onSave = { amount, category, paymentMethod, note ->
                scope.launch {
                    expenseDao.insert(
                        ExpenseEntity(
                            id = UUID.randomUUID().toString(),
                            storeId = storeId,
                            amount = amount,
                            category = category,
                            note = note,
                            paymentMethod = paymentMethod,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    showEditor = false
                    reload()
                }
            }
        )
    }
}

@Composable
private fun ExpenseEditorDialog(
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseRules.categories.first()) }
    var paymentMethod by remember { mutableStateOf(ExpenseRules.paymentMethods.first()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ADD EXPENSE") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amountText, { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, singleLine = true, label = { Text("Amount") })
                Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseRules.categories.take(4).forEach { value ->
                        if (category == value) Button(onClick = { category = value }, modifier = Modifier.weight(1f)) { Text(value.take(5)) }
                        else OutlinedButton(onClick = { category = value }, modifier = Modifier.weight(1f)) { Text(value.take(5)) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseRules.categories.drop(4).forEach { value ->
                        if (category == value) Button(onClick = { category = value }, modifier = Modifier.weight(1f)) { Text(value.take(5)) }
                        else OutlinedButton(onClick = { category = value }, modifier = Modifier.weight(1f)) { Text(value.take(5)) }
                    }
                }
                Text("Payment", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpenseRules.paymentMethods.forEach { value ->
                        if (paymentMethod == value) Button(onClick = { paymentMethod = value }, modifier = Modifier.weight(1f)) { Text(value) }
                        else OutlinedButton(onClick = { paymentMethod = value }, modifier = Modifier.weight(1f)) { Text(value) }
                    }
                }
                OutlinedTextField(note, { note = it }, minLines = 2, label = { Text("Note") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.replace(',', '.').toDoubleOrNull()
                val validation = ExpenseRules.validate(amount ?: Double.NaN, category, paymentMethod, note)
                if (validation != null) error = validation else onSave(amount!!, category, paymentMethod, note.trim())
            }) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
