package com.calisthenics.ai.ui.workoutsession

import com.calisthenics.ai.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.calisthenics.ai.data.local.SetLoggingMode
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionExercise
import com.calisthenics.ai.domain.model.WorkoutSessionSet
import com.calisthenics.ai.domain.model.WorkoutSessionSetUpdate
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface
import com.calisthenics.ai.ui.workoutsession.timer.ActiveTimerOverlay
import com.calisthenics.ai.ui.workoutsession.timer.KeepScreenOn
import com.calisthenics.ai.ui.workoutsession.timer.TimerSettingsDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    onCancel: () -> Unit,
    onFinished: (Long) -> Unit,
    onViewHistory: () -> Unit = {},
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.workout_session)) },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.minimize_workout))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            when {
                state.isLoading -> Column(
                    Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.loc_restoring_workout_now), Modifier.padding(top = 12.dp))
                }
                state.session == null -> EmptyState(
                    title = stringResource(R.string.workout_unavailable),
                    body = state.error ?: stringResource(R.string.workout_unavailable),
                    action = stringResource(R.string.loc_try_again),
                    onAction = viewModel::retrySync
                )
                else -> SessionContent(
                    session = requireNotNull(state.session),
                    elapsedSeconds = state.elapsedSeconds,
                    isSaving = state.isSaving,
                    isFinishing = state.isFinishing,
                    hasUnsyncedChanges = state.hasUnsyncedChanges,
                    hasSyncConflict = state.hasSyncConflict,
                    savingSetIds = state.savingSetIds,
                    error = state.error,
                    loggingMode = state.loggingMode,
                    selectedExerciseIndex = state.selectedExerciseIndex,
                    selectedSetIndex = state.selectedSetIndex,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onUpdateSet = { exId, setId, update -> viewModel.updateSet(exId, setId, update) },
                    onCompleteSetAndAdvance = { exId, setId, update -> viewModel.completeSetAndAdvance(exId, setId, update) },
                    onSelectExercise = viewModel::selectExercise,
                    onSelectSet = viewModel::selectSet,
                    onNextSet = viewModel::nextSet,
                    onPrevSet = viewModel::prevSet,
                    onNextExercise = viewModel::nextExercise,
                    onPrevExercise = viewModel::prevExercise,
                    onAddSet = viewModel::addSet,
                    onDeleteSet = viewModel::deleteSet,
                    onSetLoggingMode = viewModel::setLoggingMode,
                    onToggleSkipped = viewModel::toggleExerciseSkipped,
                    onStartHoldTimer = viewModel::startHoldTimer,
                    onOpenTimerSettings = { ex -> viewModel.openTimerSettings(ex.exerciseName, ex.restSeconds) },
                    onFinish = viewModel::showFinishConfirmation,
                    onRetry = viewModel::retrySync,
                    onReapplyConflicts = viewModel::reapplyConflictedChanges,
                    onViewHistory = onViewHistory
                )
            }
        }

        if (state.activeTimer.isTimerActive) {
            KeepScreenOn()
            ActiveTimerOverlay(
                state = state.activeTimer,
                onStopHold = viewModel::stopHoldTimer,
                onSaveSet = { viewModel.saveSetFromTimer() },
                onRetryHold = viewModel::retryHoldTimer,
                onCancelHold = viewModel::cancelTimer,
                onPauseRest = viewModel::pauseRestTimer,
                onResumeRest = viewModel::resumeRestTimer,
                onAddRestSeconds = viewModel::addRestSeconds,
                onSkipRest = viewModel::skipRestTimer,
                onStartNextSet = viewModel::startNextSet
            )
        }

        if (state.isTimerSettingsDialogVisible) {
            TimerSettingsDialog(
                exerciseName = state.activeConfigExerciseName,
                initialConfig = state.activeTimerConfig,
                onDismiss = viewModel::dismissTimerSettings,
                onSave = viewModel::saveTimerConfig
            )
        }

        val session = state.session
        if (state.isFinishConfirmationVisible && session != null) {
            FinishWorkoutDialog(
                session = session,
                onDismiss = viewModel::dismissFinishConfirmation,
                onConfirm = { confirmPartial -> viewModel.finish(confirmPartial, onFinished) }
            )
        }
    }
}

