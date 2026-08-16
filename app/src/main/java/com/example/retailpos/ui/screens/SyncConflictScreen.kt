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
import com.example.retailpos.data.local.entity.SyncConflictEntity
import com.example.retailpos.engine.sync.SyncEngine
import com.example.retailpos.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncConflictScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val conflicts by viewModel.unresolvedConflicts.collectAsStateWithLifecycle()
    val store by viewModel.currentStore.collectAsStateWithLifecycle()

    val syncEngine = remember { SyncEngine(context, viewModel.db.invoiceDao(), viewModel.db.syncDao()) }
    var selectedConflict by remember { mutableStateOf<SyncConflictEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Device Sync & Conflicts", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Emerald600)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Emerald600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Multi-Device Offline Synchronization", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Invoices and stock deltas synchronize safely across multiple mobile devices using persistent idempotency.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Unresolved Conflicts (${conflicts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            if (conflicts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = Emerald600)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All devices in sync!", fontWeight = FontWeight.Bold)
                        Text("No conflicting invoice or stock updates found.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conflicts) { conflict ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Conflict ID: ${conflict.entityId}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("Reason: ${conflict.conflictReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { selectedConflict = conflict },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                                ) {
                                    Text("RESOLVE CONFLICT")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedConflict != null) {
        val conflict = selectedConflict!!
        AlertDialog(
            onDismissRequest = { selectedConflict = null },
            title = { Text("Resolve Conflict Side-by-Side") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reason: ${conflict.conflictReason}", style = MaterialTheme.typography.bodySmall, color = Color.Red)

                    Text("LOCAL VERSION:", fontWeight = FontWeight.Bold)
                    Surface(color = Color.LightGray.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small) {
                        Text(conflict.localDataJson, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                    }

                    Text("SERVER VERSION:", fontWeight = FontWeight.Bold)
                    Surface(color = Color.LightGray.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small) {
                        Text(conflict.serverDataJson, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            syncEngine.resolveConflict(conflict.id, "SERVER_WINS", conflict)
                            selectedConflict = null
                            Toast.makeText(context, "Conflict resolved (Server Wins)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("USE SERVER VERSION")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        scope.launch {
                            syncEngine.resolveConflict(conflict.id, "LOCAL_WINS", conflict)
                            selectedConflict = null
                            Toast.makeText(context, "Conflict resolved (Local Wins)", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("KEEP LOCAL VERSION")
                }
            }
        )
    }
}
