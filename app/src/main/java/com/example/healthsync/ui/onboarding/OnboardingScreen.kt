package com.example.healthsync.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Welcome to HealthSync", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "HealthSync reads your heart rate and step data from Android Health Connect " +
                        "and securely uploads it to your configured server.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "• Data leaves your device only to the endpoint you configure.\n" +
                        "• Your API token is stored in encrypted storage.\n" +
                        "• Sync runs in the background on your chosen interval.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().align(Alignment.End),
            ) { Text("Get started") }
        }
    }
}
