package com.calisthenics.ai.ui.chat.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AiActionCard
import com.calisthenics.ai.domain.model.AiConversationSummary
import com.calisthenics.ai.domain.model.AiMessage
import com.calisthenics.ai.ui.chat.ChatTimeUtils
import com.calisthenics.ai.ui.chat.components.ChatInputBar
import com.calisthenics.ai.ui.chat.components.ChatLayout
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.aichat.AiAssistantMessageBubble
import com.calisthenics.ai.ui.components.aichat.AiChatComposer
import com.calisthenics.ai.ui.components.aichat.AiStatusRow
import com.calisthenics.ai.ui.components.aichat.AiUserMessageBubble
import com.calisthenics.ai.ui.components.markdown.AiMarkdownText
import com.calisthenics.ai.ui.theme.*
import kotlinx.coroutines.launch

private val QUICK_SUGGESTIONS = listOf(
    "📋 Plan a 4-day planche split",
    "🏆 Recorded 15 pull-ups today",
    "📈 Analyze my training progress",
    "💪 How to improve handstand balance?"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    onBackClick: () -> Unit,
    onViewPrsClick: () -> Unit = {},
    onViewPlansClick: () -> Unit = {},
    onViewProfileClick: () -> Unit = {},
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    val isImeVisible = WindowInsets.isImeVisible

    // Scroll to bottom when new messages arrive or keyboard opens
    LaunchedEffect(state.messages.size, isImeVisible) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    ChatLayout(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "AI Assistant",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                PillBadge("AI Coach", isAccent = true)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Accent)
                                )
                                Text(
                                    text = "Online • Ready to assist",
                                    fontSize = 11.sp,
                                    color = SecondaryText
                                )
                            }
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
                actions = {
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = PrimaryText
                        )
                    }
                    IconButton(onClick = { viewModel.openHistorySheet() }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Conversation History",
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
        containerColor = Ink,
        messageContent = {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Error banner if any
                state.error?.let { err ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (state.isLoading && state.messages.isEmpty()) {
                    LoadingView(message = "Loading AI Conversation...")
                } else if (state.messages.isEmpty()) {
                    // Empty conversation view with welcome banner and suggestions
                    AiChatEmptyWelcome(
                        onSelectSuggestion = { suggestion ->
                            viewModel.sendMessage(suggestion)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = state.messages,
                            key = { it.id }
                        ) { message ->
                            AiChatMessageItem(
                                message = message,
                                onViewPrsClick = onViewPrsClick,
                                onViewPlansClick = onViewPlansClick,
                                onViewProfileClick = onViewProfileClick
                            )
                        }

                        if (state.isSending) {
                            item(key = "typing_indicator") {
                                AiStatusRow(statusText = "AI Coach is thinking...")
                            }
                        }
                    }
                }
            }
        },
        inputBar = {
            AiChatComposer(
                prompt = inputText,
                onPromptChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank() && !state.isSending) {
                        val text = inputText
                        inputText = ""
                        viewModel.sendMessage(text)
                    }
                },
                isGenerating = state.isSending,
                placeholder = "Message AI Coach...",
                errorMessage = state.error
            )
        }
    )

    // Previous Conversations Bottom Sheet
    if (state.isHistorySheetOpen) {
        AiConversationHistorySheet(
            conversations = state.allConversations,
            activeConversationId = state.currentConversationId,
            isDeleting = state.isDeleting,
            onSelectConversation = { id ->
                viewModel.selectConversation(id)
            },
            onDeleteConversation = { id ->
                viewModel.deleteConversation(id)
            },
            onNewConversation = {
                viewModel.startNewConversation()
            },
            onDismiss = {
                viewModel.closeHistorySheet()
            }
        )
    }
}

