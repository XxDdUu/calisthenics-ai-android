package com.calisthenics.ai.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ForgotPasswordScreen(
    viewModel: PasswordResetViewModel,
    onOtpSent: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    BackHandler {
        if (!state.loading) {
            viewModel.cancelFlow()
            onBack()
        }
    }
    LaunchedEffect(state.requestCompleted) {
        if (state.requestCompleted) {
            viewModel.consumeRequestCompleted()
            onOtpSent()
        }
    }

    PasswordResetFrame(
        title = "Forgot password?",
        subtitle = "Enter the email address linked to your account.",
        onBack = { viewModel.cancelFlow(); onBack() },
        backEnabled = !state.loading
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        state.error?.let { ResetError(it) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::requestOtp,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !state.loading
        ) {
            if (state.loading) ResetLoadingIndicator() else Text("Send OTP")
        }
    }
}

@Composable
fun OtpVerificationScreen(
    viewModel: PasswordResetViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var otp by remember { mutableStateOf("") }
    BackHandler { if (!state.loading) onBack() }
    LaunchedEffect(state.verificationCompleted) {
        if (state.verificationCompleted) {
            viewModel.consumeVerificationCompleted()
            onVerified()
        }
    }

    PasswordResetFrame(
        title = "Verify your email",
        subtitle = "Enter the 6-digit code sent to ${state.email}.",
        onBack = onBack,
        backEnabled = !state.loading
    ) {
        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
        OutlinedTextField(
            value = otp,
            onValueChange = { value -> otp = value.filter { it in '0'..'9' }.take(6) },
            label = { Text("6-digit OTP") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (otp.isNotEmpty() && otp.length < 6) {
            Text(
                "Enter all 6 digits.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            text = "Code expires in ${formatCountdown(state.secondsRemaining)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        state.error?.let { ResetError(it) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.verifyOtp(otp) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !state.loading && otp.length == 6
        ) {
            if (state.loading) ResetLoadingIndicator() else Text("Verify OTP")
        }
        TextButton(
            onClick = viewModel::resendOtp,
            enabled = !state.loading && state.secondsRemaining == 0
        ) {
            Text(if (state.secondsRemaining == 0) "Resend OTP" else "Resend available in ${formatCountdown(state.secondsRemaining)}")
        }
    }
}

@Composable
fun ResetPasswordScreen(
    viewModel: PasswordResetViewModel,
    onResetComplete: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    BackHandler {
        if (!state.loading) {
            viewModel.clearResetAuthorization()
            onBack()
        }
    }
    LaunchedEffect(state.resetCompleted) {
        if (state.resetCompleted) {
            viewModel.consumeResetCompleted()
            onResetComplete()
        }
    }

    PasswordResetFrame(
        title = "Create a new password",
        subtitle = "Use a strong password that you have not used before.",
        onBack = {
            viewModel.clearResetAuthorization()
            onBack()
        },
        backEnabled = !state.loading
    ) {
        PasswordField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "New password",
            visible = showNewPassword,
            onVisibilityChange = { showNewPassword = !showNewPassword }
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm password",
            visible = showConfirmPassword,
            onVisibilityChange = { showConfirmPassword = !showConfirmPassword }
        )
        Text(
            "At least 8 characters, with uppercase and lowercase letters, a number, and a symbol.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 10.dp)
        )
        state.error?.let { ResetError(it) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.resetPassword(newPassword, confirmPassword) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !state.loading && newPassword.isNotBlank() && confirmPassword.isNotBlank()
        ) {
            if (state.loading) ResetLoadingIndicator() else Text("Reset password")
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onVisibilityChange: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = onVisibilityChange) {
                Text(if (visible) "Hide" else "Show")
            }
        }
    )
}

@Composable
private fun PasswordResetFrame(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onBack, enabled = backEnabled) { Text("Back to login") }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@Composable
private fun ResetError(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
private fun ResetLoadingIndicator() {
    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
}

private fun formatCountdown(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
