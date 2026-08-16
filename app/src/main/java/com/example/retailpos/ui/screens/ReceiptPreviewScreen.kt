package com.example.retailpos.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.retailpos.data.local.entity.InvoiceEntity
import com.example.retailpos.data.local.entity.InvoiceItemEntity
import com.example.retailpos.engine.printer.EscPosReceiptGenerator
import com.example.retailpos.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewScreen(
    invoiceId: String,
    viewModel: MainViewModel,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    var invoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var items by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }

    LaunchedEffect(invoiceId) {
        val storeId = store?.id ?: "STORE-DEFAULT-001"
        invoice = viewModel.db.invoiceDao().getInvoiceById(storeId, invoiceId)
        items = viewModel.db.invoiceItemDao().getInvoiceItems(invoiceId)
    }

    val receiptText = remember(store, invoice, items) {
        val st = store
        val inv = invoice
        if (st != null && inv != null) {
            EscPosReceiptGenerator.generateTextReceipt(st, inv, items)
        } else {
            "Loading receipt..."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Invoice Receipt", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, receiptText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Invoice via WhatsApp/SMS"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RetailPrimary)
            )
        },
        bottomBar = {
            Surface(
                color = RetailBackground,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorder)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHARE", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val st = store
                            val inv = invoice
                            if (st != null && inv != null) {
                                val bytes = EscPosReceiptGenerator.generateEscPosCommands(st, inv, items)
                                Toast.makeText(context, "ESC/POS Thermal Print Commands Sent (${bytes.size} bytes)", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailPrimary),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PRINT (ESC/POS)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(RetailBackground)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RetailSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetailBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = receiptText,
                    modifier = Modifier.padding(20.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = RetailTextPrimary
                )
            }
        }
    }
}