@Composable
private fun SessionContent(
    session: WorkoutSession,
    elapsedSeconds: Long,
    isSaving: Boolean,
    isFinishing: Boolean,
    hasUnsyncedChanges: Boolean,
    hasSyncConflict: Boolean,
    savingSetIds: Set<Long>,
    error: String?,
    loggingMode: SetLoggingMode,
    selectedExerciseIndex: Int,
    selectedSetIndex: Int,
    modifier: Modifier,
    onUpdateSet: (Long, Long, WorkoutSessionSetUpdate) -> Unit,
    onCompleteSetAndAdvance: (Long, Long, WorkoutSessionSetUpdate) -> Unit,
    onSelectExercise: (Int) -> Unit,
    onSelectSet: (Int) -> Unit,
    onNextSet: () -> Unit,
    onPrevSet: () -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (Long, Long) -> Unit,
    onSetLoggingMode: (SetLoggingMode) -> Unit,
    onToggleSkipped: (WorkoutSessionExercise) -> Unit,
    onStartHoldTimer: (WorkoutSessionExercise, WorkoutSessionSet) -> Unit,
    onOpenTimerSettings: (WorkoutSessionExercise) -> Unit,
    onFinish: () -> Unit,
    onRetry: () -> Unit,
    onReapplyConflicts: () -> Unit,
    onViewHistory: () -> Unit
) {
    val exercises = session.exercises.sortedBy { it.orderIndex }
    val completedCount = exercises.count { it.status == "COMPLETED" }
    val progress = if (exercises.isEmpty()) 0f else completedCount.toFloat() / exercises.size
    val sessionIsActive = session.status == "IN_PROGRESS" && !isFinishing

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary & Session Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        modifier = Modifier.weight(1f)
                    )

                    // Mode Switcher Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = loggingMode == SetLoggingMode.STEP_BY_STEP,
                            onClick = { onSetLoggingMode(SetLoggingMode.STEP_BY_STEP) },
                            label = { Text("Step") },
                            leadingIcon = {
                                Icon(Icons.Default.FormatListNumbered, contentDescription = null, Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent,
                                selectedLabelColor = Ink
                            )
                        )
                        FilterChip(
                            selected = loggingMode == SetLoggingMode.SHOW_ALL,
                            onClick = { onSetLoggingMode(SetLoggingMode.SHOW_ALL) },
                            label = { Text("All") },
                            leadingIcon = {
                                Icon(Icons.Default.ViewAgenda, contentDescription = null, Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent,
                                selectedLabelColor = Ink
                            )
                        )
                    }
                }

                Text(
                    text = when (session.source) {
                        "COACH" -> stringResource(R.string.loc_source_coach_assignment)
                        "PERSONAL" -> stringResource(R.string.loc_source_personal_plan)
                        else -> stringResource(R.string.loc_source_legacy_workout)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Text(
                    text = session.workoutDate?.let { formatDate(it) } ?: stringResource(R.string.loc_workout_date_fallback),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Text(
                    text = stringResource(R.string.loc_elapsed_exercises, formatDuration(elapsedSeconds), completedCount, exercises.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Accent,
                    trackColor = Surface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
                    }
                    Text(
                        text = when {
                            hasUnsyncedChanges -> stringResource(R.string.loc_saved_wait_sync)
                            isSaving -> stringResource(R.string.loc_saving_workout)
                            else -> stringResource(R.string.loc_all_changes_saved)
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasUnsyncedChanges) MaterialTheme.colorScheme.error else SecondaryText
                    )
                }
            }
        }

        if (hasUnsyncedChanges) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(), enabled = !isSaving) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.retry_synchronization))
            }
        }

        if (hasSyncConflict) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.loc_sync_conflict), color = MaterialTheme.colorScheme.onErrorContainer)
                    OutlinedButton(onClick = onReapplyConflicts, enabled = !isSaving) {
                        Text(stringResource(R.string.reapply_local_changes))
                    }
                }
            }
        }

        if (loggingMode == SetLoggingMode.STEP_BY_STEP) {
            // STEP-BY-STEP SET LOGGING VIEW
            StepByStepLoggingContent(
                exercises = exercises,
                selectedExerciseIndex = selectedExerciseIndex,
                selectedSetIndex = selectedSetIndex,
                sessionIsActive = sessionIsActive,
                savingSetIds = savingSetIds,
                onSelectExercise = onSelectExercise,
                onSelectSet = onSelectSet,
                onNextSet = onNextSet,
                onPrevSet = onPrevSet,
                onNextExercise = onNextExercise,
                onPrevExercise = onPrevExercise,
                onCompleteSetAndAdvance = onCompleteSetAndAdvance,
                onUpdateSet = onUpdateSet,
                onAddSet = onAddSet,
                onDeleteSet = onDeleteSet,
                onToggleSkipped = onToggleSkipped,
                onStartHoldTimer = onStartHoldTimer,
                onOpenTimerSettings = onOpenTimerSettings
            )
        } else {
            // SHOW ALL SETS VIEW
            ShowAllSetsLoggingContent(
                exercises = exercises,
                sessionIsActive = sessionIsActive,
                savingSetIds = savingSetIds,
                onUpdateSet = onUpdateSet,
                onAddSet = onAddSet,
                onDeleteSet = onDeleteSet,
                onToggleSkipped = onToggleSkipped,
                onStartHoldTimer = onStartHoldTimer,
                onOpenTimerSettings = onOpenTimerSettings
            )
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            enabled = session.status == "IN_PROGRESS" && !isFinishing && !hasUnsyncedChanges,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isFinishing) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.finish_workout), fontWeight = FontWeight.Bold)
            }
        }

        TextButton(onClick = onViewHistory, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.workout_history), color = SecondaryText)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Step-By-Step Logging Content:
 * Guides the user focused on one set at a time with clear progression.
 */
