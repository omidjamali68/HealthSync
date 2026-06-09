package com.example.healthsync.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val df = remember { DateFormat.getDateTimeInstance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync dashboard") },
                actions = {
                    TextButton(onClick = onOpenPermissions) { Text("Permissions") }
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Text("Device ID: ${s.deviceId}")
                    Text("Last successful sync: ${s.lastSuccessAt?.let { df.format(Date(it)) } ?: "never"}")
                    Text("Pending batches in queue: ${s.queueSize}")
                    Text("Sync interval: ${s.intervalMinutes} min")
                }
            }
            Button(onClick = vm::syncNow, modifier = Modifier.fillMaxWidth()) { Text("Sync now") }

            Text("Recent activity", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.logs, key = { it.id }) { log ->
                    ListItem(
                        headlineContent = { Text(if (log.success) "Success" else "Failed") },
                        supportingContent = { Text(log.message) },
                        trailingContent = { Text(df.format(Date(log.timestamp))) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
