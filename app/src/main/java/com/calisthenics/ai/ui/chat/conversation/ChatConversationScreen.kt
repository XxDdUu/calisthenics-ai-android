package com.calisthenics.ai.ui.chat.conversation

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.MessageDeliveryStatus
import com.calisthenics.ai.ui.chat.ChatTimeUtils
import com.calisthenics.ai.ui.chat.PresenceIndicator
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    onBackClick: () -> Unit,
    viewModel: ChatConversationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Detect when user scrolls near the top to load older messages
    val shouldLoadOlder by remember {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            firstVisibleIndex <= 2 && state.hasMoreOlderMessages && !state.isLoadingOlder && !state.isLoading
        }
    }

    LaunchedEffect(shouldLoadOlder) {
        if (shouldLoadOlder && state.messages.isNotEmpty()) {
            viewModel.loadOlderMessages()
        }
    }

    // Auto-scroll to bottom on initial load or new messages
    LaunchedEffect(state.shouldScrollToBottom, state.messages.size) {
        if (state.shouldScrollToBottom && state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
            viewModel.resetScrollToBottom()
        }
    }

    // Show error snackbar if error occurs during send
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Elevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!state.otherUserAvatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = state.otherUserAvatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initial = state.otherUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = state.otherUserName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                            PresenceIndicator(
                                presence = state.presence,
                                connectionState = state.webSocketState,
                                isLoading = state.isPresenceLoading,
                                error = state.presenceError
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                .imePadding()
        ) {
            // Message List Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        LoadingView(message = "Loading messages...")
                    }
                    state.messages.isEmpty() -> {
                        EmptyState(
                            title = "No messages yet",
                            body = "Send a greeting to begin your conversation with ${state.otherUserName}."
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Loading spinner at the top when fetching older messages
                            if (state.isLoadingOlder) {
                                item(key = "loading_older_indicator") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Accent,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = state.messages,
                                key = { _, message -> message.clientMessageId ?: message.id.toString() }
                            ) { index, message ->
                                val previousMessage = state.messages.getOrNull(index - 1)
                                val showDateHeader = shouldShowDateHeader(previousMessage, message)

                                if (showDateHeader) {
                                    DateHeader(dateText = ChatTimeUtils.getMessageDateHeader(message.createdAt))
                                }

                                MessageBubbleItem(
                                    message = message,
                                    onRetryMessage = viewModel::retryMessage
                                )
                            }
                        }
                    }
                }
            }

            // Message Composer at bottom
            MessageComposer(
                text = state.inputText,
                onTextChanged = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                isSending = state.isSending
            )
        }
    }
}

private fun shouldShowDateHeader(previousMessage: ChatMessage?, currentMessage: ChatMessage): Boolean {
    if (previousMessage == null) return true
    val prevHeader = ChatTimeUtils.getMessageDateHeader(previousMessage.createdAt)
    val currHeader = ChatTimeUtils.getMessageDateHeader(currentMessage.createdAt)
    return prevHeader != currHeader
}

@Composable
fun DateHeader(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Elevated
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MessageBubbleItem(
    message: ChatMessage,
    onRetryMessage: (String) -> Unit
) {
    val isFromMe = message.isFromCurrentUser
    val isFailed = isFromMe && message.deliveryStatus == MessageDeliveryStatus.FAILED

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = if (isFromMe) {
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp
                )
            } else {
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 16.dp
                )
            },
            color = if (isFromMe) {
                if (isFailed) Elevated else Accent
            } else Elevated,
            modifier = Modifier.widthIn(min = 60.dp, max = 290.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFromMe && !isFailed) Ink else PrimaryText,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ChatTimeUtils.formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isFromMe && !isFailed) Ink.copy(alpha = 0.7f) else SecondaryText
                    )
                    if (isFromMe) {
                        Spacer(Modifier.width(4.dp))
                        when (message.deliveryStatus) {
                            MessageDeliveryStatus.SENDING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    color = Ink.copy(alpha = 0.7f),
                                    strokeWidth = 1.5.dp
                                )
                            }
                            MessageDeliveryStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = Ink.copy(alpha = 0.7f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            MessageDeliveryStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Failed to send",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isFailed && message.clientMessageId != null) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, end = 4.dp)
                    .clickable { onRetryMessage(message.clientMessageId) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry sending",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "Failed to send. Tap to retry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun MessageComposer(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    Surface(
        color = com.calisthenics.ai.ui.theme.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = "Type a message...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 100.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Elevated,
                    unfocusedContainerColor = Elevated,
                    disabledContainerColor = Elevated,
                    focusedBorderColor = Border,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                )
            )

            Spacer(Modifier.width(8.dp))

            val canSend = text.isNotBlank() && !isSending

            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Accent else Elevated)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (canSend) Ink else SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
