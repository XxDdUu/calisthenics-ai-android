package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.calisthenics.ai.ui.components.aichat.AiAssistantMessageBubble
import com.calisthenics.ai.ui.components.aichat.AiChatComposer
import com.calisthenics.ai.ui.components.aichat.AiStatusRow
import com.calisthenics.ai.ui.components.aichat.AiUserMessageBubble
import com.calisthenics.ai.ui.components.markdown.AiMarkdownText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.AdminAiConversationSummary
import com.calisthenics.ai.domain.model.AdminAiMessage
import com.calisthenics.ai.domain.model.AdminAiRecommendation
import com.calisthenics.ai.domain.model.AdminPendingConfirmation
import com.calisthenics.ai.domain.model.AdminPlatformOverview
import com.calisthenics.ai.domain.model.AdminTrainingAnalytics
import com.calisthenics.ai.domain.model.GettingStartedCampaignAnalytics
import com.calisthenics.ai.domain.model.GettingStartedCampaignSummary
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.GettingStartedProgramPreview
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.CardBackground
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Info
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.LocalAppColors
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.SubtleBorder
import com.calisthenics.ai.ui.theme.Warning
import java.util.Locale

@Composable
fun AdminAiAgentScreen(
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit = {},
    onOpenWorkoutPlan: (Long) -> Unit = {},
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(onOverview = onBack),
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        "AI Chat",
        "AI Insights",
        "Exercise Recs",
        "Draft Programs",
        "Users & Segments"
    )

    LaunchedEffect(Unit) {
        viewModel.loadAdminAiWorkspace()
    }

    AdminConsoleScaffold(
        currentSection = AdminNavSection.AI_AGENT,
        title = "Admin AI Agent",
        subtitle = "PRO entitlements, user analytics & campaigns",
        navigation = adminNavigation,
        onBack = onBack,
        headerActions = {
            IconButton(
                onClick = { viewModel.loadAdminAiWorkspace(state.aiAnalyticsRange) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PrimaryText
                )
            }
        }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 12.dp)
    ) {

        state.error?.let { err ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.loadAdminAiWorkspace(state.aiAnalyticsRange) }) {
                        Text("Retry")
                    }
                }
            }
        }

        state.message?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::clearMessage) { Text("Dismiss") }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AdminAiChatTab(
                    conversations = state.aiConversations,
                    activeConversationId = state.activeAiConversationId,
                    messages = state.aiMessages,
                    suggestedPrompts = state.aiSuggestedFollowUps,
                    isSending = state.isSendingAiMessage,
                    executedTools = state.aiLastExecutedTools,
                    timeRangeUsed = state.aiLastTimeRangeUsed,
                    matchedUsers = state.aiMatchedChatUsers,
                    missingDataNotes = state.aiMissingDataNotes,
                    gettingStartedProgram = state.aiGettingStartedProgram,
                    gettingStartedPreview = state.aiGettingStartedPreview,
                    activeCampaigns = state.aiActiveCampaigns,
                    campaignAnalytics = state.aiCampaignAnalytics,
                    pendingConfirmation = state.aiPendingConfirmation,
                    isResolvingConfirmation = state.isResolvingConfirmation,
                    onSelectConversation = viewModel::selectAdminAiConversation,
                    onNewConversation = { viewModel.createNewAdminAiConversation() },
                    onSendMessage = viewModel::sendAdminAiMessage,
                    onResolveConfirmation = viewModel::resolvePendingConfirmation,
                    onInspectUser = { userId -> viewModel.inspectGlobalUser(userId, includeEmail = false) }
                )
                1 -> AdminAiInsightsTab(
                    selectedRange = state.aiAnalyticsRange,
                    overview = state.aiPlatformOverview,
                    training = state.aiTrainingAnalytics,
                    operationalRecommendations = state.aiRecommendations.filter { it.type == "OPERATIONAL" },
                    isGenerating = state.isGeneratingRecommendation,
                    isReviewing = state.isReviewingRecommendation,
                    onRangeSelected = { viewModel.loadAdminAiWorkspace(it) },
                    onGenerateOperational = {
                        viewModel.generateAdminRecommendations(type = "OPERATIONAL")
                    },
                    onApprove = { id -> viewModel.approveAdminRecommendation(id) },
                    onReject = { id -> viewModel.rejectAdminRecommendation(id) },
                    onImplement = { id -> viewModel.implementAdminRecommendation(id) }
                )
                2 -> AdminExerciseRecommendationsTab(
                    recommendations = state.aiRecommendations.filter { it.type == "EXERCISE" },
                    isGenerating = state.isGeneratingRecommendation,
                    isReviewing = state.isReviewingRecommendation,
                    onGenerateForSkill = { skill ->
                        viewModel.generateAdminRecommendations(
                            type = "EXERCISE",
                            targetSkill = skill
                        )
                    },
                    onApprove = { id, publishNow ->
                        viewModel.approveAdminRecommendation(id, publishExercise = publishNow)
                    },
                    onReject = { id -> viewModel.rejectAdminRecommendation(id) },
                    onConfirmPublication = { id -> viewModel.implementAdminRecommendation(id) },
                    onOpenExercise = onOpenExercise
                )
                3 -> AdminDraftWorkoutProgramsTab(
                    recommendations = state.aiRecommendations.filter { it.type == "WORKOUT_PROGRAM" },
                    isGenerating = state.isGeneratingRecommendation,
                    isReviewing = state.isReviewingRecommendation,
                    onGenerateProgram = { skill, level ->
                        viewModel.generateAdminRecommendations(
                            type = "WORKOUT_PROGRAM",
                            targetSkill = skill,
                            trainingLevel = level
                        )
                    },
                    onApprove = { id -> viewModel.approveAdminRecommendation(id) },
                    onReject = { id -> viewModel.rejectAdminRecommendation(id) },
                    onOpenWorkoutPlan = onOpenWorkoutPlan
                )
                4 -> AdminUsersAndSegmentsTab(
                    selectedRange = state.aiAnalyticsRange,
                    isLoading = state.isLoadingGlobalUsers,
                    isLoadingUserDetail = state.isLoadingUserDetail,
                    userStats = state.aiGlobalUserStatistics,
                    coachStats = state.aiCoachClientStatistics,
                    platformEngagement = state.aiPlatformEngagement,
                    userSegments = state.aiUserSegments,
                    currentFilter = state.aiSegmentFilter,
                    onEnsureLoaded = {
                        if (state.aiGlobalUserStatistics == null && !state.isLoadingGlobalUsers) {
                            viewModel.loadGlobalUserAnalytics(state.aiAnalyticsRange)
                        }
                    },
                    onRangeSelected = { range -> viewModel.loadGlobalUserAnalytics(range) },
                    onApplySegmentFilter = { filter -> viewModel.analyzeUserSegment(filter) },
                    onInspectUser = { userId -> viewModel.inspectGlobalUser(userId, includeEmail = false) }
                )
            }
        }
    }
    }

    state.inspectedUserDetail?.let { dossier ->
        AdminUserDossierDialog(
            dossier = dossier,
            isLoading = state.isLoadingUserDetail,
            onRevealEmail = {
                viewModel.inspectGlobalUser(dossier.userId, includeEmail = true)
            },
            onDismiss = { viewModel.dismissInspectedUser() }
        )
    }
}

