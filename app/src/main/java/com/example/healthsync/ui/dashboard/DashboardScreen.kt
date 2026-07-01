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

import androidx.compose.ui.res.stringResource
import com.example.healthsync.R

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
                title = { Text(stringResource(R.string.sync_dashboard)) },
                actions = {
                    TextButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions_tab)) }
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings_tab)) }
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
                    Text(stringResource(R.string.status_label), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.device_id_label, s.deviceId))
                    Text(
                        stringResource(
                            R.string.last_sync_label,
                            s.lastSuccessAt?.let { df.format(Date(it)) } ?: stringResource(R.string.never)
                        )
                    )
                    Text(stringResource(R.string.pending_batches_label, s.queueSize))
                    Text(stringResource(R.string.sync_interval_label, s.intervalMinutes))
                }
            }
            Button(onClick = vm::syncNow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sync_now)) }

            OutlinedButton(onClick = vm::sendTestData, modifier = Modifier.fillMaxWidth()) {
                Text("ارسال دیتای تست (همه پارامترها)")
            }

            Text(stringResource(R.string.recent_activity), style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.logs, key = { it.id }) { log ->
                    ListItem(
                        headlineContent = { Text(stringResource(if (log.success) R.string.success else R.string.failed)) },
                        supportingContent = { Text(log.message) },
                        trailingContent = { Text(df.format(Date(log.timestamp))) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
