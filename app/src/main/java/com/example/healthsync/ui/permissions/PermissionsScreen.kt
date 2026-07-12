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

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.healthsync.R

import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onContinue: () -> Unit,
    vm: PermissionsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val contract = remember { PermissionController.createRequestPermissionResultContract() }
    val launcher = rememberLauncherForActivityResult(contract) { _ ->
        vm.refresh()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.health_connect), color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.availability) {
                HealthRepository.Availability.NotSupported ->
                    Text(stringResource(R.string.health_connect_not_supported))
                HealthRepository.Availability.ProviderUpdateRequired ->
                    Text(stringResource(R.string.health_connect_update_required))
                HealthRepository.Availability.Available -> {
                    Text(
                        stringResource(R.string.permissions_explanation),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (state.granted) {
                        Text(stringResource(R.string.all_permissions_granted), color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = { launcher.launch(vm.required) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.grant_permissions)) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onContinue,
                enabled = state.granted || state.availability != HealthRepository.Availability.Available,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.continue_button)) }
        }
    }
}
