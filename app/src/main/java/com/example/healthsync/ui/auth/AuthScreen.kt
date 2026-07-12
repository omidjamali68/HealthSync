package com.example.healthsync.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Green Header like the website
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "طب دیتا",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!uiState.isCodeSent) {
                        LoginStage(uiState, viewModel)
                    } else {
                        VerificationStage(uiState, viewModel)
                    }
                }
            }

            uiState.error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun LoginStage(state: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "ورود به اپلیکیشن",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedTextField(
        value = state.mobileNumber,
        onValueChange = viewModel::onMobileNumberChange,
        label = { Text("شماره موبایل") },
        placeholder = { Text("09175001122", color = Color.Gray) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = viewModel::sendCode,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = !state.isLoading && state.mobileNumber.length == 11,
        shape = MaterialTheme.shapes.medium
    ) {
        Text("دریافت کد تایید", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun VerificationStage(state: AuthUiState, viewModel: AuthViewModel) {
    Text(
        text = "کد تایید را وارد کنید",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "کد به شماره ${state.mobileNumber} ارسال شد",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(32.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val focusRequesters = remember { List(6) { FocusRequester() } }

        state.verificationCode.forEachIndexed { index, char ->
            OutlinedTextField(
                value = char,
                onValueChange = {
                    if (it.length <= 1) {
                        viewModel.onVerificationCodeChange(index, it)
                        if (it.isNotEmpty() && index < 5) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    }
                },
                modifier = Modifier
                    .width(45.dp)
                    .focusRequester(focusRequesters[index]),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    val minutes = state.timerSeconds / 60
    val seconds = state.timerSeconds % 60
    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        style = MaterialTheme.typography.titleMedium,
        color = if (state.timerSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = viewModel::login,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = !state.isLoading && state.verificationCode.all { it.isNotEmpty() },
        shape = MaterialTheme.shapes.medium
    ) {
        Text("تایید و ورود")
    }

    if (state.timerSeconds == 0) {
        TextButton(onClick = viewModel::sendCode) {
            Text("ارسال مجدد کد", color = MaterialTheme.colorScheme.secondary)
        }
    }
}
