package com.calisthenics.ai.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@Composable
fun RegisterOtpVerificationScreen(
    viewModel: AuthViewModel,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var otp by remember { mutableStateOf("") }

    // If there is no pending registration email, return back to registration
    val pendingEmail = state.registrationPendingEmail.orEmpty()
    LaunchedEffect(pendingEmail) {
        if (pendingEmail.isBlank()) {
            onBack()
        }
    }

    BackHandler {
        if (!state.otpVerifying && !state.otpResending && !state.loading) {
            viewModel.clearRegistrationOtpState()
            onBack()
        }
    }

    LaunchedEffect(state.authenticated) {
        if (state.authenticated) {
            onVerified()
        }
    }

    PasswordResetContainer(
        onBack = {
            viewModel.clearRegistrationOtpState()
            onBack()
        },
        backEnabled = !state.otpVerifying && !state.otpResending && !state.loading
    ) {
        Text(
            text = "Verify your email",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Enter the 6-digit verification code sent to your email to activate your account.",
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
                        contentDescription = "Email",
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = maskEmail(pendingEmail),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                }
                TextButton(
                    onClick = {
                        viewModel.clearRegistrationOtpState()
                        onBack()
                    },
                    enabled = !state.otpVerifying && !state.otpResending && !state.loading
                ) {
                    Text(
                        text = "Change",
                        color = Accent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // 6-digit OTP Input
        val isInputError = state.otpError != null
        OtpInputField(
            otp = otp,
            onOtpChange = { newOtp ->
                otp = newOtp
            },
            isError = isInputError,
            enabled = !state.otpVerifying && !state.otpResending,
            onComplete = { completedOtp ->
                viewModel.verifyRegisterOtp(completedOtp)
            }
        )

        Spacer(Modifier.height(16.dp))

        // Feedback banners
        state.otpError?.let {
            ErrorBanner(it)
        } ?: state.otpMessage?.let {
            InfoBanner(it)
        }

        Spacer(Modifier.height(24.dp))

        // Verify button
        Button(
            onClick = { viewModel.verifyRegisterOtp(otp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = otp.length == 6 && !state.otpVerifying && !state.otpResending && !state.loading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Ink,
                disabledContainerColor = Border,
                disabledContentColor = SecondaryText
            )
        ) {
            if (state.otpVerifying) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
            } else {
                Text("Verify & Create Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Resend Section
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (state.otpResendCooldownSeconds > 0) {
                Text(
                    text = "Resend code in ${formatCountdown(state.otpResendCooldownSeconds)}",
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Didn't receive code? ",
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.otpResending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Resending...",
                            color = Accent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Resend Code",
                            color = Accent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(
                                enabled = !state.otpVerifying && !state.loading,
                                onClick = {
                                    otp = ""
                                    viewModel.resendRegisterOtp()
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
