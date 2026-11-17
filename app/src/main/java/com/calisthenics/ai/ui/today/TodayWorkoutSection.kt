package com.calisthenics.ai.ui.today

import com.calisthenics.ai.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSource
import com.calisthenics.ai.ui.media.ExerciseMediaBorder
import com.calisthenics.ai.ui.media.ExerciseMediaPreview
import com.calisthenics.ai.ui.media.ExerciseStoryViewer
import com.calisthenics.ai.ui.home.preferredMedia
import com.calisthenics.ai.ui.home.resolveWorkoutMedia
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

@Composable
fun TodayWorkoutSection(
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit,
    onStartWorkout: (Long, Long, String, Long?) -> Unit,
    onViewPlans: () -> Unit,
    onViewHistory: () -> Unit,
    onResumeSession: (WorkoutSession) -> Unit,
    onOpenBubble: (() -> Unit)? = null,
    viewModel: TodayWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onAppResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.todays_workout), style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.loc_your_training_plan_today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.loc_refresh_todays_workout))
            }
        }

        when {
            state.isLoading -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.loading_todays_workout), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.error != null -> MessageCard(
                title = stringResource(R.string.workout_unavailable),
                body = state.error ?: stringResource(R.string.could_not_load_today),
                action = stringResource(R.string.common_retry),
                onAction = viewModel::refresh
            )
            state.selection.kind == TodayWorkoutKind.NO_PLANS -> if (
                state.activeSessions.none { it.source == "GETTING_STARTED" } &&
                state.upcomingSessions.none { it.source == "GETTING_STARTED" } &&
                state.activeGettingStartedProgram?.enrollment?.addedToWorkoutPlan != true
            ) {
                MessageCard(
                    title = if (!state.isOnline) stringResource(R.string.no_cached_workout) else stringResource(R.string.no_workout_plan_yet_period),
                    body = if (!state.isOnline)
                        stringResource(R.string.no_cached_schedule)
                    else
                        stringResource(R.string.create_plan_schedule),
                    action = if (!state.isOnline) stringResource(R.string.common_retry) else stringResource(R.string.view_workout_plans),
                    onAction = if (!state.isOnline) viewModel::refresh else onViewPlans
                )
            }
            state.selection.kind == TodayWorkoutKind.PLAN_SELECTION_REQUIRED -> PlanChoiceCard(
                plans = state.plans,
                source = state.selection.source,
                onSelect = viewModel::selectPlan
            )
            else -> {
                state.selection.plan?.let { plan ->
                    if (state.plans.size > 1) {
                        ActivePlanPicker(
                            selectedPlan = plan,
                            plans = state.plans,
                            onSelect = viewModel::selectPlan
                        )
                    }
                }
                when (state.selection.kind) {
                    TodayWorkoutKind.REST_DAY -> RestDayCard(
                        plan = requireNotNull(state.selection.plan),
                        day = requireNotNull(state.selection.day),
                        source = state.selection.source,
                        coachName = state.selection.coachName,
                        onViewDetails = onViewDetails
                    )
                    TodayWorkoutKind.NOT_SCHEDULED -> NotScheduledCard(
                        plan = requireNotNull(state.selection.plan),
                        dayOfWeek = state.date.dayOfWeek.value.let(WorkoutDayOfWeek::fromOrder),
                        onViewDetails = onViewDetails
                    )
                    TodayWorkoutKind.SCHEDULED -> {
                        val plan = requireNotNull(state.selection.plan)
                        val day = requireNotNull(state.selection.day)
                        ScheduledWorkoutCard(
                            plan = plan,
                            day = day,
                            date = state.date.toString(),
                            source = state.selection.source,
                            coachName = state.selection.coachName,
                            assignmentId = state.selection.assignmentId,
                            onViewDetails = onViewDetails,
                            onStartWorkout = onStartWorkout
                        )
                    }
                    else -> Unit
                }
            }
        }
        val gettingStartedActiveSessions = state.activeSessions.filter {
            it.source == "GETTING_STARTED" && it.status != "COMPLETED"
        }
        val otherActiveSessions = state.activeSessions.filter { it.source != "GETTING_STARTED" }
        if (gettingStartedActiveSessions.isNotEmpty()) {
            GettingStartedTodaySessionsCard(
                sessions = gettingStartedActiveSessions,
                onStartOrResumeSession = onResumeSession
            )
        }
        if (state.isBubbleEnabled && otherActiveSessions.isNotEmpty()) {
            ActiveWorkoutRecoveryCard(
                sessions = otherActiveSessions,
                onResumeSession = onResumeSession,
                onOpenBubble = onOpenBubble ?: {
                    val opened = viewModel.openWorkoutBubble()
                    if (!opened) {
                        if (!state.isBubbleEnabled) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.bubble_feature_disabled),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else if (otherActiveSessions.isEmpty()) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.no_unfinished_workouts),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        } else if (state.isBubbleEnabled && state.activeSessionsError != null) {
            Text(
                state.activeSessionsError ?: stringResource(R.string.unfinished_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.upcomingSessions.isNotEmpty()) {
            UpcomingWorkoutsCard(state.upcomingSessions.take(4))
        }
        TextButton(onClick = onViewHistory) { Text(stringResource(R.string.workout_history)) }

        when {
            state.selection.plan != null -> {
                val plan = requireNotNull(state.selection.plan)
                WeeklyWorkoutMediaSection(
                    plan = plan,
                    today = state.date,
                    onDayClick = { day -> onViewDetails(plan.id, day) }
                )
            }
            state.isLoading -> WeeklyScheduleEmptySection(state.date, stringResource(R.string.loading_weekly_schedule))
            state.error != null -> WeeklyScheduleEmptySection(state.date, stringResource(R.string.schedule_unavailable_retry))
            state.selection.kind == TodayWorkoutKind.PLAN_SELECTION_REQUIRED ->
                WeeklyScheduleEmptySection(state.date, stringResource(R.string.choose_active_plan_schedule))
            else -> WeeklyScheduleEmptySection(
                state.date,
                if (!state.isOnline)
                    stringResource(R.string.no_cached_schedule)
                else
                    stringResource(R.string.create_plan_weekly_schedule)
            )
        }
    }
}

@Composable
private fun ActiveWorkoutRecoveryCard(
    sessions: List<WorkoutSession>,
    onResumeSession: (WorkoutSession) -> Unit,
    onOpenBubble: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.unfinished_workouts),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryText
                    )
                }
            }

            Text(
                text = stringResource(R.string.loc_choose_session_resume),
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )

            sessions.forEach { session ->
                val totalSets = session.exercises.sumOf { it.sets.size }
                val completedSets = session.exercises.sumOf { ex -> ex.sets.count { it.completed } }
                val progress = if (totalSets > 0) completedSets.toFloat() / totalSets else 0f
                val currentExercise = session.exercises.firstOrNull { ex ->
                    ex.status != "COMPLETED" && ex.status != "SKIPPED"
                }?.exerciseName ?: session.exercises.firstOrNull()?.exerciseName.orEmpty()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentExercise.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentExercise,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (totalSets > 0) {
                            Text(
                                text = "$completedSets/$totalSets ${stringResource(R.string.loc_sets_label)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryText
                            )
                        }
                    }

                    if (totalSets > 0) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Accent,
                            trackColor = Border,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onOpenBubble,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryText
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BubbleChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Accent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.open_bubble),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Button(
                            onClick = { onResumeSession(session) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Ink
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (session.status == "SCHEDULED") {
                                    stringResource(R.string.start_workout)
                                } else {
                                    stringResource(R.string.resume)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanChoiceCard(
    plans: List<UserWorkoutPlan>,
    source: WorkoutSource,
    onSelect: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (source == WorkoutSource.COACH) stringResource(R.string.choose_coach_plan) else stringResource(R.string.choose_personal_plan),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.loc_multiple_plans_today),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            plans.forEach { plan ->
                OutlinedButton(onClick = { onSelect(plan.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(plan.name)
                }
            }
        }
    }
}

@Composable
private fun ActivePlanPicker(
    selectedPlan: UserWorkoutPlan,
    plans: List<UserWorkoutPlan>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.active_plan), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(4.dp))
        TextButton(onClick = { expanded = true }) { Text(selectedPlan.name) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = { Text(plan.name) },
                    onClick = {
                        expanded = false
                        if (plan.id != selectedPlan.id) onSelect(plan.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun RestDayCard(
    plan: UserWorkoutPlan,
    day: UserWorkoutPlanDay,
    source: WorkoutSource,
    coachName: String?,
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.rest_day), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.recovery_is_progress), style = MaterialTheme.typography.titleLarge)
            Text(
                sourceLabel(source, coachName),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Text(stringResource(R.string.no_exercises_today), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onViewDetails(plan.id, day.dayOfWeek) }) { Text(stringResource(R.string.view_workout_details)) }
        }
    }
}

