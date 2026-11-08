package com.example.healthsync.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthsync.R
import com.example.healthsync.data.local.SyncLogEntity
import com.example.healthsync.domain.model.SyncPayload
import kotlinx.serialization.json.Json
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    onLogout: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val df = remember { DateFormat.getDateTimeInstance() }
    val sheetState = rememberModalBottomSheetState()

    if (s.selectedLog != null) {
        SyncLogDetailSheet(
            log = s.selectedLog!!,
            onDismiss = { vm.selectLog(null) },
            sheetState = sheetState
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.sync_dashboard), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.logout),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onOpenPermissions) { Text(stringResource(R.string.permissions_tab), color = Color.White) }
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings_tab), color = Color.White) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.status_label),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.device_id_label, s.deviceId), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(
                            R.string.last_sync_label,
                            s.lastSuccessAt?.let { df.format(Date(it)) } ?: stringResource(R.string.never)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(stringResource(R.string.pending_batches_label, s.queueSize), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.sync_interval_label, s.intervalMinutes), style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Button(
                onClick = vm::syncNow,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) { 
                Text(stringResource(R.string.sync_now)) 
            }

            OutlinedButton(
                onClick = vm::sendTestData,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(R.string.send_test_data))
            }

            TextButton(
                onClick = vm::clearQueue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.clear_queue), color = MaterialTheme.colorScheme.error)
            }

            Text(
                stringResource(R.string.recent_activity),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(s.logs, key = { it.id }) { log ->
                        ListItem(
                            modifier = Modifier.clickable { vm.selectLog(log) },
                            headlineContent = {
                                Surface(
                                    color = if (log.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(if (log.success) R.string.success else R.string.failed),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (log.success) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            },
                            supportingContent = { 
                                Text(
                                    log.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) 
                            },
                            trailingContent = { 
                                Text(
                                    df.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                ) 
                            },
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLogDetailSheet(
    log: SyncLogEntity,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    val payload = remember(log.payloadJson) {
        log.payloadJson?.let {
            runCatching { Json.decodeFromString(SyncPayload.serializer(), it) }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.sync_log_detail_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider()

            if (payload != null) {
                DetailRow(stringResource(R.string.detail_steps), payload.steps.size.toString())
                DetailRow(stringResource(R.string.detail_heart_rate), payload.heartRate.size.toString())
                DetailRow(stringResource(R.string.detail_resting_heart_rate), payload.restingHeartRate.size.toString())
                DetailRow(stringResource(R.string.detail_blood_pressure), payload.bloodPressure.size.toString())
                DetailRow(stringResource(R.string.detail_blood_oxygen), payload.bloodOxygen.size.toString())
                DetailRow(stringResource(R.string.detail_sleep), payload.sleep.size.toString())
                
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.data_time_window),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Text(
                    text = stringResource(R.string.time_range_format, payload.window.from, payload.window.to),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = stringResource(R.string.log_details_not_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