@Composable
private fun AiChatEmptyWelcome(
    onSelectSuggestion: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Calisthenics AI Assistant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Your personal coach for workout planning, automatic PR recording, and training progress analysis.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Quick Prompts",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SecondaryText
        )

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QUICK_SUGGESTIONS.forEach { prompt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectSuggestion(prompt) },
                    shape = RoundedCornerShape(14.dp),
                    color = Elevated,
                    border = BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryText
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatMessageItem(
    message: AiMessage,
    onViewPrsClick: () -> Unit,
    onViewPlansClick: () -> Unit,
    onViewProfileClick: () -> Unit
) {
    if (message.isFromUser) {
        AiUserMessageBubble(text = message.content)
    } else {
        AiAssistantMessageBubble(
            markdown = message.content,
            extraContent = {
                message.actionCard?.let { card ->
                    when (card) {
                        is AiActionCard.PrRecorded -> {
                            AiPrRecordedCard(card = card, onViewPrsClick = onViewPrsClick)
                        }
                        is AiActionCard.WorkoutPlan -> {
                            AiWorkoutPlanCard(card = card, onViewPlansClick = onViewPlansClick)
                        }
                        is AiActionCard.ProgressSummary -> {
                            AiProgressSummaryCard(card = card)
                        }
                        is AiActionCard.ProfileUpdate -> {
                            AiProfileUpdateCard(card = card, onViewProfileClick = onViewProfileClick)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun AiPrRecordedCard(
    card: AiActionCard.PrRecorded,
    onViewPrsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
        border = BorderStroke(1.5.dp, Accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Personal Record Saved!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
                PillBadge(card.recordType.replace("MAX_", "").replace('_', ' '), isAccent = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = card.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    card.formattedValue?.let { fv ->
                        Text(text = fv, fontSize = 12.sp, color = SecondaryText)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    val displayVal = if (card.value % 1.0 == 0.0) "${card.value.toLong()}" else "${card.value}"
                    Text(
                        text = "$displayVal ${card.unit}".trim(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Accent
                    )
                    card.improvementPercentage?.let { pct ->
                        if (pct > 0) {
                            Text(
                                text = "+$pct% improvement",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Accent
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onViewPrsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Ink
                )
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("View in Personal Records", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AiWorkoutPlanCard(
    card: AiActionCard.WorkoutPlan,
    onViewPlansClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
        border = BorderStroke(1.5.dp, Accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Workout Routine",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
                PillBadge("${card.durationWeeks} Weeks", isAccent = true)
            }

            Text(
                text = card.planName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            card.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onViewPlansClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Ink
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("View Workout Plans", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AiProgressSummaryCard(
    card: AiActionCard.ProgressSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Training Progress Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${card.totalPrs}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Accent
                    )
                    Text("Total PRs", fontSize = 11.sp, color = SecondaryText)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${card.uniqueExercisesCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryText
                    )
                    Text("Exercises", fontSize = 11.sp, color = SecondaryText)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${card.completedSessionsCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryText
                    )
                    Text("Workouts", fontSize = 11.sp, color = SecondaryText)
                }
            }
        }
    }
}

@Composable
private fun AiProfileUpdateCard(
    card: AiActionCard.ProfileUpdate,
    onViewProfileClick: () -> Unit
) {
    if (card.isAmbiguous) return

    val borderColor = if (card.success) Success else Warning
    val statusText = if (card.success) "Updated" else "Notice"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Ink),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (card.success) Success.copy(alpha = 0.2f) else Warning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (card.success) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (card.success) Success else Warning,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (card.success) "Profile Synchronized" else "Profile Notice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }
                PillBadge(text = statusText, isAccent = card.success)
            }

            if (card.updatedFields.isNotEmpty()) {
                Surface(
                    color = Color(0xFF1E2433),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        card.updatedFields.forEach { (key, value) ->
                            val label = when (key) {
                                "weightKg" -> "Weight"
                                "heightCm" -> "Height"
                                "primaryGoals" -> "Primary Goal"
                                "experienceLevel" -> "Level"
                                "displayName" -> "Display Name"
                                "fullName" -> "Full Name"
                                "bio" -> "Bio"
                                "trainingInterests" -> "Interests"
                                "preferredFrequency" -> "Frequency"
                                else -> key.replaceFirstChar { it.uppercase() }
                            }
                            val displayVal = when (key) {
                                "weightKg" -> "$value kg"
                                "heightCm" -> "$value cm"
                                else -> value
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 12.sp, color = SecondaryText)
                                Text(displayVal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryText)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onViewProfileClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Ink
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("View Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiConversationHistorySheet(
    conversations: List<AiConversationSummary>,
    activeConversationId: Long?,
    isDeleting: Boolean,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var conversationToDelete by remember { mutableStateOf<AiConversationSummary?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Elevated,
        contentColor = PrimaryText,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Conversations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Button(
                    onClick = onNewConversation,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("New Chat", fontWeight = FontWeight.Bold)
                }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No previous conversations yet.",
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isActive = conv.id == activeConversationId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectConversation(conv.id) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isActive) Accent.copy(alpha = 0.12f) else Ink,
                            border = BorderStroke(1.dp, if (isActive) Accent else Border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = conv.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isActive) Accent else PrimaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isActive) {
                                            PillBadge("Active", isAccent = true)
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = conv.lastMessagePreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { conversationToDelete = conv },
                                    enabled = !isDeleting
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete conversation",
                                        tint = SecondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    conversationToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = {
                Text("Delete Conversation?", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${conv.title}\"? This conversation and its history cannot be recovered.",
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = conv.id
                        conversationToDelete = null
                        onDeleteConversation(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel", color = PrimaryText)
                }
            },
            containerColor = Elevated
        )
    }
}
