package com.example.healthsync.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DateFormat
import java.util.Date

import androidx.compose.ui.res.stringResource
import com.example.healthsync.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp

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
                Text("ارسال دیتای تست (همه پارامترها)")
            }

            TextButton(
                onClick = vm::clearQueue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("پاکسازی صف انتظار", color = MaterialTheme.colorScheme.error)
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
