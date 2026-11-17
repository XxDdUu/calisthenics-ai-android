package com.calisthenics.ai.ui.auth

import com.calisthenics.ai.R

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import kotlinx.coroutines.delay

/**
 * Mask an email address to protect privacy.
 * E.g., athlete@example.com -> at***e@example.com
 */
fun maskEmail(email: String): String {
    if (email.isBlank() || !email.contains("@")) return email
    val parts = email.split("@", limit = 2)
    val local = parts[0]
    val domain = parts[1]
    val maskedLocal = when {
        local.length <= 1 -> "$local***"
        local.length == 2 -> "${local.first()}***"
        else -> "${local.first()}***${local.last()}"
    }
    return "$maskedLocal@$domain"
}

fun formatCountdown(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)

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

    PasswordResetContainer(
        onBack = {
            viewModel.cancelFlow()
            onBack()
        },
        backEnabled = !state.loading
    ) {
        Text(
            text = stringResource(R.string.forgot_password),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.loc_enter_reset_email),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = { Text(stringResource(R.string.loc_email_address)) },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = stringResource(R.string.loc_email_icon), tint = SecondaryText)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Elevated,
                unfocusedContainerColor = Elevated,
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border,
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.requestOtp() }
            )
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(it)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = viewModel::requestOtp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !state.loading && state.email.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Ink,
                disabledContainerColor = Border,
                disabledContentColor = SecondaryText
            )
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.loc_send_verification_code), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OtpVerificationScreen(
    viewModel: PasswordResetViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    onChangeEmail: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    var otp by remember { mutableStateOf("") }

    // Redirect back if email is empty (prevent direct URL/route bypass)
    LaunchedEffect(state.email) {
        if (state.email.isBlank()) {
            onBack()
        }
    }

    BackHandler {
        if (!state.verifying && !state.loading && !state.resending) {
            onBack()
        }
    }

    LaunchedEffect(state.verificationCompleted) {
        if (state.verificationCompleted) {
            delay(300)
            viewModel.consumeVerificationCompleted()
            onVerified()
        }
    }

    PasswordResetContainer(
        onBack = onBack,
        backEnabled = !state.verifying && !state.resending && !state.loading
    ) {
        Text(
            text = stringResource(R.string.verify_email),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.enter_verification_code),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )

        Spacer(Modifier.height(16.dp))

        // Masked Email Card with Change Email action
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = stringResource(R.string.email),
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = maskEmail(state.email),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                }
                TextButton(
                    onClick = { onChangeEmail?.invoke() ?: onBack() },
                    enabled = !state.verifying && !state.resending && !state.loading
                ) {
                    Text(
                        text = stringResource(R.string.loc_change),
                        color = Accent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // 6-digit OTP Input
        val isInputError = state.status == OtpStatus.INVALID_OTP || state.status == OtpStatus.EXPIRED_OTP
        OtpInputField(
            otp = otp,
            onOtpChange = { newOtp ->
                otp = newOtp
            },
            isError = isInputError,
            enabled = !state.verifying && !state.resending,
            onComplete = { completedOtp ->
                viewModel.verifyOtp(completedOtp)
            }
        )

        Spacer(Modifier.height(16.dp))

        // Status Feedback Banners
        if (state.error != null) {
            ErrorBanner(state.error!!)
        } else if (state.status == OtpStatus.SUCCESS) {
            SuccessBanner(state.message ?: stringResource(R.string.email_verified_successfully))
        } else if (!state.message.isNullOrBlank()) {
            InfoBanner(state.message!!)
        }

        Spacer(Modifier.height(24.dp))

        // Verify Action Button
        Button(
            onClick = { viewModel.verifyOtp(otp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = otp.length == 6 && !state.verifying && !state.resending && !state.loading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Ink,
                disabledContainerColor = Border,
                disabledContentColor = SecondaryText
            )
        ) {
            if (state.verifying) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.loc_verify), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Resend Section
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (state.resendSecondsRemaining > 0) {
                Text(
                    text = stringResource(R.string.resend_code_in, formatCountdown(state.resendSecondsRemaining)),
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.didnt_receive_code),
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.resending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.resending),
                            color = Accent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.resend_code),
                            color = Accent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(
                                enabled = !state.verifying && !state.loading,
                                onClick = viewModel::resendOtp
                            )
                        )
                    }
                }
            }
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

    // Security check: if resetToken is null or blank, user cannot be on this screen
    LaunchedEffect(state.resetToken) {
        if (state.resetToken.isNullOrBlank()) {
            onBack()
        }
    }

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

    PasswordResetContainer(
        onBack = {
            viewModel.clearResetAuthorization()
            onBack()
        },
        backEnabled = !state.loading
    ) {
        Text(
            text = stringResource(R.string.create_new_password),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.strong_password_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText
        )
        Spacer(Modifier.height(28.dp))

        PasswordField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = stringResource(R.string.loc_new_password),
            visible = showNewPassword,
            onVisibilityChange = { showNewPassword = !showNewPassword }
        )
        Spacer(Modifier.height(14.dp))
        PasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = stringResource(R.string.loc_confirm_new_password),
            visible = showConfirmPassword,
            onVisibilityChange = { showConfirmPassword = !showConfirmPassword }
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.password_requirements),
            color = SecondaryText,
            style = MaterialTheme.typography.bodySmall
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(it)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.resetPassword(newPassword, confirmPassword) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !state.loading && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Ink,
                disabledContainerColor = Border,
                disabledContentColor = SecondaryText
            )
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.loc_reset_password), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * 6-box OTP input field component with:
 * - Auto-focus on first display
 * - Auto advance / retreat on digit typing / backspace
 * - Full 6-digit paste support
 * - Numeric-only input
 * - Blinking cursor animation on active cell
 */
