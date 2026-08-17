package com.retailpos.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retailpos.app.data.ReceiptFormatter
import com.retailpos.app.data.SaleEntity
import com.retailpos.app.data.SaleLineEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    sale: SaleEntity,
    lines: List<SaleLineEntity>,
    onBack: () -> Unit,
    onShare: (String) -> Unit
) {
    val receipt = ReceiptFormatter.format(sale, lines)
    Scaffold(topBar = { TopAppBar(title = { Text("RECEIPT", fontWeight = FontWeight.Black) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(receipt, modifier = Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("DONE") }
                Button(onClick = { onShare(receipt) }, modifier = Modifier.weight(1f)) { Text("SHARE") }
            }
        }
    }
}
