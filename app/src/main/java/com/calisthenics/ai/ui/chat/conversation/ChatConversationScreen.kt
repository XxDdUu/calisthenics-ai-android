package com.calisthenics.ai.ui.chat.conversation

import com.calisthenics.ai.R
import com.calisthenics.ai.ui.components.aichat.MessageCopyButton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.calisthenics.ai.ui.report.ReportUserSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto
import com.calisthenics.ai.domain.model.ChatMessage
import com.calisthenics.ai.domain.model.MessageDeliveryStatus
import com.calisthenics.ai.ui.chat.ChatTimeUtils
import com.calisthenics.ai.ui.chat.PresenceIndicator
import com.calisthenics.ai.ui.chat.components.ChatInputBar
import com.calisthenics.ai.ui.chat.components.ChatLayout
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    onBackClick: () -> Unit,
    onSharedPrClick: (SharedPersonalRecordDto) -> Unit = {},
    viewModel: ChatConversationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0

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

    // Keep latest messages visible when the software keyboard opens
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Show error snackbar if error occurs during send
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.clearError()
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    if (showReportSheet && state.otherUserId > 0L) {
        ReportUserSheet(
            userId = state.otherUserId,
            userName = state.otherUserName.ifBlank { "User" },
            onDismiss = { showReportSheet = false }
        )
    }

    ChatLayout(
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
                                    contentDescription = stringResource(R.string.loc_avatar),
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
                            contentDescription = stringResource(R.string.common_back),
                            tint = PrimaryText
                        )
                    }
                },
                actions = {
                    if (state.otherUserId > 0L) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = SecondaryText
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(Elevated)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.report_user),
                                            color = PrimaryText
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showReportSheet = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    titleContentColor = PrimaryText
                )
            )
        },
        snackbarHostState = snackbarHostState,
        containerColor = Ink,
        messageContent = {
            // Message List Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when {
                    state.isLoading -> {
                        LoadingView(message = stringResource(R.string.loading_messages))
                    }
                    state.messages.isEmpty() -> {
                        EmptyState(
                            title = stringResource(R.string.no_messages),
                            body = stringResource(R.string.greeting_message, state.otherUserName)
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
                                    onRetryMessage = viewModel::retryMessage,
                                    onSharedPrClick = onSharedPrClick
                                )
                            }
                        }
                    }
                }
            }
        },
        inputBar = {
            // Message Composer at bottom
            MessageComposer(
                text = state.inputText,
                onTextChanged = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                isSending = state.isSending
            )
        }
    )
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
    onRetryMessage: (String) -> Unit,
    onSharedPrClick: (SharedPersonalRecordDto) -> Unit = {}
) {
    val isFromMe = message.isFromCurrentUser
    val isFailed = isFromMe && message.deliveryStatus == MessageDeliveryStatus.FAILED
    val sharedPr = message.sharedPr

    if (message.messageType.equals("PR_SHARE", ignoreCase = true) || sharedPr != null) {
        PrShareMessageCard(
            message = message,
            sharedPr = sharedPr,
            isFromMe = isFromMe,
            isFailed = isFailed,
            onRetryMessage = onRetryMessage,
            onCardClick = { sharedPr?.let(onSharedPrClick) }
        )
        return
    }

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
                    MessageCopyButton(
                        textToCopy = message.content,
                        cleanMarkdown = false,
                        tint = if (isFromMe && !isFailed) Ink.copy(alpha = 0.65f) else SecondaryText,
                        copiedTint = if (isFromMe && !isFailed) Ink else Accent
                    )
                    Spacer(Modifier.width(4.dp))
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
                                    contentDescription = stringResource(R.string.loc_sent),
                                    tint = Ink.copy(alpha = 0.7f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            MessageDeliveryStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.loc_failed_to_send),
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
                    contentDescription = stringResource(R.string.loc_retry_sending),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.failed_send_retry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun PrShareMessageCard(
    message: ChatMessage,
    sharedPr: SharedPersonalRecordDto?,
    isFromMe: Boolean,
    isFailed: Boolean,
    onRetryMessage: (String) -> Unit,
    onCardClick: () -> Unit
) {
    val pr = sharedPr?.personalRecord
    val exerciseName = pr?.exerciseName?.takeIf { it.isNotBlank() } ?: "Personal Record"
    val achievementValue = pr?.displayFormattedValue?.takeIf { it.isNotBlank() } ?: message.content
    val prTypeLabel = pr?.recordType
        ?.replace("MAX_", "")
        ?.replace('_', ' ')
        ?.takeIf { it.isNotBlank() }
        ?: "PR"
    val achievedDate = pr?.achievedAt?.take(10)?.takeIf { it.isNotBlank() }
    val previousFormatted = pr?.formattedPrevious?.takeIf { it.isNotBlank() }
        ?: pr?.previousRecordValue?.let { prev ->
            val num = if (prev % 1.0 == 0.0) "${prev.toLong()}" else "$prev"
            val unit = pr.unit.orEmpty()
            if (unit.isNotBlank()) "$num $unit" else num
        }
    val improvementPct = pr?.improvementPercentage?.takeIf { it > 0.0 }
    val notes = pr?.notes?.takeIf { it.isNotBlank() }
    val coachFeedback = sharedPr?.coachFeedback?.takeIf { it.isNotBlank() }
    val rating = sharedPr?.rating?.takeIf { it > 0 }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 240.dp, max = 320.dp)
                .clickable(enabled = sharedPr != null, onClick = onCardClick),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isFromMe) 18.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(containerColor = Elevated),
            border = BorderStroke(
                width = 1.dp,
                color = if (isFailed) MaterialTheme.colorScheme.error else Accent.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Warning.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Warning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "PERSONAL RECORD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Warning,
                            letterSpacing = 0.6.sp
                        )
                    }
                    PillBadge(text = prTypeLabel, isAccent = true)
                }

                // Exercise Name
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Achievement Value + Improvement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievementValue,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Accent
                    )
                    if (improvementPct != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Accent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "+$improvementPct%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Metadata: Previous value & Achieved date
                if (!previousFormatted.isNullOrBlank() || !achievedDate.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!previousFormatted.isNullOrBlank()) {
                            Text(
                                text = "Prev: $previousFormatted",
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                        if (!achievedDate.isNullOrBlank()) {
                            Text(
                                text = achievedDate,
                                fontSize = 11.sp,
                                color = SecondaryText
                            )
                        }
                    }
                }

                // Optional PR Notes
                if (!notes.isNullOrBlank()) {
                    Text(
                        text = notes,
                        fontSize = 12.sp,
                        color = PrimaryText.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                // Coach feedback preview if reviewed
                if (!coachFeedback.isNullOrBlank() || rating != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Coach Feedback",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent
                            )
                            if (rating != null) {
                                Text(
                                    text = "★".repeat(rating),
                                    fontSize = 11.sp,
                                    color = Warning
                                )
                            }
                        }
                        if (!coachFeedback.isNullOrBlank()) {
                            Text(
                                text = "\"$coachFeedback\"",
                                fontSize = 11.sp,
                                color = PrimaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = Border.copy(alpha = 0.5f))

                // Footer: View PR Details + Message Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Tap to view PR details",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Accent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ChatTimeUtils.formatMessageTime(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = SecondaryText
                        )
                        if (isFromMe) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.loc_sent),
                                tint = Accent,
                                modifier = Modifier.size(11.dp)
                            )
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
                    contentDescription = stringResource(R.string.loc_retry_sending),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.failed_send_retry),
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
    ChatInputBar(
        inputText = text,
        onInputChange = onTextChanged,
        onSend = onSend,
        isSending = isSending,
        placeholder = stringResource(R.string.type_message),
        sendIcon = Icons.AutoMirrored.Filled.Send,
        sendContentDescription = stringResource(R.string.loc_send_message),
        containerColor = com.calisthenics.ai.ui.theme.Surface,
        sendButtonBackground = Accent,
        sendButtonContentColor = Ink
    )
}