@Composable
private fun StepByStepLoggingContent(
    exercises: List<WorkoutSessionExercise>,
    selectedExerciseIndex: Int,
    selectedSetIndex: Int,
    sessionIsActive: Boolean,
    savingSetIds: Set<Long>,
    onSelectExercise: (Int) -> Unit,
    onSelectSet: (Int) -> Unit,
    onNextSet: () -> Unit,
    onPrevSet: () -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onCompleteSetAndAdvance: (Long, Long, WorkoutSessionSetUpdate) -> Unit,
    onUpdateSet: (Long, Long, WorkoutSessionSetUpdate) -> Unit,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (Long, Long) -> Unit,
    onToggleSkipped: (WorkoutSessionExercise) -> Unit,
    onStartHoldTimer: (WorkoutSessionExercise, WorkoutSessionSet) -> Unit,
    onOpenTimerSettings: (WorkoutSessionExercise) -> Unit
) {
    if (exercises.isEmpty()) return
    val currentExercise = exercises.getOrNull(selectedExerciseIndex) ?: exercises.first()
    val sortedSets = currentExercise.sets.sortedBy { it.setNumber }
    val currentSet = sortedSets.getOrNull(selectedSetIndex) ?: sortedSets.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Exercise Selector Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exercises.forEachIndexed { index, ex ->
                val isSelected = index == selectedExerciseIndex
                val isCompleted = ex.status == "COMPLETED"
                val isSkipped = ex.status == "SKIPPED"
                Card(
                    modifier = Modifier
                        .clickable { onSelectExercise(index) },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Accent else Border
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Surface else Elevated
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${ex.exerciseName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) PrimaryText else SecondaryText
                        )
                        if (isCompleted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = Accent, modifier = Modifier.size(15.dp))
                        } else if (isSkipped) {
                            Text("Skipped", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                    }
                }
            }
        }

        // Current Exercise Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentExercise.exerciseName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = when (currentExercise.status) {
                                "COMPLETED" -> stringResource(R.string.loc_completed_status)
                                "SKIPPED" -> stringResource(R.string.loc_skipped_status)
                                "INCOMPLETE" -> stringResource(R.string.loc_unfinished_status)
                                else -> "Target: ${currentExercise.targetSets} sets" +
                                    (currentExercise.targetReps?.let { " × $it reps" } ?: "") +
                                    (currentExercise.targetHoldDurationSeconds?.let { " · ${it}s hold" } ?: "")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Accent
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onOpenTimerSettings(currentExercise) }) {
                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.timer_settings), tint = SecondaryText)
                        }
                        if (sessionIsActive && currentExercise.status != "COMPLETED") {
                            TextButton(onClick = { onToggleSkipped(currentExercise) }) {
                                Text(
                                    if (currentExercise.status == "SKIPPED") stringResource(R.string.loc_undo_skip)
                                    else stringResource(R.string.loc_skip),
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }

                currentExercise.instructions?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }
                currentExercise.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                }

                // Horizontal Set Stepper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sortedSets.forEachIndexed { setIdx, set ->
                        val isSetSelected = setIdx == selectedSetIndex
                        Card(
                            modifier = Modifier
                                .clickable { onSelectSet(setIdx) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSetSelected) 2.dp else 1.dp,
                                color = if (isSetSelected) Accent else if (set.completed) Accent.copy(alpha = 0.5f) else Border
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSetSelected) Accent.copy(alpha = 0.15f) else Surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (set.completed) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = "Set ${set.setNumber}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSetSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSetSelected) Accent else PrimaryText
                                )
                            }
                        }
                    }

                    if (sessionIsActive && currentExercise.status != "SKIPPED") {
                        IconButton(
                            onClick = { onAddSet(currentExercise.id) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Surface)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Set", tint = Accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Focused Single Set Card
        if (currentSet != null) {
            FocusedStepSetCard(
                exercise = currentExercise,
                set = currentSet,
                totalSetsCount = sortedSets.size,
                sessionIsActive = sessionIsActive,
                saving = currentSet.id in savingSetIds,
                canDelete = sortedSets.size > 1 && sessionIsActive,
                hasPrevious = selectedSetIndex > 0,
                hasNext = selectedSetIndex < sortedSets.size - 1,
                onCompleteAndAdvance = { update -> onCompleteSetAndAdvance(currentExercise.id, currentSet.id, update) },
                onUpdate = { update -> onUpdateSet(currentExercise.id, currentSet.id, update) },
                onDelete = { onDeleteSet(currentExercise.id, currentSet.id) },
                onPrevSet = onPrevSet,
                onNextSet = onNextSet,
                onStartHoldTimer = { onStartHoldTimer(currentExercise, currentSet) }
            )
        }

        // Bottom Navigation Across Exercises
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrevExercise,
                enabled = selectedExerciseIndex > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.previous_exercise))
            }

            TextButton(
                onClick = onNextExercise,
                enabled = selectedExerciseIndex < exercises.size - 1
            ) {
                Text(stringResource(R.string.next_exercise))
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Focused Single Set Card for Step-by-Step Mode.
 */
@Composable
private fun FocusedStepSetCard(
    exercise: WorkoutSessionExercise,
    set: WorkoutSessionSet,
    totalSetsCount: Int,
    sessionIsActive: Boolean,
    saving: Boolean,
    canDelete: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onCompleteAndAdvance: (WorkoutSessionSetUpdate) -> Unit,
    onUpdate: (WorkoutSessionSetUpdate) -> Unit,
    onDelete: () -> Unit,
    onPrevSet: () -> Unit,
    onNextSet: () -> Unit,
    onStartHoldTimer: () -> Unit
) {
    var repsText by remember(set.id) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var holdText by remember(set.id) { mutableStateOf(set.holdDurationSeconds?.toString().orEmpty()) }
    var weightText by remember(set.id) { mutableStateOf(set.weightKg?.toString().orEmpty()) }
    var assistanceText by remember(set.id) { mutableStateOf(set.assistanceWeightKg?.toString().orEmpty()) }
    var notesText by remember(set.id) { mutableStateOf(set.notes.orEmpty()) }

    val showReps = exercise.targetReps != null || set.reps != null || exercise.targetHoldDurationSeconds == null
    val showHold = exercise.targetHoldDurationSeconds != null || set.holdDurationSeconds != null
    val requireReps = exercise.targetReps != null
    val requireHold = exercise.targetHoldDurationSeconds != null

    val invalid = (repsText.isNotBlank() && repsText.toIntOrNull()?.let { it in 0..10_000 } != true) ||
        (holdText.isNotBlank() && holdText.toIntOrNull()?.let { it in 0..86_400 } != true) ||
        (weightText.isNotBlank() && weightText.toDoubleOrNull()?.let { it in 0.0..2000.0 } != true) ||
        (assistanceText.isNotBlank() && assistanceText.toDoubleOrNull()?.let { it in 0.0..2000.0 } != true)

    val missingRequiredMetric = (requireReps && repsText.toIntOrNull() == null) ||
        (requireHold && holdText.toIntOrNull() == null)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (set.completed) Accent else Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Set X of Y + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.set_progress_n_of_m, set.setNumber, totalSetsCount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
                    }
                    if (set.completed) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Accent
                            )
                        }
                    }
                }
            }

            // Metric inputs
            if (showReps || showHold) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showReps) {
                        MetricField("Reps", repsText, KeyboardType.Number, !sessionIsActive, Modifier.weight(1f)) { value ->
                            repsText = value
                            if (value.isBlank() || value.toIntOrNull()?.let { it in 0..10_000 } == true) {
                                onUpdate(createSetUpdate(value, holdText, weightText, assistanceText, notesText, set.completed))
                            }
                        }
                    }
                    if (showHold) {
                        MetricField(stringResource(R.string.loc_hold_seconds), holdText, KeyboardType.Number, !sessionIsActive, Modifier.weight(1f)) { value ->
                            holdText = value
                            if (value.isBlank() || value.toIntOrNull()?.let { it in 0..86_400 } == true) {
                                onUpdate(createSetUpdate(repsText, value, weightText, assistanceText, notesText, set.completed))
                            }
                        }
                        if (sessionIsActive && !set.completed) {
                            OutlinedButton(
                                onClick = onStartHoldTimer,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.timer))
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricField(stringResource(R.string.loc_added_kg), weightText, KeyboardType.Decimal, !sessionIsActive, Modifier.weight(1f)) { value ->
                    weightText = value
                    if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..2000.0 } == true) {
                        onUpdate(createSetUpdate(repsText, holdText, value, assistanceText, notesText, set.completed))
                    }
                }
                MetricField(stringResource(R.string.loc_assist_kg), assistanceText, KeyboardType.Decimal, !sessionIsActive, Modifier.weight(1f)) { value ->
                    assistanceText = value
                    if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..2000.0 } == true) {
                        onUpdate(createSetUpdate(repsText, holdText, weightText, value, notesText, set.completed))
                    }
                }
            }

            OutlinedTextField(
                value = notesText,
                onValueChange = { value ->
                    notesText = value.take(2000)
                    onUpdate(createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, set.completed))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.set_notes)) },
                enabled = sessionIsActive,
                singleLine = true
            )

            if (invalid || (missingRequiredMetric && !set.completed)) {
                Text(stringResource(R.string.loc_enter_allowed_numbers), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // PRIMARY ACTION BUTTON: Complete Set X or Update Set X
            if (sessionIsActive) {
                if (!set.completed) {
                    Button(
                        onClick = {
                            val update = createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, true)
                            onCompleteAndAdvance(update)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !invalid && !missingRequiredMetric,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.complete_set_n, set.setNumber),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val update = createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, true)
                                onUpdate(update)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = PrimaryText),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.update_set_n, set.setNumber), fontWeight = FontWeight.SemiBold)
                        }

                        TextButton(
                            onClick = {
                                val update = createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, false)
                                onUpdate(update)
                            }
                        ) {
                            Text("Mark Incomplete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Set Navigation & Delete controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPrevSet,
                    enabled = hasPrevious
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.previous_set))
                }

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.remove_set),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                TextButton(
                    onClick = onNextSet,
                    enabled = hasNext
                ) {
                    Text(stringResource(R.string.next_set))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Show All Sets Logging Content:
 * Displays all exercises and all sets simultaneously in a logbook layout.
 */
@Composable
private fun ShowAllSetsLoggingContent(
    exercises: List<WorkoutSessionExercise>,
    sessionIsActive: Boolean,
    savingSetIds: Set<Long>,
    onUpdateSet: (Long, Long, WorkoutSessionSetUpdate) -> Unit,
    onAddSet: (Long) -> Unit,
    onDeleteSet: (Long, Long) -> Unit,
    onToggleSkipped: (WorkoutSessionExercise) -> Unit,
    onStartHoldTimer: (WorkoutSessionExercise, WorkoutSessionSet) -> Unit,
    onOpenTimerSettings: (WorkoutSessionExercise) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        exercises.forEach { exercise ->
            SessionExerciseCard(
                exercise = exercise,
                sessionIsActive = sessionIsActive,
                savingSetIds = savingSetIds,
                onUpdateSet = { setId, update -> onUpdateSet(exercise.id, setId, update) },
                onAddSet = { onAddSet(exercise.id) },
                onDeleteSet = { setId -> onDeleteSet(exercise.id, setId) },
                onToggleSkipped = { onToggleSkipped(exercise) },
                onStartHoldTimer = { set -> onStartHoldTimer(exercise, set) },
                onOpenTimerSettings = { onOpenTimerSettings(exercise) }
            )
        }
    }
}