@Composable
private fun AdminAiChatTab(
    conversations: List<AdminAiConversationSummary>,
    activeConversationId: Long?,
    messages: List<AdminAiMessage>,
    suggestedPrompts: List<String>,
    isSending: Boolean,
    executedTools: List<com.calisthenics.ai.domain.model.AdminToolExecution> = emptyList(),
    timeRangeUsed: com.calisthenics.ai.domain.model.AdminTimeRange? = null,
    matchedUsers: List<com.calisthenics.ai.domain.model.AdminGlobalUserSummary> = emptyList(),
    missingDataNotes: List<String> = emptyList(),
    gettingStartedProgram: GettingStartedProgram? = null,
    gettingStartedPreview: GettingStartedProgramPreview? = null,
    activeCampaigns: List<GettingStartedCampaignSummary> = emptyList(),
    campaignAnalytics: GettingStartedCampaignAnalytics? = null,
    pendingConfirmation: AdminPendingConfirmation? = null,
    isResolvingConfirmation: Boolean = false,
    onSelectConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onSendMessage: (String) -> Unit,
    onResolveConfirmation: (Boolean) -> Unit = {},
    onInspectUser: (Long) -> Unit = {}
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onNewConversation) {
                Text("+ New Session")
            }
            conversations.forEach { conv ->
                FilterChip(
                    selected = conv.id == activeConversationId,
                    onClick = { onSelectConversation(conv.id) },
                    label = {
                        Text(
                            text = conv.title.take(28),
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Suggested Prompts",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedPrompts.forEach { prompt ->
                AssistChip(
                    onClick = {
                        if (!isSending) {
                            onSendMessage(prompt)
                        }
                    },
                    label = { Text(prompt) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (messages.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dedicated Admin Operations & Training Advisor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Ask about platform activity, skill progression gaps (Planche, Front Lever, Back Lever, Handstand, Muscle-up, Human Flag), draft workout programs, or operational improvements. All recommendations require your explicit approval before modifying production records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role.equals("USER", ignoreCase = true)
                    if (isUser) {
                        AiUserMessageBubble(text = msg.content)
                    } else {
                        AiAssistantMessageBubble(
                            markdown = msg.content,
                            badgeText = msg.intent
                        )
                    }
                }

                if (gettingStartedPreview != null) {
                    item(key = "gs_preview") {
                        GettingStartedPreviewCard(preview = gettingStartedPreview)
                    }
                } else if (gettingStartedProgram != null && activeCampaigns.isEmpty() && campaignAnalytics == null) {
                    item(key = "gs_program") {
                        GettingStartedProgramStatusCard(program = gettingStartedProgram)
                    }
                }

                if (activeCampaigns.isNotEmpty()) {
                    item(key = "gs_campaigns") {
                        GettingStartedCampaignsCard(campaigns = activeCampaigns)
                    }
                }

                if (campaignAnalytics != null) {
                    item(key = "gs_analytics") {
                        GettingStartedAnalyticsCard(analytics = campaignAnalytics)
                    }
                }
            }
        }

        if (isSending) {
            AiStatusRow(statusText = "Analyzing platform data...")
        }

        if (pendingConfirmation != null) {
            AdminPendingConfirmationCard(
                confirmation = pendingConfirmation,
                isResolving = isResolvingConfirmation,
                onApprove = { onResolveConfirmation(true) },
                onCancel = { onResolveConfirmation(false) }
            )
        }

        if (executedTools.isNotEmpty() || timeRangeUsed != null || matchedUsers.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rangeLabel = timeRangeUsed?.let {
                        "${it.range}: ${it.startDate ?: "-"} to ${it.endDate ?: "-"}"
                    }
                    if (executedTools.isNotEmpty() || rangeLabel != null) {
                        Text(
                            text = buildString {
                                if (rangeLabel != null) append("Window: $rangeLabel")
                                if (executedTools.isNotEmpty()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("Tools: ")
                                    append(executedTools.joinToString(", ") {
                                        "${it.toolName}${it.recordCount?.let { c -> " ($c)" } ?: ""}"
                                    })
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (matchedUsers.isNotEmpty()) {
                        Text(
                            text = "Matched Users (tap to inspect dossier):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            matchedUsers.take(10).forEach { user ->
                                AssistChip(
                                    onClick = { onInspectUser(user.userId) },
                                    label = {
                                        Text(
                                            "${user.fullName} (#${user.userId}, ${user.activityStatus}, ${formatPct(user.workoutCompletionRate)})"
                                        )
                                    }
                                )
                            }
                        }
                    }
                    if (missingDataNotes.isNotEmpty()) {
                        Text(
                            text = "Data completeness: ${missingDataNotes.first()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        AiChatComposer(
            prompt = inputText,
            onPromptChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank() && !isSending) {
                    val toSend = inputText
                    inputText = ""
                    onSendMessage(toSend)
                }
            },
            isGenerating = isSending,
            placeholder = "Ask Admin AI Agent..."
        )
    }
}

@Composable
private fun AdminPendingConfirmationCard(
    confirmation: AdminPendingConfirmation,
    isResolving: Boolean,
    onApprove: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current
    val isRevokePro = confirmation.actionType.equals("REVOKE_PRO", ignoreCase = true)
    val isGrantPro = confirmation.actionType.equals("GRANT_PRO", ignoreCase = true)
    val highlightColor = when {
        isRevokePro -> Error
        isGrantPro -> Accent
        else -> Info
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = highlightColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Confirmation Required: ${confirmation.actionType.replace('_', ' ')}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            }

            if (confirmation.isProAction) {
                Text(
                    text = "Target Account: ${confirmation.targetUserName ?: "User"} (ID #${confirmation.targetUserId ?: "-"})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                confirmation.targetUserEmail?.takeIf { it.isNotBlank() }?.let { email ->
                    Text(
                        text = "Email: $email",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
                Text(
                    text = "Current Tier: ${confirmation.targetUserCurrentTier ?: "FREE"} (${confirmation.targetUserMembershipSource ?: "Default"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                if (confirmation.targetUserPaidPro == true) {
                    Text(
                        text = "Paid PRO subscription is active and preserved independently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Info
                    )
                }
                confirmation.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Text(
                        text = "Reason: $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            } else {
                Text(
                    text = "Program: ${confirmation.programTitle ?: "Getting Started"} (#${confirmation.programId ?: "-"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryText
                )
                Text(
                    text = "Audience: ${confirmation.audienceLabel ?: confirmation.targetAudience ?: "ALL_ATHLETES"} • Eligible athletes: ${confirmation.eligibleAthleteCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }

            confirmation.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isResolving,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onApprove,
                    enabled = !isResolving,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRevokePro) Error else Accent,
                        contentColor = if (isRevokePro) Color.White else colors.onAccent
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        when {
                            isGrantPro -> "Confirm Grant PRO"
                            isRevokePro -> "Confirm Revoke PRO"
                            confirmation.actionType.contains("UNPUBLISH", ignoreCase = true) -> "Confirm Unpublish"
                            else -> "Confirm Publish"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GettingStartedPreviewCard(preview: GettingStartedProgramPreview) {
    val program = preview.program
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Program Preview: ${program?.title ?: "Getting Started"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Status: ${if (program?.published == true) "Published" else "Draft"} v${program?.version ?: 1} • ${if (preview.valid) "Valid" else "Needs fixes"}",
                style = MaterialTheme.typography.labelMedium,
                color = if (preview.valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                text = "Target: ${preview.audienceLabel ?: preview.targetAudience ?: "ALL_ATHLETES"} • Est. audience: ${preview.eligibleAthleteCount} / ${preview.totalAthleteCount} athletes",
                style = MaterialTheme.typography.bodySmall
            )
            if (preview.validationErrors.isNotEmpty()) {
                preview.validationErrors.forEach { err ->
                    Text(
                        text = "• $err",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            program?.days?.sortedBy { it.dayNumber }?.forEach { day ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "Day ${day.dayNumber}: ${day.title} (${day.exercises.size} exercises)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                day.exercises.forEach { ex ->
                    val volume = ex.reps?.let { "${ex.sets}x$it reps" }
                        ?: ex.durationSeconds?.let { "${ex.sets}x${it}s hold" }
                        ?: "${ex.sets} sets"
                    Text(
                        text = "  • ${ex.exerciseName}: $volume (rest ${ex.restSeconds}s)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun GettingStartedProgramStatusCard(program: GettingStartedProgram) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${program.title} (#${program.id})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (program.published) "Published" else "Draft"} • v${program.version} • Audience: ${program.targetAudience.name} • ${program.days.size} days",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GettingStartedCampaignsCard(campaigns: List<GettingStartedCampaignSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Active Getting Started Campaigns (${campaigns.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            campaigns.forEach { campaign ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    text = "${campaign.title} (v${campaign.version}, #${campaign.programId})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Audience: ${campaign.audienceLabel ?: campaign.targetAudience ?: "ALL_ATHLETES"} • Eligible: ${campaign.eligibleAthleteCount} • Enrollments: ${campaign.enrollments}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun GettingStartedAnalyticsCard(analytics: GettingStartedCampaignAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Campaign Analytics: ${analytics.programTitle}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (analytics.published) "Published" else "Draft"} v${analytics.version} • Audience: ${analytics.audienceLabel ?: analytics.targetAudience ?: "ALL_ATHLETES"}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Eligible: ${analytics.eligibleAthleteCount} • Impressions: ${analytics.modalImpressions} • Dismissals: ${analytics.modalDismissals}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Detail Views: ${analytics.programDetailViews} • Enrollments: ${analytics.enrollments} • Completed: ${analytics.completedEnrollments}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Day 1: ${formatPct(analytics.day1CompletionRate)} • Day 2: ${formatPct(analytics.day2CompletionRate)} • Day 3: ${formatPct(analytics.day3CompletionRate)} • Overall: ${formatPct(analytics.overallCompletionRate)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminAiInsightsTab(
    selectedRange: String,
    overview: AdminPlatformOverview?,
    training: AdminTrainingAnalytics?,
    operationalRecommendations: List<AdminAiRecommendation>,
    isGenerating: Boolean,
    isReviewing: Boolean,
    onRangeSelected: (String) -> Unit,
    onGenerateOperational: () -> Unit,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onImplement: (Long) -> Unit
) {
    val ranges = listOf(
        "TODAY" to "Today",
        "LAST_7_DAYS" to "Last 7 Days",
        "LAST_30_DAYS" to "Last 30 Days"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ranges.forEach { (code, label) ->
                FilterChip(
                    selected = selectedRange == code,
                    onClick = { onRangeSelected(code) },
                    label = { Text(label) }
                )
            }
        }

        overview?.let { ov ->
            Text(
                text = "Platform Overview (${ov.startDate ?: ""} to ${ov.endDate ?: ""})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("Registered Users", ov.totalRegisteredUsers.toString(), Modifier.weight(1f))
                StatCard("Active Users", "${ov.activeUsersInPeriod} (${formatPct(ov.activeUserRatePercent)})", Modifier.weight(1f))
                StatCard("New Registrations", ov.newRegistrationsInPeriod.toString(), Modifier.weight(1f))
                StatCard("Active Coach-Client", ov.activeCoachClientRelationships.toString(), Modifier.weight(1f))
                StatCard("Completed Workouts", "${ov.completedWorkoutsInPeriod} / ${ov.totalCompletedWorkoutsAllTime}", Modifier.weight(1f))
                StatCard("Recorded PRs", "${ov.recordedPrsInPeriod} / ${ov.totalRecordedPrsAllTime}", Modifier.weight(1f))
                StatCard("Workout Plans", "${ov.activeWorkoutPlans} active / ${ov.totalWorkoutPlans}", Modifier.weight(1f))
                StatCard("Exercise Library", "${ov.activeExercises} active / ${ov.totalExercises}", Modifier.weight(1f))
            }
        }

        training?.let { tr ->
            Text(
                text = "Skill Focus & Missing Progressions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            tr.skillFocusBreakdown.forEach { skill ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(skill.skillName, fontWeight = FontWeight.SemiBold)
                            Text("${skill.existingExerciseCount} catalog exercises", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "Sessions: ${skill.sessionPerformancesCount} • Plans: ${skill.planReferencesCount} • PRs: ${skill.prCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (skill.missingProgressions.isNotEmpty()) {
                            Text(
                                text = "Missing Progressions: ${skill.missingProgressions.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (tr.mostPerformedExercises.isNotEmpty()) {
                Text(
                    text = "Most Frequently Performed Exercises",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                tr.mostPerformedExercises.take(5).forEach { ex ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ex.exerciseName, fontWeight = FontWeight.SemiBold)
                                Text("${ex.category} • ${ex.difficulty}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "${ex.sessionPerformances} sessions (${formatPct(ex.completionRatePercent)})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            tr.inactiveUsersSummary?.let { inact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("User Inactivity & Plan Adherence", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Inactive >14d: ${inact.inactiveOver14DaysCount}/${inact.totalUsersEvaluated} (${formatPct(inact.inactiveRatePercent)}) • Inactive >30d: ${inact.inactiveOver30DaysCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        tr.planAdherenceSummary?.let { adh ->
                            Text(
                                text = "Users with active plan: ${adh.usersWithActivePersonalPlan} • Coach assignment adherence: ${formatPct(adh.adherenceRatePercent)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Operational Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                enabled = !isGenerating,
                onClick = onGenerateOperational
            ) {
                Text(if (isGenerating) "Analyzing..." else "Generate Insights")
            }
        }

        operationalRecommendations.forEach { rec ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rec.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        PriorityBadge(rec.priority)
                    }
                    Text("Status: ${rec.status}", style = MaterialTheme.typography.labelSmall)
                    rec.operationalDetails?.let { op ->
                        Text("Issue: ${op.identifiedIssue}", style = MaterialTheme.typography.bodySmall)
                        op.supportingData?.let { Text("Data: $it", style = MaterialTheme.typography.bodySmall) }
                        op.suggestedAction?.let { Text("Suggested Action: $it", style = MaterialTheme.typography.bodySmall) }
                        op.expectedBenefit?.let { Text("Expected Benefit: $it", style = MaterialTheme.typography.bodySmall) }
                    } ?: rec.summary?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (rec.status == "PENDING_APPROVAL" || rec.status == "DRAFT") {
                            Button(enabled = !isReviewing, onClick = { onApprove(rec.id) }) {
                                Text("Approve")
                            }
                            OutlinedButton(enabled = !isReviewing, onClick = { onReject(rec.id) }) {
                                Text("Reject")
                            }
                        } else if (rec.status == "APPROVED") {
                            Button(enabled = !isReviewing, onClick = { onImplement(rec.id) }) {
                                Text("Mark Implemented")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminExerciseRecommendationsTab(
    recommendations: List<AdminAiRecommendation>,
    isGenerating: Boolean,
    isReviewing: Boolean,
    onGenerateForSkill: (String) -> Unit,
    onApprove: (Long, Boolean) -> Unit,
    onReject: (Long) -> Unit,
    onConfirmPublication: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit
) {
    val skills = listOf("Planche", "Front Lever", "Back Lever", "Handstand", "Muscle-up", "Human Flag")
    var selectedSkill by rememberSaveable { mutableStateOf("Planche") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Exercise Recommendation Engine",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skills.forEach { skill ->
                FilterChip(
                    selected = selectedSkill == skill,
                    onClick = { selectedSkill = skill },
                    label = { Text(skill) }
                )
            }
        }
        Button(
            onClick = { onGenerateForSkill(selectedSkill) },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isGenerating) "Analyzing $selectedSkill Library..." else "Recommend Missing $selectedSkill Progressions")
        }

        if (recommendations.isEmpty()) {
            Text(
                text = "No exercise recommendations yet. Select a skill above or ask the Admin AI Agent what exercises to add.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recommendations, key = { it.id }) { rec ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                PriorityBadge(rec.priority)
                            }
                            Text(
                                text = "Status: ${rec.status}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            rec.exerciseDetails?.let { ex ->
                                Text("Category: ${ex.category} • Difficulty: ${ex.difficulty}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                ex.targetSkill?.let { Text("Skill: $it (${ex.progressionLevel ?: "Progression"})", style = MaterialTheme.typography.bodySmall) }
                                ex.equipment?.let { Text("Equipment: $it", style = MaterialTheme.typography.bodySmall) }
                                ex.targetMuscles?.let { Text("Target Muscles: $it", style = MaterialTheme.typography.bodySmall) }
                                ex.prerequisites?.let { Text("Prerequisites: $it", style = MaterialTheme.typography.bodySmall) }
                                ex.description?.let { Text("Description: $it", style = MaterialTheme.typography.bodySmall) }
                                ex.whyAdd?.let { Text("Why Add: $it", style = MaterialTheme.typography.bodySmall) }
                            } ?: rec.rationale?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (rec.status) {
                                    "DRAFT", "PENDING_APPROVAL" -> {
                                        Button(
                                            enabled = !isReviewing,
                                            onClick = { onApprove(rec.id, false) }
                                        ) {
                                            Text("Approve (Draft)")
                                        }
                                        OutlinedButton(
                                            enabled = !isReviewing,
                                            onClick = { onReject(rec.id) }
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                    "APPROVED" -> {
                                        Button(
                                            enabled = !isReviewing,
                                            onClick = { onConfirmPublication(rec.id) }
                                        ) {
                                            Text("Confirm Publication")
                                        }
                                        rec.createdResourceId?.let { exId ->
                                            OutlinedButton(onClick = { onOpenExercise(exId) }) {
                                                Text("Edit Exercise #$exId")
                                            }
                                        }
                                    }
                                    "IMPLEMENTED" -> {
                                        rec.createdResourceId?.let { exId ->
                                            OutlinedButton(onClick = { onOpenExercise(exId) }) {
                                                Text("View Published Exercise #$exId")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDraftWorkoutProgramsTab(
    recommendations: List<AdminAiRecommendation>,
    isGenerating: Boolean,
    isReviewing: Boolean,
    onGenerateProgram: (String, String) -> Unit,
    onApprove: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onOpenWorkoutPlan: (Long) -> Unit
) {
    val skills = listOf("Planche", "Front Lever", "Handstand", "Muscle-up", "General Strength & Conditioning")
    val levels = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED")
    var selectedSkill by rememberSaveable { mutableStateOf("Planche") }
    var selectedLevel by rememberSaveable { mutableStateOf("BEGINNER") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Draft Workout Program Generator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skills.forEach { skill ->
                FilterChip(
                    selected = selectedSkill == skill,
                    onClick = { selectedSkill = skill },
                    label = { Text(skill) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            levels.forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { selectedLevel = level },
                    label = { Text(level) }
                )
            }
        }
        Button(
            onClick = { onGenerateProgram(selectedSkill, selectedLevel) },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isGenerating) "Generating Draft Program..." else "Generate Draft $selectedLevel $selectedSkill Program")
        }

        if (recommendations.isEmpty()) {
            Text(
                text = "No draft workout programs yet. Generate one above or ask the Admin AI Agent to propose a program.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recommendations, key = { it.id }) { rec ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                PriorityBadge(rec.priority)
                            }
                            Text(
                                text = "Status: ${rec.status} (Never auto-published or auto-assigned)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            rec.programDetails?.let { prog ->
                                Text(
                                    text = "Level: ${prog.trainingLevel ?: "All"} • ${prog.weeklyFrequency}x/week • ${prog.durationWeeks} weeks",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                prog.targetAudience?.let { Text("Target Audience: $it", style = MaterialTheme.typography.bodySmall) }
                                prog.requiredEquipment?.let { Text("Equipment: $it", style = MaterialTheme.typography.bodySmall) }
                                prog.progressionCriteria?.let { Text("Progression Criteria: $it", style = MaterialTheme.typography.bodySmall) }
                                prog.safetyConsiderations?.let { Text("Safety Considerations: $it", style = MaterialTheme.typography.bodySmall) }
                                prog.rationale?.let { Text("Reasoning: $it", style = MaterialTheme.typography.bodySmall) }

                                prog.days.filterNot { it.restDay }.forEach { day ->
                                    Text(
                                        text = "${day.dayOfWeek}: ${day.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    day.exercises.forEach { ex ->
                                        val prescription = if (ex.holdSeconds != null && ex.holdSeconds > 0) {
                                            "${ex.sets} sets x ${ex.holdSeconds}s hold (rest ${ex.restSeconds}s)"
                                        } else {
                                            "${ex.sets} sets x ${ex.reps ?: 8} reps (rest ${ex.restSeconds}s)"
                                        }
                                        Text(
                                            text = " • ${ex.exerciseName}: $prescription",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                when (rec.status) {
                                    "DRAFT", "PENDING_APPROVAL" -> {
                                        Button(
                                            enabled = !isReviewing,
                                            onClick = { onApprove(rec.id) }
                                        ) {
                                            Text("Approve & Create Draft Plan")
                                        }
                                        OutlinedButton(
                                            enabled = !isReviewing,
                                            onClick = { onReject(rec.id) }
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                    "APPROVED", "IMPLEMENTED" -> {
                                        rec.createdResourceId?.let { planId ->
                                            OutlinedButton(onClick = { onOpenWorkoutPlan(planId) }) {
                                                Text("Open Created Draft Plan #$planId")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: String) {
    val bgColor = when (priority.uppercase(Locale.ROOT)) {
        "HIGH" -> Color(0xFFB3261E)
        "LOW" -> Color(0xFF386A20)
        else -> Color(0xFF7D5700)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priority,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AdminUsersAndSegmentsTab(
    selectedRange: String,
    isLoading: Boolean,
    isLoadingUserDetail: Boolean,
    userStats: com.calisthenics.ai.domain.model.AdminGlobalUserStatistics?,
    coachStats: com.calisthenics.ai.domain.model.AdminCoachClientStatistics?,
    platformEngagement: com.calisthenics.ai.domain.model.AdminPlatformEngagement?,
    userSegments: com.calisthenics.ai.domain.model.AdminUserSegments?,
    currentFilter: com.calisthenics.ai.domain.model.AdminUserSegmentFilter,
    onEnsureLoaded: () -> Unit,
    onRangeSelected: (String) -> Unit,
    onApplySegmentFilter: (com.calisthenics.ai.domain.model.AdminUserSegmentFilter) -> Unit,
    onInspectUser: (Long) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(selectedRange) {
        onEnsureLoaded()
    }

    var searchQuery by rememberSaveable { mutableStateOf(currentFilter.searchQuery ?: "") }
    var naturalLanguageQuery by rememberSaveable { mutableStateOf(currentFilter.naturalLanguageQuery ?: "") }
    var selectedActivity by rememberSaveable { mutableStateOf(currentFilter.activityStatus ?: "ALL") }
    var selectedExperience by rememberSaveable { mutableStateOf(currentFilter.experienceLevel ?: "ALL") }
    var selectedCoachStatus by rememberSaveable { mutableStateOf(currentFilter.coachRelationshipStatus ?: "ALL") }

    val ranges = listOf(
        "TODAY" to "Today",
        "LAST_7_DAYS" to "Last 7 Days",
        "LAST_30_DAYS" to "Last 30 Days"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ranges.forEach { (code, label) ->
                    FilterChip(
                        selected = selectedRange == code,
                        onClick = { onRangeSelected(code) },
                        label = { Text(label) }
                    )
                }
            }
            OutlinedButton(
                enabled = !isLoading,
                onClick = { onRangeSelected(selectedRange) }
            ) {
                Text("Refresh")
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Read-only Admin RBAC Access • Emails masked unless explicitly revealed • Raw private chat & AI messages are never exposed.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(10.dp)
            )
        }

        if (isLoading || isLoadingUserDetail) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isLoadingUserDetail) "Loading user dossier..." else "Aggregating platform user data...",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        userStats?.let { stats ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Global User Statistics (${stats.timeRange?.startDate ?: "-"} to ${stats.timeRange?.endDate ?: "-"})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Registered: ${stats.totalRegisteredUsers} (Clients: ${stats.totalClients}, Coaches: ${stats.totalCoaches}, Verified Coaches: ${stats.verifiedCoaches}, Admins: ${stats.totalAdmins})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Activity: Today ${stats.activeUsersToday} • 7d ${stats.activeUsers7Days} • 30d ${stats.activeUsers30Days} • New in Period: ${stats.newRegistrationsInPeriod}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Retention Risk: Inactive 14d+ ${stats.inactiveUsers14PlusDays} • At-Risk 7-14d ${stats.atRiskUsers7To14Days}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Coaching: With Active Coach ${stats.usersWithActiveCoach} • Pending Invite ${stats.usersWithPendingCoachInvite} • No Coach ${stats.usersWithoutCoach}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Workouts (${stats.timeRange?.range ?: selectedRange}): ${stats.completedWorkoutsInRange}/${stats.totalWorkoutsInRange} completed (${formatPct(stats.overallWorkoutCompletionRate)}) • Incomplete/Abandoned: ${stats.incompleteOrAbandonedWorkoutsInRange}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Personal Records: ${stats.totalPersonalRecordsAllTime} all-time (${stats.newPersonalRecordsInRange} new) • Stale PRs 30d+: ${stats.usersWithStalePrs30PlusDays} users",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (stats.missingDataNotes.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        stats.missingDataNotes.forEach { note ->
                            Text(
                                text = "Missing Data: $note",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        coachStats?.let { cs ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Coach-Client Operations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active Relationships: ${cs.activeRelationships}/${cs.totalRelationships} • Pending Invites: ${cs.pendingInvitations} • Assigned Plans Active: ${cs.activeAssignedPlans}/${cs.totalAssignedPlans}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Shared PRs: ${cs.totalSharedPrs} • Coach Feedback Response Rate: ${formatPct(cs.feedbackResponseRate)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (cs.topCoachNames.isNotEmpty()) {
                        Text(
                            text = "Top Coaches: ${cs.topCoachNames.joinToString("; ")}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        platformEngagement?.let { eng ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Platform Engagement & Experience Comparison",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DAU ${eng.dauCount} • WAU ${eng.wauCount} • MAU ${eng.mauCount} • Stickiness ${formatPct(eng.stickinessDauToMauPercent)} • AI msgs: ${eng.totalAiMessagesInRange} • Chat msgs: ${eng.totalChatMessagesInRange}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    eng.experienceComparisons.forEach { row ->
                        Text(
                            text = " • ${row.experienceLevel}: ${row.userCount} users, ${formatPct(row.activeRatePercent)} active, ${String.format(Locale.US, "%.1f", row.avgWorkoutsPerUser)} workouts/user, ${formatPct(row.avgCompletionRate)} completion, ${row.prCount} PRs",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (eng.topSkillImprovers.isNotEmpty()) {
                        Text(
                            text = "Top PR Improver: ${eng.topSkillImprovers.first().fullName} (${eng.topSkillImprovers.first().exerciseName}${eng.topSkillImprovers.first().improvementPercent?.let { " +${formatPct(it)}" } ?: ""})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "User Segmentation & Search",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = naturalLanguageQuery,
                    onValueChange = { naturalLanguageQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Natural-language segment query (optional)") },
                    placeholder = { Text("e.g. beginner Planche no coach completion below 50%") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search name, masked email, or goal") },
                    singleLine = true
                )

                Text("Activity Status", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "ACTIVE", "AT_RISK", "INACTIVE").forEach { status ->
                        FilterChip(
                            selected = selectedActivity == status,
                            onClick = { selectedActivity = status },
                            label = { Text(status) }
                        )
                    }
                }

                Text("Experience Level", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "BEGINNER", "INTERMEDIATE", "ADVANCED").forEach { exp ->
                        FilterChip(
                            selected = selectedExperience == exp,
                            onClick = { selectedExperience = exp },
                            label = { Text(exp) }
                        )
                    }
                }

                Text("Coach Status", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "HAS_ACTIVE_COACH", "NO_COACH", "PENDING_INVITATION").forEach { cs ->
                        FilterChip(
                            selected = selectedCoachStatus == cs,
                            onClick = { selectedCoachStatus = cs },
                            label = { Text(cs) }
                        )
                    }
                }

                Button(
                    enabled = !isLoading,
                    onClick = {
                        onApplySegmentFilter(
                            com.calisthenics.ai.domain.model.AdminUserSegmentFilter(
                                searchQuery = searchQuery.trim().ifBlank { null },
                                naturalLanguageQuery = naturalLanguageQuery.trim().ifBlank { null },
                                activityStatus = selectedActivity.takeIf { it != "ALL" },
                                experienceLevel = selectedExperience.takeIf { it != "ALL" },
                                coachRelationshipStatus = selectedCoachStatus.takeIf { it != "ALL" },
                                range = selectedRange,
                                page = 0,
                                size = 20
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run User Segment Query")
                }
            }
        }

        userSegments?.let { seg ->
            Text(
                text = "${seg.segmentSummary ?: "Matched Users"} (${seg.matchedCount} matched, page ${seg.page + 1}/${maxOf(1, seg.totalPages)})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            seg.users.forEach { u ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${u.fullName} (#${u.userId})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${u.accountCategory} • ${u.activityStatus}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Email: ${u.maskedEmail ?: "hidden"} • Level: ${u.experienceLevel ?: "UNSPECIFIED"} • Coach: ${u.coachRelationshipStatus ?: "-"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Workouts: ${u.completedWorkouts}/${u.totalWorkouts} (${formatPct(u.workoutCompletionRate)}) • PRs: ${u.totalPersonalRecords} • Trend: ${u.consistencyTrend ?: "INSUFFICIENT_DATA"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!u.primaryGoals.isNullOrBlank()) {
                            Text(
                                text = "Goals: ${u.primaryGoals}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { onInspectUser(u.userId) }
                        ) {
                            Text("Inspect User Dossier")
                        }
                    }
                }
            }

            if (seg.totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        enabled = seg.page > 0 && !isLoading,
                        onClick = {
                            onApplySegmentFilter(currentFilter.copy(page = seg.page - 1, range = selectedRange))
                        }
                    ) {
                        Text("Previous")
                    }
                    OutlinedButton(
                        enabled = (seg.page + 1) < seg.totalPages && !isLoading,
                        onClick = {
                            onApplySegmentFilter(currentFilter.copy(page = seg.page + 1, range = selectedRange))
                        }
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserDossierDialog(
    dossier: com.calisthenics.ai.domain.model.AdminUserDetailDossier,
    isLoading: Boolean,
    onRevealEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${dossier.fullName} (#${dossier.userId})")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Role: ${dossier.accountCategory} (${dossier.role}) • Level: ${dossier.experienceLevel ?: "UNSPECIFIED"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Email: ${dossier.email ?: dossier.maskedEmail ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (dossier.email == null) {
                    OutlinedButton(
                        enabled = !isLoading,
                        onClick = onRevealEmail
                    ) {
                        Text("Reveal Authorized Email (Audit Logged)")
                    }
                }
                Text(
                    text = "Goals: ${dossier.primaryGoals ?: "N/A"} • Frequency: ${dossier.preferredFrequency ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Body: Height ${dossier.heightCm ?: "-"} cm • Latest Weight ${dossier.latestWeightKg ?: "-"} kg",
                    style = MaterialTheme.typography.bodySmall
                )
                if (dossier.inferredAvailableEquipment.isNotEmpty()) {
                    Text(
                        text = "Equipment (inferred): ${dossier.inferredAvailableEquipment.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                dossier.workoutSummary?.let { w ->
                    HorizontalDivider()
                    Text("Workouts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Completed ${w.completedSessions}/${w.totalSessions} (${formatPct(w.completionRate)}) • In Progress ${w.inProgressSessions} • Cancelled ${w.cancelledSessions} • Trend: ${w.consistencyTrend ?: "-"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (w.assignedPlanNames.isNotEmpty()) {
                        Text("Assigned Plans: ${w.assignedPlanNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                    w.recentSessionLabels.take(4).forEach { s ->
                        Text(" • $s", style = MaterialTheme.typography.labelSmall)
                    }
                }

                dossier.personalRecordsSummary?.let { pr ->
                    HorizontalDivider()
                    Text("Personal Records", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Total PRs: ${pr.totalRecords} (${pr.recordsInRange} in period) • Shared: ${pr.sharedPrCount} • Coach Feedback: ${pr.coachFeedbackCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    pr.recordSummaries.take(4).forEach { r ->
                        Text(" • $r", style = MaterialTheme.typography.labelSmall)
                    }
                    pr.feedbackSummaries.take(3).forEach { f ->
                        Text(" • $f", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                dossier.progressSummary?.let { prog ->
                    HorizontalDivider()
                    Text("Progress & Consistency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Current Period Workouts: ${prog.currentPeriodWorkouts} vs Previous: ${prog.previousPeriodWorkouts}${prog.workoutVolumeDeltaPercent?.let { " (${formatPct(it)})" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                dossier.activitySummary?.let { act ->
                    HorizontalDivider()
                    Text("Chat, AI & Analysis Activity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Presence: ${act.presenceStatus ?: "UNKNOWN"} • Chat Sent: ${act.chatMessagesSentInRange} • AI Msgs: ${act.aiMessagesInRange} • Planner Runs: ${act.workoutPlannerRunsInRange}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    act.privacyNotice?.let { notice ->
                        Text(notice, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (dossier.missingDataFields.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Missing / Incomplete Data Fields", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    dossier.missingDataFields.forEach { field ->
                        Text(" • $field", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatPct(value: Double): String = String.format(Locale.US, "%.1f%%", value)
