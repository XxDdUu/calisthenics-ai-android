package com.calisthenics.ai.ui.chat.list

import com.calisthenics.ai.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.calisthenics.ai.data.remote.websocket.WebSocketState
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.ChatContact
import com.calisthenics.ai.domain.model.ChatConversation
import com.calisthenics.ai.domain.model.UserPresence
import com.calisthenics.ai.ui.chat.ChatTimeUtils
import com.calisthenics.ai.ui.chat.PresenceIndicator
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onConversationClick: (conversationId: Long, otherUserId: Long, otherUserName: String, otherUserAvatarUrl: String?) -> Unit,
    onAiAssistantClick: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.messages),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        if (state.totalUnreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Accent)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state.totalUnreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                            }
                        }
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.showNewChatDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.loc_start_new_conversation),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.loadConversations(isRefresh = true) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.loc_refresh_conversations),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    titleContentColor = PrimaryText
                )
            )
        },
        containerColor = Ink
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.conversations.isEmpty() && state.aiSummary == null -> {
                    LoadingView(message = stringResource(R.string.loading_conversations))
                }
                state.error != null && state.conversations.isEmpty() && state.aiSummary == null -> {
                    ErrorView(
                        message = state.error ?: stringResource(R.string.unable_load_conversations),
                        onRetry = { viewModel.loadConversations() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. AI Assistant Entry prominently placed at the top
                        item(key = "ai_assistant_entry") {
                            AiAssistantEntryCard(
                                summary = state.aiSummary,
                                onClick = onAiAssistantClick
                            )
                        }

                        // 2. Section Header for User Conversations
                        item(key = "direct_messages_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Direct Messages",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                if (state.conversations.isNotEmpty()) {
                                    Text(
                                        text = "${state.conversations.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SecondaryText
                                    )
                                }
                            }
                        }

                        // 3. User Conversations List or Empty State
                        if (state.conversations.isEmpty()) {
                            item(key = "empty_user_conversations") {
                                DarkCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = SecondaryText,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.no_conversations),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryText
                                        )
                                        Text(
                                            text = stringResource(R.string.connect_chat_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryText,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        TextButton(onClick = { viewModel.showNewChatDialog() }) {
                                            Text(stringResource(R.string.start_chat), color = Accent)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(
                                items = state.conversations,
                                key = { it.id }
                            ) { conversation ->
                                ConversationRowItem(
                                    conversation = conversation,
                                    presence = state.presences[conversation.otherUserId],
                                    connectionState = state.webSocketState,
                                    isPresenceLoading = state.isPresenceLoading,
                                    presenceError = state.presenceError,
                                    onClick = {
                                        onConversationClick(
                                            conversation.id,
                                            conversation.otherUserId,
                                            conversation.otherUserName,
                                            conversation.otherUserAvatarUrl
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (state.isNewChatDialogVisible) {
                NewChatDialog(
                    contacts = state.availableContacts,
                    isCreating = state.isCreatingConversation,
                    onSelectContact = { contact ->
                        viewModel.startConversation(contact.userId) { conversationId ->
                            onConversationClick(
                                conversationId,
                                contact.userId,
                                contact.displayName,
                                null
                            )
                        }
                    },
                    onDismiss = { viewModel.dismissNewChatDialog() }
                )
            }
        }
    }
}

@Composable
fun ConversationRowItem(
    conversation: ChatConversation,
    presence: UserPresence? = null,
    connectionState: WebSocketState = WebSocketState.DISCONNECTED,
    isPresenceLoading: Boolean = false,
    presenceError: String? = null,
    onClick: () -> Unit
) {
    DarkCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Elevated),
                contentAlignment = Alignment.Center
            ) {
                if (!conversation.otherUserAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = conversation.otherUserAvatarUrl,
                        contentDescription = stringResource(R.string.avatar_of, conversation.otherUserName),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = conversation.otherUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = conversation.otherUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        PresenceIndicator(
                            presence = presence,
                            connectionState = connectionState,
                            isLoading = isPresenceLoading,
                            error = presenceError
                        )
                    }
                    conversation.lastMessageAt?.let { timeStr ->
                        Text(
                            text = ChatTimeUtils.formatConversationTime(timeStr),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessagePreview ?: stringResource(R.string.loc_no_messages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.unreadCount > 0) PrimaryText else SecondaryText,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Accent)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatDialog(
    contacts: List<ChatContact>,
    isCreating: Boolean,
    onSelectContact: (ChatContact) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.start_conversation),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isCreating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Accent)
                    }
                } else if (contacts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_active_connections_chat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                } else {
                    Text(
                        text = stringResource(R.string.select_contact),
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(contacts, key = { it.userId }) { contact ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectContact(contact) },
                                color = Elevated
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = contact.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = PrimaryText
                                        )
                                        Text(
                                            text = contact.roleLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text(stringResource(R.string.cancel), color = Accent)
            }
        },
        containerColor = com.calisthenics.ai.ui.theme.Surface
    )
}

@Composable
fun AiAssistantEntryCard(
    summary: AiConversationSummary?,
    onClick: () -> Unit
) {
    DarkCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Avatar with active dot
            Box(
                modifier = Modifier.size(50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                        tint = Ink,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // Online pulse indicator
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Ink)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Accent)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        PillBadge(text = "AI Coach", isAccent = true)
                    }

                    val timeLabel = summary?.lastInteractionAt?.let { ChatTimeUtils.formatMessageTime(it) } ?: "Active"
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }

                val preview = summary?.lastMessagePreview ?: "Ask about workouts, record PRs, or analyze progress"
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
