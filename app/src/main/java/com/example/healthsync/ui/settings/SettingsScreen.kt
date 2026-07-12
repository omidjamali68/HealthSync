package com.example.healthsync.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
                title = { Text("تنظیمات", color = Color.White) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "تنظیمات همگام‌سازی",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = s.intervalMinutes, onValueChange = vm::setInterval,
                label = { Text(stringResource(R.string.sync_interval)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
