package com.example.healthsync.ui.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthsync.R
import com.example.healthsync.util.SmsOtpParser
import com.example.healthsync.util.SmsOtpReceiver

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
        // Green Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.teb_data),
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
                Text(text = it.asString(), color = MaterialTheme.colorScheme.error)
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
        text = stringResource(R.string.login_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedTextField(
        value = state.mobileNumber,
        onValueChange = viewModel::onMobileNumberChange,
        label = { Text(stringResource(R.string.mobile_number)) },
        placeholder = { Text(stringResource(R.string.mobile_placeholder), color = Color.Gray) },
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
        Text(stringResource(R.string.send_code), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun VerificationStage(state: AuthUiState, viewModel: AuthViewModel) {
    val context = LocalContext.current

    val smsConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsOtpReceiver.EXTRA_SMS_MESSAGE)
            val code = SmsOtpParser.extractOtpCode(message)
            if (code != null) {
                viewModel.onOtpAutoFilled(code)
            }
        }
    }

    DisposableEffect(context) {
        SmsOtpReceiver.startSmsUserConsent(context)
        val receiver = SmsOtpReceiver.register(context) { consentIntent: Intent ->
            smsConsentLauncher.launch(consentIntent)
        }
        onDispose {
            SmsOtpReceiver.unregister(context, receiver)
        }
    }

    Text(
        text = stringResource(R.string.enter_verification_code),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.code_sent_to, state.mobileNumber),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.waiting_for_sms),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary
    )
    Spacer(modifier = Modifier.height(24.dp))

    OtpInputField(
        otpText = state.verificationCode,
        onOtpTextChange = viewModel::onVerificationCodeChange
    )

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
        enabled = !state.isLoading && state.verificationCode.length == 6,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(stringResource(R.string.verify_and_login))
    }

    if (state.timerSeconds == 0) {
        TextButton(onClick = viewModel::sendCode) {
            Text(stringResource(R.string.resend_code), color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun OtpInputField(
    otpText: String,
    otpCount: Int = 6,
    onOtpTextChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = otpText,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                if (filtered.length <= otpCount) {
                    onOtpTextChange(filtered)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.focusRequester(focusRequester),
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(otpCount) { index ->
                        val char = when {
                            index < otpText.length -> otpText[index].toString()
                            else -> ""
                        }
                        val isFocused = index == otpText.length || (index == otpCount - 1 && otpText.length == otpCount)

                        OutlinedTextField(
                            value = char,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = if (isFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.width(45.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp
                            ),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }
            }
        )
    }
}
