package com.example.healthsync.ui.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import com.example.healthsync.data.repository.HealthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onContinue: () -> Unit,
    vm: PermissionsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val contract = remember { PermissionController.createRequestPermissionResultContract() }
    val launcher = rememberLauncherForActivityResult(contract) { granted ->
        scope.launch { vm.onPermissionResult(granted.containsAll(vm.required)) }
    }

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Health Connect") }) }) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.availability) {
                HealthRepository.Availability.NotSupported ->
                    Text("Health Connect is not supported on this device.")
                HealthRepository.Availability.ProviderUpdateRequired ->
                    Text("Please install or update the Health Connect app from Play Store, then return here.")
                HealthRepository.Availability.Available -> {
                    Text(
                        "HealthSync needs permission to read:\n• Heart rate samples\n• Daily steps",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (state.granted) {
                        Text("All required permissions are granted.", color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = { launcher.launch(vm.required) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Grant Health Connect permissions") }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onContinue,
                enabled = state.granted || state.availability != HealthRepository.Availability.Available,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue") }
        }
    }
}