@Composable
private fun NotScheduledCard(
    plan: UserWorkoutPlan,
    dayOfWeek: WorkoutDayOfWeek,
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit
) {
    MessageCard(
        title = stringResource(R.string.loc_nothing_scheduled_today),
        body = stringResource(R.string.not_configured_day, plan.name),
        action = stringResource(R.string.view_weekly_schedule),
        onAction = { onViewDetails(plan.id, dayOfWeek) }
    )
}

@Composable
private fun ScheduledWorkoutCard(
    plan: UserWorkoutPlan,
    day: UserWorkoutPlanDay,
    date: String,
    source: WorkoutSource,
    coachName: String?,
    assignmentId: Long?,
    onViewDetails: (Long, WorkoutDayOfWeek) -> Unit,
    onStartWorkout: (Long, Long, String, Long?) -> Unit
) {
    val exercises = day.exercises.sortedBy { it.displayOrder }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.today_focus), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(day.sessionType.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
                sourceLabel(source, coachName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${plan.name} · ${plan.splitType.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${pluralStringResource(R.plurals.exercise_count, exercises.size)} · ${stringResource(R.string.scheduled)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onViewDetails(plan.id, day.dayOfWeek) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.view_workout_details))
                }
                OutlinedButton(
                    onClick = { onStartWorkout(plan.id, day.id, date, assignmentId) },
                    enabled = exercises.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.start_workout))
                }
            }
        }
    }
    if (exercises.isEmpty()) {
        MessageCard(
            title = stringResource(R.string.no_exercises_assigned),
            body = stringResource(R.string.scheduled_empty_workout),
            action = null,
            onAction = {}
        )
    } else {
        Text(stringResource(R.string.exercises), style = MaterialTheme.typography.titleLarge)
        exercises.forEach { exercise ->
            ExerciseCard(exercise)
        }
    }
}

