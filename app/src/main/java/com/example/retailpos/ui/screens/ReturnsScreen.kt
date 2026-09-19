package com.example.retailpos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.retailpos.data.local.entity.InvoiceWithItems
import com.example.retailpos.repository.PosRepository
import com.example.retailpos.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bill history + returns. A shopkeeper finds an original bill, selects the
 * items/quantities to return, and the repository performs the validated
 * refund (inventory restoration + khata reversal).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var returnTarget by remember { mutableStateOf<InvoiceWithItems?>(null) }

    val filtered = remember(invoices, searchQuery) {
        val q = searchQuery.trim()
        invoices.filter { inv ->
            inv.invoice.status != "CANCELLED" && (
                q.isBlank() ||
                    inv.invoice.invoiceNumber.contains(q, ignoreCase = true) ||
                    inv.invoice.customerName.contains(q, ignoreCase = true) ||
                    inv.invoice.customerPhone.contains(q)
                )
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("RETURNS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by bill number, customer or phone", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search bills", tint = TextSecondary) },
                shape = Shapes.card,
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "No bills", modifier = Modifier.size(64.dp), tint = TextSecondary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (invoices.isEmpty()) "No bills yet" else "No matching bills",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (invoices.isEmpty()) "Completed sales appear here and can be returned"
                            else "Try a different search",
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
                    items(filtered) { inv ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Surface,
                            shape = Shapes.card,
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineVariant),
                            onClick = { returnTarget = inv }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(inv.invoice.invoiceNumber, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        "${inv.invoice.customerName} · ${dateFormat.format(Date(inv.invoice.createdAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "${inv.items.size} items · ${inv.invoice.paymentMethod}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "₹${String.format("%,.2f", inv.invoice.grandTotal)}",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Open return", tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    returnTarget?.let { target ->
        ReturnDialog(
            target = target,
            onDismiss = { returnTarget = null },
            onConfirm = { items, reason ->
                scope.launch {
                    val refund = viewModel.processReturn(target.invoice.id, items, reason)
                    returnTarget = null
                    Toast.makeText(
                        context,
                        if (refund != null) "Return processed — refund ₹${String.format("%,.2f", refund)}"
                        else "Return rejected: check quantities",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}

@Composable
private fun ReturnDialog(
    target: InvoiceWithItems,
    onDismiss: () -> Unit,
    onConfirm: (items: List<PosRepository.ReturnItem>, reason: String) -> Unit
) {
    // Per-item return quantities, keyed by invoice item id.
    val returnQty = remember { mutableStateMapOf<String, Double>() }
    var reason by remember { mutableStateOf("") }

    val selected = returnQty.entries
        .filter { it.value > 0 }
        .map { PosRepository.ReturnItem(it.key, it.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return items — ${target.invoice.invoiceNumber}", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                target.items.forEach { item ->
                    val returnable = item.quantity - item.returnedQty
                    val entered = returnQty[item.id] ?: 0.0
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Sold ${formatQty(item.quantity)} · Already returned ${formatQty(item.returnedQty)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            if (returnable > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { returnQty[item.id] = (entered - 1.0).coerceIn(0.0, returnable) },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease return quantity", tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                                    Text(
                                        formatQty(entered),
                                        modifier = Modifier.widthIn(min = 32.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (entered > 0) Error else TextSecondary
                                    )
                                    IconButton(
                                        onClick = { returnQty[item.id] = (entered + 1.0).coerceAtMost(returnable) },
                                        modifier = Modifier.size(32.dp)
                                    ) { Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase return quantity", tint = Primary, modifier = Modifier.size(20.dp)) }
                                }
                            } else {
                                Text("Fully returned", style = MaterialTheme.typography.labelSmall, color = Success)
                            }
                        }
                        if (returnable > 1 && returnable % 1.0 != 0.0) {
                            // Fractional units (e.g. kg) — allow direct entry.
                            OutlinedTextField(
                                value = if (entered == 0.0) "" else formatQty(entered),
                                onValueChange = { txt ->
                                    txt.toDoubleOrNull()?.let { returnQty[item.id] = it.coerceIn(0.0, returnable) }
                                },
                                label = { Text("Return qty (max ${formatQty(returnable)})") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected, reason.ifBlank { "Customer return" }) },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = Shapes.pill
            ) { Text("PROCESS RETURN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

private fun formatQty(q: Double): String =
    if (q == q.toLong().toDouble()) q.toLong().toString() else String.format("%.2f", q)
