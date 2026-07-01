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

import androidx.compose.ui.res.stringResource
import com.example.healthsync.R

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
                Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.welcome_desc),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.onboarding_points),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().align(Alignment.End),
            ) { Text(stringResource(R.string.get_started)) }
        }
    }
}