@Composable
private fun sourceLabel(source: WorkoutSource, coachName: String?): String =
    when (source) {
        WorkoutSource.COACH -> coachName?.let { stringResource(R.string.assigned_by_coach, it) } ?: stringResource(R.string.assigned_by_your_coach)
        WorkoutSource.PERSONAL -> stringResource(R.string.from_personal_plan)
        WorkoutSource.NONE -> stringResource(R.string.no_workout_source)
    }

@Composable
private fun WeeklyWorkoutMediaSection(
    plan: UserWorkoutPlan,
    today: LocalDate,
    onDayClick: (WorkoutDayOfWeek) -> Unit
) {
    val days = WorkoutDayOfWeek.entries
    var selectedDay by rememberSaveable { mutableStateOf(WorkoutDayOfWeek.fromOrder(today.dayOfWeek.value)) }
    val selectedWorkout = plan.days.firstOrNull { it.dayOfWeek == selectedDay }
    val categoryFallback = plan.days
        .asSequence()
        .filter { it.dayOfWeek != selectedDay && it.sessionType == selectedWorkout?.sessionType }
        .flatMap { it.exercises.asSequence() }
        .flatMap { it.exercise.mediaItems.asSequence() }
        .toList()
        .preferredMedia()
    val globalFallback = com.calisthenics.ai.domain.model.ExerciseMedia(
        type = com.calisthenics.ai.domain.model.ExerciseMediaType.IMAGE,
        url = "android.resource://com.calisthenics.ai/drawable/ic_launcher_foreground"
    )
    val resolved = selectedWorkout?.let { resolveWorkoutMedia(it, categoryFallback, globalFallback) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.weekly_workouts), style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { day ->
                val isSelected = day == selectedDay
                val isToday = day == WorkoutDayOfWeek.fromOrder(today.dayOfWeek.value)
                Surface(
                    modifier = Modifier
                        .then(
                            if (isToday) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { selectedDay = day },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            day.shortName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (selectedWorkout == null || selectedWorkout.isRestDay) {
            MessageCard(
                title = if (selectedWorkout?.isRestDay == true) stringResource(R.string.recovery_day) else stringResource(R.string.no_workout_scheduled),
                body = stringResource(R.string.no_training_session_day, selectedDay.displayName),
                action = null,
                onAction = {}
            )
        } else {
            WeeklyWorkoutMediaCard(
                day = selectedWorkout,
                media = resolved?.media,
                fallbackMedia = resolved?.fallbackMedia,
                globalFallback = resolved?.globalFallback,
                onClick = { onDayClick(selectedDay) }
            )
        }
    }
}

@Composable
private fun WeeklyWorkoutMediaCard(
    day: UserWorkoutPlanDay,
    media: com.calisthenics.ai.domain.model.ExerciseMedia?,
    fallbackMedia: com.calisthenics.ai.domain.model.ExerciseMedia?,
    globalFallback: com.calisthenics.ai.domain.model.ExerciseMedia?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var failedMediaUrls by remember(media?.url, fallbackMedia?.url) { mutableStateOf(emptySet<String>()) }
    var isFavorite by rememberSaveable(day.id) { mutableStateOf(false) }
    val selectedMedia = listOfNotNull(media, fallbackMedia, globalFallback)
        .firstOrNull { it.url !in failedMediaUrls }
    val posterUrl = selectedMedia?.thumbnailUrl
        ?: listOfNotNull(fallbackMedia, globalFallback).firstOrNull()?.url
    val player = remember(selectedMedia?.url) {
        selectedMedia
            ?.takeIf { it.type == com.calisthenics.ai.domain.model.ExerciseMediaType.VIDEO }
            ?.let {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(it.url))
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0f
                    prepare()
                    playWhenReady = true
                }
            }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(player, lifecycleOwner) {
        if (player == null) {
            onDispose { }
        } else {
            var shouldResume = true
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> if (shouldResume) player.play()
                    Lifecycle.Event.ON_STOP -> {
                        shouldResume = player.isPlaying || player.playWhenReady
                        player.pause()
                    }
                    else -> Unit
                }
            }
            val listener = object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    failedMediaUrls = failedMediaUrls + (selectedMedia?.url ?: return)
                }
            }
            player.addListener(listener)
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                player.removeListener(listener)
                lifecycleOwner.lifecycle.removeObserver(observer)
                player.release()
            }
        }
    }
    val difficulty = day.exercises
        .map { it.exercise.difficulty.name.lowercase().replaceFirstChar(Char::uppercaseChar) }
        .distinct()
        .joinToString(" / ")
        .takeIf(String::isNotBlank)
    val durationSeconds = day.exercises.sumOf { (it.durationSeconds ?: 0) * it.sets }
    val exercisesLabel = stringResource(R.string.exercises)
    val minutesLabel = stringResource(R.string.minutes_short, durationSeconds / 60)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight()) {
            if (posterUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(posterUrl).crossfade(true).build(),
                    contentDescription = day.name ?: day.sessionType.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        failedMediaUrls = failedMediaUrls + posterUrl
                    }
                )
            }
            if (player != null) {
                val exoPlayer = player
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            this.player = exoPlayer
                        }
                    },
                    update = { it.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            IconButton(
                onClick = { isFavorite = !isFavorite },
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                )
            }
            Column(
                Modifier.align(Alignment.BottomStart).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    day.name ?: day.sessionType.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    buildString {
                        append(day.sessionType.displayName)
                        append(" · ")
                        append(day.exercises.size)
                        append(" ")
                        append(exercisesLabel)
                        difficulty?.let {
                            append(" · ")
                            append(it)
                        }
                        if (durationSeconds > 0) {
                            append(" · ")
                            append(minutesLabel)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f)
                )
                Text(
                    stringResource(R.string.loc_scheduled_workout),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun GettingStartedTodaySessionsCard(
    sessions: List<WorkoutSession>,
    onStartOrResumeSession: (WorkoutSession) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = stringResource(R.string.getting_started_short_badge),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.getting_started_temporary_badge),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            sessions.forEach { session ->
                val isScheduled = session.status == "SCHEDULED"
                val totalSets = session.exercises.sumOf { it.sets.size }
                val completedSets = session.exercises.sumOf { ex -> ex.sets.count { it.completed } }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = buildString {
                                        if (!session.workoutDate.isNullOrBlank()) {
                                            append(session.workoutDate)
                                            append(" · ")
                                        }
                                        append(pluralStringResource(R.plurals.exercise_count, session.exercises.size))
                                        if (!isScheduled && totalSets > 0) {
                                            append(" · $completedSets/$totalSets")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isScheduled) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                                }
                            ) {
                                Text(
                                    text = stringResource(
                                        if (isScheduled) R.string.scheduled else R.string.getting_started_in_progress_badge
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isScheduled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Button(
                            onClick = { onStartOrResumeSession(session) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (isScheduled) R.string.start_workout else R.string.resume
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingWorkoutsCard(sessions: List<WorkoutSession>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.upcoming_workouts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            sessions.forEach { session ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (session.source == "GETTING_STARTED") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        stringResource(R.string.getting_started_short_badge),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        stringResource(R.string.getting_started_temporary_badge),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Text(
                            session.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            session.workoutDate.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            stringResource(R.string.upcoming_status),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyScheduleEmptySection(today: LocalDate, message: String) {
    val weekStart = datesForWeek(today).first()
    val weekEnd = weekStart.plusDays(6)
    val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val range = if (weekStart.year == weekEnd.year) {
        "${weekStart.format(shortDateFormatter)} – ${weekEnd.format(shortDateFormatter)}"
    } else {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
        "${weekStart.format(formatter)} – ${weekEnd.format(formatter)}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.this_week), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.weekly_schedule), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(range, style = MaterialTheme.typography.titleSmall)
        MessageCard(
            title = stringResource(R.string.weekly_schedule_not_shown),
            body = message,
            action = null,
            onAction = {}
        )
    }
}

@Composable
private fun WeeklyWorkoutDayCard(
    date: LocalDate,
    day: UserWorkoutPlanDay?,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val dayLabel = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(Locale.getDefault())
    val title = when {
        day == null -> stringResource(R.string.nothing_scheduled)
        day.sessionType.isRest -> stringResource(R.string.rest_day_title)
        else -> day.sessionType.displayName
    }
    val subtitle = when {
        day == null -> stringResource(R.string.not_configured_plan)
        day.sessionType.isRest -> stringResource(R.string.recovery_day)
        else -> pluralStringResource(R.plurals.exercise_count, day.exercises.size)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isToday) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ) {
                    Text(
                        stringResource(R.string.loc_today_upper),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: UserWorkoutPlanExercise) {
    var storyStartIndex by rememberSaveable(exercise.id) { mutableStateOf<Int?>(null) }

    ExerciseMediaBorder(
        hasMedia = exercise.exercise.mediaItems.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(exercise.prescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.rest_label, exercise.restSeconds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        exercise.notes?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (exercise.exercise.mediaItems.isNotEmpty()) {
                    ExerciseMediaPreview(
                        mediaItems = exercise.exercise.mediaItems,
                        onMediaClick = { storyStartIndex = it }
                    )
                }
            }
        }
    }

    storyStartIndex?.let { index ->
        ExerciseStoryViewer(
            mediaItems = exercise.exercise.mediaItems,
            initialIndex = index,
            onDismiss = { storyStartIndex = null }
        )
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}
