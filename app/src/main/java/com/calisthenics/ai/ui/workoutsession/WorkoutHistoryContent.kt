package com.calisthenics.ai.ui.workoutsession

import com.calisthenics.ai.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionClassification
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.home.GettingStartedCompletedProgramCard
import com.calisthenics.ai.ui.pr.SharePrBottomSheet
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.LocalAppColors
import com.calisthenics.ai.ui.theme.Warning
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    onSessionSelected: (Long) -> Unit,
    onAnalysisSessionSelected: ((String) -> Unit)? = null,
    onResumeSession: ((WorkoutSession) -> Unit)? = null,
    onExploreAiPlanner: () -> Unit = {},
    onCreateWorkoutPlan: () -> Unit = {},
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text(stringResource(R.string.workout_history)) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text(stringResource(R.string.live_analysis_history)) }
                )
            }

            if (state.selectedTab == 0) {
                val today = remember { LocalDate.now() }
                val completedSessions = remember(state.sessions, today) {
                    state.sessions.filter {
                        it.effectiveClassification(today) == WorkoutSessionClassification.COMPLETED
                    }
                }
                val visibleSessions = remember(
                    state.selectedFilter,
                    state.unfinishedSessions,
                    state.upcomingSessions,
                    completedSessions
                ) {
                    when (state.selectedFilter) {
                        WorkoutHistoryFilter.ALL ->
                            (state.unfinishedSessions + completedSessions + state.upcomingSessions).distinctBy { it.id }
                        WorkoutHistoryFilter.COMPLETED -> completedSessions
                        WorkoutHistoryFilter.UNFINISHED -> state.unfinishedSessions
                        WorkoutHistoryFilter.UPCOMING -> state.upcomingSessions
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            WorkoutHistoryFilter.ALL to stringResource(R.string.common_all),
                            WorkoutHistoryFilter.COMPLETED to stringResource(R.string.completed_status),
                            WorkoutHistoryFilter.UNFINISHED to stringResource(R.string.loc_unfinished_status),
                            WorkoutHistoryFilter.UPCOMING to stringResource(R.string.upcoming_status)
                        ).forEach { (filter, label) ->
                            FilterChip(
                                selected = state.selectedFilter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(label) }
                            )
                        }
                    }
                    val showCompletedPrograms = (state.selectedFilter == WorkoutHistoryFilter.ALL ||
                        state.selectedFilter == WorkoutHistoryFilter.COMPLETED) &&
                        state.completedGettingStartedPrograms.isNotEmpty()
                    when {
                        state.isLoading -> Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) { CircularProgressIndicator() }
                        state.error != null && visibleSessions.isEmpty() && !showCompletedPrograms -> EmptyState(
                            title = stringResource(R.string.history_unavailable),
                            body = state.error ?: "Could not load workout history.",
                            action = stringResource(R.string.common_retry),
                            onAction = viewModel::load
                        )
                        visibleSessions.isEmpty() && !showCompletedPrograms -> EmptyState(
                            title = when (state.selectedFilter) {
                                WorkoutHistoryFilter.UNFINISHED -> stringResource(R.string.no_unfinished_workouts)
                                WorkoutHistoryFilter.UPCOMING -> stringResource(R.string.no_upcoming_workouts)
                                else -> stringResource(R.string.loc_no_completed_workouts)
                            },
                            body = when (state.selectedFilter) {
                                WorkoutHistoryFilter.UNFINISHED -> stringResource(R.string.no_unfinished_workouts)
                                WorkoutHistoryFilter.UPCOMING -> stringResource(R.string.no_upcoming_workouts)
                                else -> "Finished workouts will appear here."
                            }
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showCompletedPrograms) {
                                item(key = "completed-programs-header") {
                                    Text(
                                        text = stringResource(R.string.getting_started_completed_programs_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                items(
                                    state.completedGettingStartedPrograms,
                                    key = { "gs-completed-${it.id}" }
                                ) { completedProgram ->
                                    GettingStartedCompletedProgramCard(
                                        enrollment = completedProgram,
                                        onExploreAiPlanner = onExploreAiPlanner,
                                        onCreateWorkoutPlan = onCreateWorkoutPlan
                                    )
                                }
                            }
                            items(visibleSessions, key = { it.id }) { session ->
                                val classification = session.effectiveClassification(today)
                                WorkoutHistoryRow(
                                    session = session,
                                    classification = classification,
                                    onClick = {
                                        when (classification) {
                                            WorkoutSessionClassification.UNFINISHED ->
                                                onResumeSession?.invoke(session) ?: onSessionSelected(session.id)
                                            WorkoutSessionClassification.COMPLETED ->
                                                onSessionSelected(session.id)
                                            else -> Unit
                                        }
                                    }
                                )
                            }
                            if (state.error != null) {
                                item { Text(state.error ?: "", color = MaterialTheme.colorScheme.error) }
                            }
                            val supportsPaging = state.selectedFilter == WorkoutHistoryFilter.ALL ||
                                state.selectedFilter == WorkoutHistoryFilter.COMPLETED
                            if (supportsPaging && state.page + 1 < state.totalPages) {
                            item {
                                OutlinedButton(
                                    onClick = viewModel::loadMore,
                                    enabled = !state.isLoadingMore,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (state.isLoadingMore) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                                    else Text(stringResource(R.string.load_more))
                                }
                            }
                        }
                        }
                    }
                }
            } else {
                when {
                    state.isLoadingLiveAnalysis -> Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) { CircularProgressIndicator() }
                    state.liveAnalysisError != null && state.liveAnalysisSessions.isEmpty() -> EmptyState(
                        title = stringResource(R.string.history_unavailable),
                        body = state.liveAnalysisError ?: "Could not load AI analysis history.",
                        action = stringResource(R.string.common_retry),
                        onAction = viewModel::loadLiveAnalysis
                    )
                    state.liveAnalysisSessions.isEmpty() -> EmptyState(
                        title = stringResource(R.string.your_history_is_clear),
                        body = "Completed live AI analysis sessions will appear here."
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.liveAnalysisSessions, key = { it.id }) { session ->
                            LiveAnalysisHistoryRow(
                                session = session,
                                onClick = { onAnalysisSessionSelected?.invoke(session.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAnalysisHistoryRow(session: LiveSessionRecord, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = appColors.accent)
                    Text(
                        session.exerciseDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                session.averageScore?.let { score ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (if (score >= 80.0) Color(0xFF4CAF50) else Color(0xFFFFA000)).copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "${score.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (score >= 80.0) Color(0xFF4CAF50) else Color(0xFFFFA000),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                formatWorkoutDate(session.startedAt ?: session.completedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${session.totalReps} reps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDuration(session.durationSeconds.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(
    session: WorkoutSession,
    classification: WorkoutSessionClassification,
    onClick: () -> Unit
) {
    val isClickable = classification != WorkoutSessionClassification.UPCOMING &&
        classification != WorkoutSessionClassification.CANCELLED
    val badgeLabel = when (classification) {
        WorkoutSessionClassification.UPCOMING -> stringResource(R.string.upcoming_status)
        WorkoutSessionClassification.UNFINISHED -> stringResource(R.string.loc_unfinished_status)
        else -> stringResource(R.string.completed_status)
    }
    val badgeContainer = when (classification) {
        WorkoutSessionClassification.UPCOMING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        WorkoutSessionClassification.UNFINISHED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeText = when (classification) {
        WorkoutSessionClassification.UPCOMING -> MaterialTheme.colorScheme.primary
        WorkoutSessionClassification.UNFINISHED -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Card(
        Modifier.fillMaxWidth().then(if (isClickable) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        session.title,
                        Modifier.padding(start = 9.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = badgeContainer
                ) {
                    Text(
                        badgeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                formatWorkoutDate(session.workoutDate ?: session.completedAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (classification == WorkoutSessionClassification.COMPLETED) {
                Text(
                    "${formatDuration(session.durationSeconds ?: 0)} · ${session.completedExercises} completed · " +
                        "${session.skippedExercises} skipped",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    onReturnToToday: () -> Unit,
    onViewHistory: () -> Unit,
    onResume: (WorkoutSession) -> Unit,
    onExploreAiPlanner: () -> Unit = {},
    onCreateWorkoutPlan: () -> Unit = {},
    viewModel: WorkoutSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val appColors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.shareSuccessMessage, state.error) {
        state.shareSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_summary)) },
                navigationIcon = {
                    IconButton(onClick = onReturnToToday) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to today") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            state.session == null -> EmptyState(
                title = stringResource(R.string.summary_unavailable),
                body = state.error ?: "Could not load this workout.",
                action = stringResource(R.string.common_retry),
                onAction = viewModel::load
            )
            else -> {
                val session = requireNotNull(state.session)
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.gettingStartedCompletedProgram?.let { completedProgram ->
                            item(key = "getting-started-completed-summary") {
                                GettingStartedCompletedProgramCard(
                                    enrollment = completedProgram,
                                    onExploreAiPlanner = onExploreAiPlanner,
                                    onCreateWorkoutPlan = onCreateWorkoutPlan
                                )
                            }
                        }
                        if (state.prs.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (appColors.isDark) appColors.surfaceVariant else Color(0xFF26210F)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Warning)
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        val unsharedCount = state.prs.count { pr -> state.sharedPrs.none { it.personalRecord.id == pr.id } }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.EmojiEvents,
                                                    contentDescription = stringResource(R.string.loc_pr_short),
                                                    tint = Warning,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Text(
                                                    stringResource(R.string.loc_new_personal_record),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Warning
                                                )
                                            }

                                            if (unsharedCount > 1 && state.hasActiveCoach) {
                                                Button(
                                                    onClick = { viewModel.shareAllRecordsWithCoach() },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (appColors.isDark) appColors.accent else Warning,
                                                        contentColor = if (appColors.isDark) appColors.onAccent else Color.Black
                                                    ),
                                                    enabled = !state.isSharing
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(stringResource(R.string.loc_share_all_count, unsharedCount), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        if (!state.hasActiveCoach) {
                                            Text(
                                                stringResource(R.string.loc_no_coach_assigned),
                                                color = if (appColors.isDark) appColors.primaryText else Color(0xFFE0E0E0),
                                                fontSize = 12.sp
                                            )
                                        } else if (state.coachName != null) {
                                            Text(
                                                "Assigned Coach: ${state.coachName}",
                                                color = Warning,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        state.prs.forEach { pr ->
                                            val isShared = state.sharedPrs.any { it.personalRecord.id == pr.id }
                                            val sharedRecord = state.sharedPrs.find { it.personalRecord.id == pr.id }

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (appColors.isDark) appColors.surface else Color(0xFF1B170B)
                                                )
                                            ) {
                                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            pr.exerciseName.ifEmpty { "Exercise" },
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (appColors.isDark) appColors.primaryText else Color.White,
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                        PillBadge(
                                                            pr.recordType.replace("MAX_", "").replace('_', ' ').ifEmpty { "PR" },
                                                            isAccent = true
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                pr.displayFormattedValue,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Accent
                                                            )
                                                            if (pr.improvementPercentage != null && pr.improvementPercentage > 0) {
                                                                Text(
                                                                    "+${pr.improvementPercentage}% improvement",
                                                                    color = Accent,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            } else if (pr.previousRecordValue == null) {
                                                                Text(
                                                                    stringResource(R.string.loc_first_recorded_performance),
                                                                    color = Color.LightGray,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }

                                                        if (isShared) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier
                                                                    .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Accent,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(
                                                                    if (sharedRecord?.coachFeedback != null) "Reviewed by Coach" else "Shared with Coach",
                                                                    color = Accent,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = { viewModel.openShareSheet(pr) },
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                shape = RoundedCornerShape(8.dp),
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = if (appColors.isDark) appColors.accent else Warning,
                                                                    contentColor = if (appColors.isDark) appColors.onAccent else Color.Black
                                                                ),
                                                                enabled = !state.isSharing
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(stringResource(R.string.share_with_coach), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }

                                                    if (sharedRecord?.coachFeedback != null) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    if (appColors.isDark) appColors.surfaceHover else Color(0xFF26210F),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .padding(8.dp),
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    stringResource(R.string.loc_coach_feedback),
                                                                    fontSize = 11.sp,
                                                                    color = Warning,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                if (sharedRecord.rating != null && sharedRecord.rating > 0) {
                                                                    Text(
                                                                        "★".repeat(sharedRecord.rating),
                                                                        color = Warning,
                                                                        fontSize = 12.sp
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                "\"${sharedRecord.coachFeedback}\"",
                                                                fontSize = 12.sp,
                                                                color = if (appColors.isDark) appColors.primaryText else Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(session.title, style = MaterialTheme.typography.headlineSmall)
                                    Text(formatWorkoutDate(session.workoutDate ?: session.completedAt))
                                    Text(stringResource(R.string.loc_duration_label, formatDuration(session.durationSeconds ?: 0)))
                                    Text(stringResource(R.string.loc_workout_history_summary, session.completedExercises, session.skippedExercises, session.unfinishedExercises))
                                    Text(stringResource(R.string.loc_workout_sets_summary, session.completedSets, session.totalReps, session.totalHoldDurationSeconds))
                                }
                            }
                        }
                        items(session.exercises.sortedBy { it.orderIndex }, key = { it.id }) { exercise ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                                    Text(exercise.status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() })
                                    exercise.sets.forEach { set ->
                                        Text(
                                            buildString {
                                                append("Set ${set.setNumber}: ")
                                                if (set.reps != null) append("${set.reps} reps")
                                                if (set.holdDurationSeconds != null) append("${if (set.reps != null) " · " else ""}${set.holdDurationSeconds}s hold")
                                                if (set.weightKg != null) append(" · +${set.weightKg} kg")
                                                if (set.assistanceWeightKg != null) append(" · ${set.assistanceWeightKg} kg assist")
                                                if (!set.completed) append(" · not completed")
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (session.status == "IN_PROGRESS") {
                        Button(onClick = { onResume(session) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.resume_workout))
                        }
                    }
                    Button(onClick = onReturnToToday, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.return_today))
                    }
                    TextButton(onClick = onViewHistory, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.open_workout_history))
                    }
                }
            }
        }
    }

    state.sharingPr?.let { prToShare ->
        val alreadySharedIds = remember(state.sharedPrs, prToShare.id) {
            state.sharedPrs
                .filter { it.personalRecord.id == prToShare.id }
                .map { it.coachUserId }
                .toSet()
        }
        SharePrBottomSheet(
            pr = prToShare,
            recipients = state.shareRecipients,
            alreadySharedRecipientIds = alreadySharedIds,
            isLoadingRecipients = false,
            isSharing = state.isSharing,
            errorMessage = state.shareError,
            onDismiss = { viewModel.dismissShareSheet() },
            onConfirmShare = { recipientId ->
                viewModel.shareRecordWithCoach(prToShare.id, recipientId)
            }
        )
    }
}

private fun formatWorkoutDate(value: String?): String {
    if (value.isNullOrBlank()) return "Workout date unavailable"
    return runCatching {
        LocalDate.parse(value.take(10))
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()))
    }.recoverCatching {
        ZonedDateTime.parse(value)
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()))
    }.getOrDefault(value)
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainder)
    else "%02d:%02d".format(minutes, remainder)
}
