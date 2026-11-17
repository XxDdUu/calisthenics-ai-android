package com.calisthenics.ai.ui.profile

import com.calisthenics.ai.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.SavedAccount
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.UserAvatar
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchAccountScreen(
    onAccountSwitched: () -> Unit,
    onAddAccount: () -> Unit,
    onAllAccountsRemoved: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: SwitchAccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.switchSuccess) {
        if (state.switchSuccess) {
            onAccountSwitched()
        }
    }

    LaunchedEffect(state.accounts.size) {
        if (state.accounts.isEmpty()) {
            onAllAccountsRemoved()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.loc_switch_account_vietnamese),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    titleContentColor = PrimaryText
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Ink
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.loc_saved_accounts),
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.loc_switch_account_description_vietnamese),
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
            Spacer(Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.accounts, key = { it.userId }) { account ->
                    AccountCard(
                        account = account,
                        isSwitching = state.switchingUserId == account.userId,
                        onSelect = { viewModel.onAccountSelected(account) },
                        onRemove = { viewModel.requestRemoveAccount(account) }
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onAddAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Accent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Accent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.loc_add_account_vietnamese),
                            fontWeight = FontWeight.SemiBold,
                            color = Accent
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog when removing an account
    state.accountPendingRemoval?.let { account ->
        Dialog(
            onDismissRequest = { viewModel.cancelRemoveAccount() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DarkCard(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        stringResource(R.string.loc_delete_account_question),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        stringResource(R.string.loc_delete_account_message_vietnamese, account.email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                    if (account.isActive) {
                        Text(
                            stringResource(R.string.loc_active_account_warning_vietnamese),
                            style = MaterialTheme.typography.bodySmall,
                            color = Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cancelRemoveAccount() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = { viewModel.confirmRemoveAccount() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Error,
                                contentColor = PrimaryText
                            )
                        ) {
                            Text(stringResource(R.string.common_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: SavedAccount,
    isSwitching: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !account.isActive && !isSwitching, onClick = onSelect)
            .then(
                if (account.isActive) {
                    Modifier.border(1.5.dp, Accent, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, Border, RoundedCornerShape(16.dp))
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isActive) Elevated else Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(account.avatarUrl, modifier = Modifier.size(50.dp))

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = account.fullName.ifBlank { account.email },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (account.role == "ROLE_ADMIN") {
                        PillBadge("Admin", isAccent = true)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = account.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            when {
                isSwitching -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Accent
                    )
                }
                account.isActive -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.loc_active),
                                tint = Accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        PillBadge("Đang dùng", isAccent = true)
                    }
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.loc_delete_account_device),
                                tint = SecondaryText.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
