package com.calisthenics.ai.ui.workoutsession

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionExercise
import com.calisthenics.ai.domain.model.WorkoutSessionSet
import com.calisthenics.ai.domain.model.WorkoutSessionSetUpdate
import com.calisthenics.ai.ui.components.EmptyState
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
                    title = { Text("Workout session") },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Minimize workout")
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
                    Text("Restoring your workout…", Modifier.padding(top = 12.dp))
                }
                state.session == null -> EmptyState(
                    title = "Workout unavailable",
                    body = state.error ?: "This workout could not be loaded.",
                    action = "Try again",
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
                    modifier = Modifier.fillMaxSize().padding(padding),
                    onUpdateSet = { exId, setId, update -> viewModel.updateSet(exId, setId, update) },
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
    modifier: Modifier,
    onUpdateSet: (Long, Long, WorkoutSessionSetUpdate) -> Unit,
    onToggleSkipped: (WorkoutSessionExercise) -> Unit,
    onStartHoldTimer: (WorkoutSessionExercise, WorkoutSessionSet) -> Unit,
    onOpenTimerSettings: (WorkoutSessionExercise) -> Unit,
    onFinish: () -> Unit,
    onRetry: () -> Unit,
    onReapplyConflicts: () -> Unit,
    onViewHistory: () -> Unit
) {
    val exercises = session.exercises.sortedBy { it.orderIndex }
    val completed = exercises.count { it.status == "COMPLETED" }
    val progress = if (exercises.isEmpty()) 0f else completed.toFloat() / exercises.size
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    when (session.source) {
                        "COACH" -> "Source: Coach assignment"
                        "PERSONAL" -> "Source: personal plan"
                        else -> "Source: legacy workout"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    session.workoutDate?.let { formatDate(it) } ?: "Workout",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Elapsed ${formatDuration(elapsedSeconds)} · $completed of ${exercises.size} exercises")
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSaving) CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                    Text(
                        when {
                            hasUnsyncedChanges -> "Saved on this device · waiting to sync"
                            isSaving -> "Saving workout…"
                            else -> "All changes saved"
                        },
                        Modifier.padding(start = 8.dp),
                        color = if (hasUnsyncedChanges) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (hasUnsyncedChanges) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth(), enabled = !isSaving) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry synchronization")
            }
        }
        if (hasSyncConflict) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "A saved value changed on another device. Your local edit is preserved but has not synced.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    OutlinedButton(onClick = onReapplyConflicts, enabled = !isSaving) {
                        Text("Reapply my local changes")
                    }
                }
            }
        }
        exercises.forEach { exercise ->
            SessionExerciseCard(
                exercise = exercise,
                sessionIsActive = session.status == "IN_PROGRESS" && !isFinishing,
                savingSetIds = savingSetIds,
                onUpdateSet = { setId, update -> onUpdateSet(exercise.id, setId, update) },
                onToggleSkipped = { onToggleSkipped(exercise) },
                onStartHoldTimer = { set -> onStartHoldTimer(exercise, set) },
                onOpenTimerSettings = { onOpenTimerSettings(exercise) }
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            enabled = session.status == "IN_PROGRESS" && !isFinishing && !hasUnsyncedChanges
        ) {
            if (isFinishing) CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
            else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Finish workout")
            }
        }
        TextButton(onClick = onViewHistory, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Workout history")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SessionExerciseCard(
    exercise: WorkoutSessionExercise,
    sessionIsActive: Boolean,
    savingSetIds: Set<Long>,
    onUpdateSet: (Long, WorkoutSessionSetUpdate) -> Unit,
    onToggleSkipped: () -> Unit,
    onStartHoldTimer: (WorkoutSessionSet) -> Unit,
    onOpenTimerSettings: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        when (exercise.status) {
                            "COMPLETED" -> "Completed"
                            "SKIPPED" -> "Skipped"
                            "INCOMPLETE" -> "Unfinished"
                            else -> "Target: ${exercise.targetSets} sets" +
                                (exercise.targetReps?.let { " × $it reps" } ?: "") +
                                (exercise.targetHoldDurationSeconds?.let { " · ${it}s hold" } ?: "")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenTimerSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Timer settings")
                    }
                    if (sessionIsActive && exercise.status != "COMPLETED") {
                        TextButton(onClick = onToggleSkipped) {
                            Text(if (exercise.status == "SKIPPED") "Undo skip" else "Skip")
                        }
                    }
                }
            }
            exercise.instructions?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            exercise.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    onUpdate = { onUpdateSet(set.id, it) },
                    onStartHoldTimer = { onStartHoldTimer(set) }
                )
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
    onUpdate: (WorkoutSessionSetUpdate) -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Set ${set.setNumber}", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (saving) CircularProgressIndicator(Modifier.width(16.dp).height(16.dp), strokeWidth = 2.dp)
                Checkbox(
                    checked = set.completed,
                    enabled = !disabled && !invalid && (!missingRequiredMetric || set.completed),
                    onCheckedChange = { complete ->
                        onUpdate(createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, complete))
                    }
                )
                Text("Done")
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
                    MetricField("Hold (sec)", holdText, KeyboardType.Number, disabled, Modifier.weight(1f)) { value ->
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
                            Text("Timer")
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricField("Added kg", weightText, KeyboardType.Decimal, disabled, Modifier.weight(1f)) { value ->
                weightText = value
                if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..2000.0 } == true) {
                    onUpdate(createSetUpdate(repsText, holdText, value, assistanceText, notesText, set.completed))
                }
            }
            MetricField("Assist kg", assistanceText, KeyboardType.Decimal, disabled, Modifier.weight(1f)) { value ->
                assistanceText = value
                if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..2000.0 } == true) {
                    onUpdate(createSetUpdate(repsText, holdText, weightText, value, notesText, set.completed))
                }
            }
        }
        if (invalid || (missingRequiredMetric && !set.completed)) {
            Text("Enter non-negative numbers within the allowed range.", color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            value = notesText,
            onValueChange = { value ->
                notesText = value.take(2000)
                onUpdate(createSetUpdate(repsText, holdText, weightText, assistanceText, notesText, set.completed))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Set notes") },
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
        title = { Text("Finish workout?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("${session.completedExercises} completed · ${session.skippedExercises} skipped · ${session.unfinishedExercises} unfinished")
                Text("${session.completedSets} completed sets · ${session.totalReps} reps · ${session.totalHoldDurationSeconds}s holds")
                if (unfinished) Text("Unfinished exercises will remain marked as incomplete.")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(unfinished) }) {
                Text(if (unfinished) "Finish partial workout" else "Finish")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep training") } }
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