@Composable
fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    onComplete: ((String) -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Throwable) {
        }
    }

    BasicTextField(
        value = TextFieldValue(
            text = otp,
            selection = TextRange(otp.length)
        ),
        onValueChange = { textFieldValue ->
            val digitsOnly = textFieldValue.text.filter { it.isDigit() }.take(6)
            if (digitsOnly != otp) {
                onOtpChange(digitsOnly)
                if (digitsOnly.length == 6) {
                    onComplete?.invoke(digitsOnly)
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (otp.length == 6) {
                    onComplete?.invoke(otp)
                }
                focusManager.clearFocus()
            }
        ),
        enabled = enabled,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused },
        decorationBox = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val digit = otp.getOrNull(i)?.toString() ?: ""
                    val isCurrentBox = isFocused && (i == otp.length || (i == 5 && otp.length == 6))
                    OtpDigitBox(
                        digit = digit,
                        isCurrent = isCurrentBox,
                        isError = isError,
                        enabled = enabled
                    )
                }
            }
        }
    )
}

@Composable
private fun OtpDigitBox(
    digit: String,
    isCurrent: Boolean,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isCurrent -> Accent
        digit.isNotEmpty() -> Accent.copy(alpha = 0.5f)
        else -> Border
    }
    val borderWidth = if (isCurrent || isError) 2.dp else 1.dp

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Elevated)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (digit.isNotEmpty()) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                textAlign = TextAlign.Center
            )
        } else if (isCurrent && enabled) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(Accent.copy(alpha = cursorAlpha))
            )
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
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Elevated,
            unfocusedContainerColor = Elevated,
            focusedBorderColor = Accent,
            unfocusedBorderColor = Border,
            focusedTextColor = PrimaryText,
            unfocusedTextColor = PrimaryText
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = SecondaryText
                )
            }
        }
    )
}

@Composable
internal fun PasswordResetContainer(
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(36.dp))
        IconButton(
            onClick = onBack,
            enabled = backEnabled,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = if (backEnabled) PrimaryText else SecondaryText
            )
        }
        Spacer(Modifier.height(16.dp))
        content()
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
internal fun ErrorBanner(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.error_content_description),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun SuccessBanner(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.loc_success),
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                color = Accent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun InfoBanner(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = stringResource(R.string.loc_info),
                tint = Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