@Composable
private fun SessionExerciseCard(
    exercise: WorkoutSessionExercise,
    sessionIsActive: Boolean,
    savingSetIds: Set<Long>,
    onUpdateSet: (Long, WorkoutSessionSetUpdate) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (Long) -> Unit,
    onToggleSkipped: () -> Unit,
    onStartHoldTimer: (WorkoutSessionSet) -> Unit,
    onOpenTimerSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryText)
                    Text(
                        when (exercise.status) {
                            "COMPLETED" -> stringResource(R.string.loc_completed_status)
                            "SKIPPED" -> stringResource(R.string.loc_skipped_status)
                            "INCOMPLETE" -> stringResource(R.string.loc_unfinished_status)
                            else -> "Target: ${exercise.targetSets} sets" +
                                (exercise.targetReps?.let { " × $it reps" } ?: "") +
                                (exercise.targetHoldDurationSeconds?.let { " · ${it}s hold" } ?: "")
                        },
                        color = Accent
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenTimerSettings) {
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.timer_settings), tint = SecondaryText)
                    }
                    if (sessionIsActive && exercise.status != "COMPLETED") {
                        TextButton(onClick = onToggleSkipped) {
                            Text(
                                if (exercise.status == "SKIPPED") stringResource(R.string.loc_undo_skip)
                                else stringResource(R.string.loc_skip),
                                color = SecondaryText
                            )
                        }
                    }
                }
            }
            exercise.instructions?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
            exercise.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }

            exercise.sets.sortedBy { it.setNumber }.forEach { set ->
                SessionSetRow(
                    set = set,
                    disabled = !sessionIsActive || exercise.status == "SKIPPED",
                    showReps = exercise.targetReps != null || set.reps != null ||
                        exercise.targetHoldDurationSeconds == null,
                    showHold = exercise.targetHoldDurationSeconds != null || set.holdDurationSeconds != null,
                    requireReps = exercise.targetReps != null,
                    requireHold = exercise.targetHoldDurationSeconds != null,
                    saving = set.id in savingSetIds,
                    canDelete = exercise.sets.size > 1 && sessionIsActive,
                    onUpdate = { onUpdateSet(set.id, it) },
                    onDelete = { onDeleteSet(set.id) },
                    onStartHoldTimer = { onStartHoldTimer(set) }
                )
            }

            if (sessionIsActive && exercise.status != "SKIPPED") {
                OutlinedButton(
                    onClick = onAddSet,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_set))
                }
            }
        }
    }
}

