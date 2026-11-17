package com.calisthenics.ai.ui.main

import com.calisthenics.ai.R

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.HomeTopBarActions
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.home.GettingStartedCompactBanner
import com.calisthenics.ai.ui.home.GettingStartedOnboardingHost
import com.calisthenics.ai.ui.home.GettingStartedOnboardingModal
import com.calisthenics.ai.ui.home.GettingStartedProgramBottomSheet
import com.calisthenics.ai.ui.home.HomeViewModel
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import com.calisthenics.ai.ui.today.TodayWorkoutSection

@Composable
fun HomeDashboard(
    onAnalysis: () -> Unit,
    onTraining: () -> Unit,
    onHistoryItem: (Long) -> Unit,
    onWorkoutDetails: (Long, WorkoutDayOfWeek) -> Unit,
    onStartWorkout: (Long, Long, String, Long?) -> Unit,
    onWorkoutHistory: () -> Unit,
    onResumeWorkout: (WorkoutSession) -> Unit,
    onAnalytics: (() -> Unit)? = null,
    onOpenBubble: (() -> Unit)? = null,
    onOpenAiPlanner: () -> Unit = {},
    onOpenWorkoutPlans: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    openProgramDetailsSignal: Boolean = false,
    onConsumeOpenProgramDetailsSignal: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUnreadNotificationCount()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(openProgramDetailsSignal) {
        if (openProgramDetailsSignal) {
            viewModel.openProgramDetails()
            onConsumeOpenProgramDetailsSignal()
        }
    }

    if (state.showProgramDetails) {
        GettingStartedProgramBottomSheet(
            program = state.publishedProgram,
            onDismiss = viewModel::closeProgramDetails,
            onEnroll = viewModel::requestEnroll,
            isEnrolling = state.isEnrolling,
            onStartDay = { dayNum -> viewModel.startDayWorkout(dayNum, onResumeWorkout) },
            isStartingDay = state.isStartingDaySession,
            enrollmentError = state.enrollmentError,
            onDismissError = viewModel::clearEnrollmentError,
            onExploreAiPlanner = onOpenAiPlanner,
            onCreateWorkoutPlan = onOpenWorkoutPlans
        )
    }

    com.calisthenics.ai.ui.home.GettingStartedEnrollmentDialogs(
        showEnrollChoiceDialog = state.showEnrollChoiceDialog,
        showConflictDialog = state.showConflictDialog,
        conflicts = state.pendingScheduleConflicts,
        isBusy = state.isEnrolling || state.isCheckingConflicts,
        onDismissChoice = viewModel::dismissEnrollChoiceDialog,
        onSelectAddToWorkoutPlan = viewModel::enrollAddToWorkoutPlan,
        onSelectJustStart = viewModel::enrollJustStart,
        onDismissConflict = viewModel::dismissConflictDialog,
        onConfirmConflictAdd = viewModel::confirmConflictEnrollment
    )

    if (state.shouldShowOnboardingModal) {
        GettingStartedOnboardingModal(
            program = state.publishedProgram,
            onViewProgram = {
                viewModel.dismissToCompactBanner()
                viewModel.openProgramDetails()
            },
            onDismiss = viewModel::dismissToCompactBanner
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 18.dp,
            bottom = FloatingBottomBarDefaults.ContentBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "home_top_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.loc_calisthenics_ai),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.analyze_form_ai),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                }
                HomeTopBarActions(
                    unreadCount = state.unreadNotificationCount,
                    unreadCountAvailable = state.unreadCountAvailable,
                    onNotificationsClick = onNotificationsClick,
                    onProfileClick = onProfileClick
                )
            }
        }
        if (state.shouldShowCompactBanner) {
            item(key = "getting_started_compact_banner") {
                GettingStartedCompactBanner(
                    program = state.publishedProgram,
                    onViewProgram = viewModel::openProgramDetails,
                    onDismiss = viewModel::permanentlyDismissOnboarding
                )
            }
        }
        item {
            TodayWorkoutSection(
                onViewDetails = onWorkoutDetails,
                onStartWorkout = onStartWorkout,
                onViewPlans = onTraining,
                onViewHistory = onWorkoutHistory,
                onResumeSession = onResumeWorkout,
                onOpenBubble = onOpenBubble
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DarkCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null)
                        Text(stringResource(R.string.ai_analysis), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.review_your_form), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onAnalysis) { Text(stringResource(R.string.common_start)) }
                    }
                }
                DarkCard(
                    Modifier
                        .weight(1f)
                        .then(if (onAnalytics != null) Modifier.clickable { onAnalytics() } else Modifier)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null)
                        Text(stringResource(R.string.analytics), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.track_overload_prs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { onAnalytics?.invoke() }) { Text(stringResource(R.string.common_view)) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.recent_activity), style = MaterialTheme.typography.titleLarge) }
        when {
            state.isLoadingHistory -> item { CircularProgressIndicator(Modifier.size(28.dp)) }
            state.error != null -> item {
                EmptyState(stringResource(R.string.history_unavailable), state.error ?: stringResource(R.string.could_not_load_history), stringResource(R.string.common_retry), viewModel::loadHistory)
            }
            state.history.isEmpty() -> item {
                EmptyState(stringResource(R.string.your_history_is_clear), stringResource(R.string.loc_complete_analysis_history))
            }
            else -> items(state.history.take(5), key = { it.id }) { result ->
                RecentAnalysisRow(result, onClick = { onHistoryItem(result.id) })
            }
        }
        item { Spacer(Modifier.size(12.dp)) }
    }
}

@Composable
private fun RecentAnalysisRow(result: AnalysisResult, onClick: () -> Unit) {
    DarkCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(result.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text(result.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            result.overallScore?.let { Text("${it.toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, body: String) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DarkCard(Modifier.fillMaxWidth()) {
            EmptyState(stringResource(R.string.not_connected_yet), stringResource(R.string.feature_ready_no_data))
        }
    }
}
