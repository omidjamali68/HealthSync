package com.example.healthsync.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthsync.R
import com.example.healthsync.data.local.SyncLogEntity
import com.example.healthsync.domain.model.SyncPayload
import com.example.healthsync.ui.Routes
import com.example.healthsync.ui.components.AppBottomNavigation
import com.example.healthsync.util.DateUtils
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    onLogout: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val isPersian = DateUtils.isPersianMode()
    val sheetState = rememberModalBottomSheetState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        vm.logout()
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (s.selectedLog != null) {
        SyncLogDetailSheet(
            log = s.selectedLog!!,
            onDismiss = { vm.selectLog(null) },
            sheetState = sheetState
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sync_dashboard),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.logout),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onOpenPermissions,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.permissions_tab),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        onClick = onOpenSettings,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_tab),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            AppBottomNavigation(
                currentRoute = Routes.DASHBOARD,
                onNavigate = onNavigateToRoute
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero Status Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(
                                stringResource(R.string.status_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    StatusItemRow(
                        icon = Icons.Default.Devices,
                        label = stringResource(R.string.device_id_label, s.deviceId)
                    )
                    StatusItemRow(
                        icon = Icons.Default.CloudDone,
                        label = stringResource(
                            R.string.last_sync_label,
                            s.lastSuccessAt?.let { DateUtils.formatDateTime(it, isPersian) } ?: stringResource(R.string.never)
                        )
                    )
                    StatusItemRow(
                        icon = Icons.Default.FormatListNumbered,
                        label = stringResource(R.string.pending_batches_label, s.queueSize)
                    )
                    StatusItemRow(
                        icon = Icons.Default.AccessTime,
                        label = stringResource(R.string.sync_interval_label, s.intervalMinutes)
                    )
                }
            }
            
            // Quick Action Button
            Button(
                onClick = vm::syncNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.sync_now), fontWeight = FontWeight.Bold)
                }
            }

            if (s.isAdmin) {
                OutlinedButton(
                    onClick = vm::sendTestData,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.send_test_data))
                }
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(s.logs, key = { it.id }) { log ->
                        ListItem(
                            modifier = Modifier
                                .clickable { vm.selectLog(log) }
                                .padding(vertical = 2.dp),
                            headlineContent = {
                                Surface(
                                    color = if (log.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(if (log.success) R.string.success else R.string.failed),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
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
                                    DateUtils.formatDateTime(log.timestamp, isPersian),
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

@Composable
private fun StatusItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
                fontWeight = FontWeight.Bold,
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
                val isPersian = DateUtils.isPersianMode()
                Text(
                    text = stringResource(
                        R.string.time_range_format,
                        DateUtils.formatIsoDateTime(payload.window.from, isPersian),
                        DateUtils.formatIsoDateTime(payload.window.to, isPersian)
                    ),
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