@Composable
private fun SessionSetRow(
    set: WorkoutSessionSet,
    disabled: Boolean,
    showReps: Boolean,
    showHold: Boolean,
    requireReps: Boolean,
    requireHold: Boolean,
    saving: Boolean,
    canDelete: Boolean = false,
    onUpdate: (WorkoutSessionSetUpdate) -> Unit,
    onDelete: () -> Unit = {},
    onStartHoldTimer: () -> Unit
) {
    var repsText by remember(set.id) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var holdText by remember(set.id) { mutableStateOf(set.holdDurationSeconds?.toString().orEmpty()) }
    var weightText by remember(set.id) { mutableStateOf(set.weightKg?.toString().orEmpty()) }
    var assistanceText by remember(set.id) { mutableStateOf(set.assistanceWeightKg?.toString().orEmpty()) }
    var notesText by remember(set.id) { mutableStateOf(set.notes.orEmpty()) }
    val invalid = (repsText.isNotBlank() && repsText.toIntOrNull()?.let { it in 0..10_000 } != true) ||
        (holdText.isNotBlank() && holdText.toIntOrNull()?.let { it in 0..86_400 } != true) ||
        (weightText.isNotBlank() && weightText.toDoubleOrNull()?.let { it in 0.0..2000.0 } != true) ||
        (assistanceText.isNotBlank() && assistanceText.toDoubleOrNull()?.let { it in 0.0..2000.0 } != true)
    val missingRequiredMetric = (requireReps && repsText.toIntOrNull() == null) ||
        (requireHold && holdText.toIntOrNull() == null)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.loc_set_number, set.setNumber), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = PrimaryText)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Set", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
                Checkbox(
                    checked = set.completed,
                    enabled = !disabled && !invalid && (!missingRequiredMetric || set.completed),
                    onCheckedChange = { complete ->
                        onUpdate(createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, complete))
                    }
                )
                Text(stringResource(R.string.common_done), color = PrimaryText)
            }
        }
        if (showReps || showHold) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showReps) {
                    MetricField("Reps", repsText, KeyboardType.Number, disabled, Modifier.weight(1f)) { value ->
                        repsText = value
                        if (value.isBlank() || value.toIntOrNull()?.let { it in 0..10_000 } == true) {
                            onUpdate(createSetUpdate(value, holdText, weightText, assistanceText, notesText, set.completed))
                        }
                    }
                }
                if (showHold) {
                    MetricField(stringResource(R.string.loc_hold_seconds), holdText, KeyboardType.Number, disabled, Modifier.weight(1f)) { value ->
                        holdText = value
                        if (value.isBlank() || value.toIntOrNull()?.let { it in 0..86_400 } == true) {
                            onUpdate(createSetUpdate(repsText, value, weightText, assistanceText, notesText, set.completed))
                        }
                    }
                    if (!disabled && !set.completed) {
                        OutlinedButton(
                            onClick = onStartHoldTimer,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.timer))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricField(stringResource(R.string.loc_added_kg), weightText, KeyboardType.Decimal, disabled, Modifier.weight(1f)) { value ->
                weightText = value
                if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..2000.0 } == true) {
                    onUpdate(createSetUpdate(repsText, holdText, value, assistanceText, notesText, set.completed))
                }
            }
            MetricField(stringResource(R.string.loc_assist_kg), assistanceText, KeyboardType.Decimal, disabled, Modifier.weight(1f)) { value ->
                assistanceText = value
                if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..2000.0 } == true) {
                    onUpdate(createSetUpdate(repsText, holdText, weightText, value, notesText, set.completed))
                }
            }
        }
        if (invalid || (missingRequiredMetric && !set.completed)) {
            Text(stringResource(R.string.loc_enter_allowed_numbers), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = notesText,
            onValueChange = { value ->
                notesText = value.take(2000)
                onUpdate(createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, set.completed))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.set_notes)) },
            enabled = !disabled,
            singleLine = true
        )
    }
}

