package com.example.healthsync.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.ui.res.stringResource
import com.example.healthsync.R

import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onContinue: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.api_configuration), color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = s.baseUrl, onValueChange = vm::setBaseUrl,
                label = { Text(stringResource(R.string.base_url)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.ingestPath, onValueChange = vm::setIngestPath,
                label = { Text(stringResource(R.string.ingest_path)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.token, onValueChange = vm::setToken,
                label = { Text(stringResource(R.string.auth_token)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.deviceId, onValueChange = vm::setDeviceId,
                label = { Text(stringResource(R.string.device_id)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.intervalMinutes, onValueChange = vm::setInterval,
                label = { Text(stringResource(R.string.sync_interval)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.language_selection), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = s.language.startsWith("fa"),
                    onClick = { vm.setLanguage("fa") },
                    label = { Text(stringResource(R.string.persian)) }
                )
                FilterChip(
                    selected = s.language.startsWith("en"),
                    onClick = { vm.setLanguage("en") },
                    label = { Text(stringResource(R.string.english)) }
                )
            }

            if (s.isSaved) {
                Text(stringResource(R.string.saved_msg), color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = vm::save, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save)) }
                Button(
                    onClick = { vm.save(); onContinue() },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.save_continue)) }
            }
        }
    }
}
