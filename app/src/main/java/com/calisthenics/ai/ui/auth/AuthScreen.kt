package com.calisthenics.ai.ui.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.ui.components.UserAvatar
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToVerifyRegisterOtp: (() -> Unit)? = null,
    resetSuccessMessage: String? = null,
    isAddingAccount: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    googleAuthManager: GoogleAuthManager? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authManager = googleAuthManager ?: remember(context) {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                GoogleAuthEntryPoint::class.java
            ).googleAuthManager()
        } catch (_: Throwable) {
            GoogleAuthManager(context.applicationContext)
        }
    }

    var isRegistering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var accountPendingRemoval by remember { mutableStateOf<SavedAccount?>(null) }
    var hasNavigated by remember { mutableStateOf(false) }
    var showAddGoogleAccountDialog by remember { mutableStateOf(false) }
    var isGoogleClickProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(isAddingAccount) {
        if (isAddingAccount) {
            viewModel.prepareForAddAccount()
        } else {
            viewModel.refreshSavedAccounts()
        }
    }

    LaunchedEffect(state.authenticated) {
        if (state.authenticated && !hasNavigated) {
            hasNavigated = true
            onAuthenticated()
        }
    }

    LaunchedEffect(state.registrationOtpSent) {
        if (state.registrationOtpSent && !hasNavigated) {
            onNavigateToVerifyRegisterOtp?.invoke()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (onBackClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    stringResource(R.string.loc_add_account_vietnamese),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = stringResource(R.string.loc_calisthenics_ai),
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.loc_calisthenics_ai), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(if (isRegistering) R.string.auth_register_tagline else R.string.auth_login_tagline),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        // Saved accounts section
        if (state.savedAccounts.isNotEmpty() && !isRegistering && !isAddingAccount) {
            Text(
                stringResource(R.string.loc_saved_accounts_vietnamese),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            state.savedAccounts.forEach { account ->
                SavedAccountAuthCard(
                    account = account,
                    isSwitching = state.switchingUserId == account.userId,
                    onSelect = { viewModel.switchToAccount(account.userId) },
                    onRemove = { accountPendingRemoval = account }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = stringResource(R.string.auth_or_sign_in_new),
                    modifier = Modifier.padding(horizontal = 14.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        if (isRegistering) {
            OutlinedTextField(
                name,
                { name = it },
                label = { Text(stringResource(R.string.full_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            email,
            { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password,
            { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (!isRegistering && !resetSuccessMessage.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(resetSuccessMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (isRegistering) viewModel.register(name, email, password)
                else viewModel.login(email, password)
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !state.loading
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(if (isRegistering) R.string.auth_create_account else R.string.auth_sign_in))
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = stringResource(R.string.auth_or),
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                if (state.loading || isGoogleClickProcessing) return@OutlinedButton
                isGoogleClickProcessing = true
                val requestId = java.util.UUID.randomUUID().toString().take(8)
                android.util.Log.d("GoogleSignIn", "[$requestId] GOOGLE_SIGN_IN_CLICKED")
                coroutineScope.launch {
                    try {
                        viewModel.startGoogleSignIn(requestId)
                        when (val result = authManager.signIn(context, requestId)) {
                            is GoogleSignInResult.Success -> {
                                viewModel.loginWithFirebase(result.idToken, requestId)
                            }
                            is GoogleSignInResult.Cancelled -> {
                                viewModel.cancelGoogleSignIn(requestId)
                            }
                            is GoogleSignInResult.Error -> {
                                viewModel.cancelGoogleSignIn(requestId)
                                if (result.canAddAccount) {
                                    showAddGoogleAccountDialog = true
                                }
                                viewModel.setErrorMessage(result.message)
                            }
                        }
                    } finally {
                        isGoogleClickProcessing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !state.loading && !isGoogleClickProcessing
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.sign_in_google_loading))
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = stringResource(R.string.loc_google),
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.continue_with_google))
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { isRegistering = !isRegistering }) {
            Text(stringResource(if (isRegistering) R.string.auth_already_have_account else R.string.auth_create_an_account))
        }
        if (!isRegistering) {
            TextButton(onClick = onForgotPassword) {
                Text(stringResource(R.string.forgot_password))
            }
        }
    }

    accountPendingRemoval?.let { account ->
        AlertDialog(
            onDismissRequest = { accountPendingRemoval = null },
            title = { Text(stringResource(R.string.loc_delete_saved_account_vietnamese)) },
            text = { Text(stringResource(R.string.loc_delete_saved_account_message_vietnamese, account.email)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeSavedAccount(account.userId)
                        accountPendingRemoval = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountPendingRemoval = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddGoogleAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoogleAccountDialog = false },
            title = { Text(stringResource(R.string.loc_add_google_account_vietnamese)) },
            text = { Text(stringResource(R.string.loc_saved_google_accounts_vietnamese)) },
            confirmButton = {
                Button(
                    onClick = {
                        showAddGoogleAccountDialog = false
                        authManager.openAccountSettings(context)
                    }
                ) {
                    Text(stringResource(R.string.loc_open_settings_vietnamese))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoogleAccountDialog = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }
}

@Composable
private fun SavedAccountAuthCard(
    account: SavedAccount,
    isSwitching: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSwitching, onClick = onSelect)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(account.avatarUrl, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.fullName.ifBlank { account.email },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = account.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isSwitching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