@Composable
private fun MetricField(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    disabled: Boolean,
    modifier: Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(8)) },
        modifier = modifier,
        label = { Text(label) },
        enabled = !disabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

private fun createSetUpdate(
    reps: String,
    hold: String,
    weight: String,
    assistance: String,
    notes: String,
    completed: Boolean
) = WorkoutSessionSetUpdate(
    reps = reps.toIntOrNull(),
    holdDurationSeconds = hold.toIntOrNull(),
    weightKg = weight.toDoubleOrNull(),
    assistanceWeightKg = assistance.toDoubleOrNull(),
    completed = completed,
    notes = notes.takeIf { it.isNotBlank() }
)

@Composable
private fun FinishWorkoutDialog(
    session: WorkoutSession,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    val unfinished = session.unfinishedExercises > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finish_workout_question)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(stringResource(R.string.loc_workout_finish_summary, session.completedExercises, session.skippedExercises, session.unfinishedExercises))
                Text(stringResource(R.string.loc_sets_finish_summary, session.completedSets, session.totalReps, session.totalHoldDurationSeconds))
                if (unfinished) Text(stringResource(R.string.unfinished_exercises_incomplete))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(unfinished) }) {
                Text(if (unfinished) stringResource(R.string.loc_finish_partial) else stringResource(R.string.loc_finish))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.keep_training)) } }
    )
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainder)
    else "%02d:%02d".format(minutes, remainder)
}

private fun formatDate(date: String): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
}.getOrDefault(date)
