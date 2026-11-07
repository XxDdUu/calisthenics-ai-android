package com.calisthenics.ai.ui.workoutplan

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.PlannedExercise
import com.calisthenics.ai.domain.model.PlannedWorkout
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val weekFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
private val workoutCategories = listOf("Push", "Pull", "Legs", "Upper", "Lower", "Full Body", "Skill", "Custom")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanScreen(
    onStartWorkout: (workoutId: String, date: String) -> Unit,
    viewModel: WorkoutPlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var overflowExpanded by remember { mutableStateOf(false) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var confirmDeletePlan by remember { mutableStateOf(false) }
    var workoutPendingDelete by remember { mutableStateOf<PlannedWorkout?>(null) }
    var showDiscardEditorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.planError) {
        state.planError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Workout Plan", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${state.weekStart.format(weekFormatter)} – ${state.weekStart.plusDays(6).format(weekFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPlanDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create a new plan")
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Plan options")
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("New weekly plan") },
                                onClick = { overflowExpanded = false; showPlanDialog = true }
                            )
                            if (state.plan != null) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Delete plan") },
                                    onClick = { overflowExpanded = false; confirmDeletePlan = true }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> WorkoutPlanLoading(Modifier.padding(padding))
            state.planError != null && state.plan == null -> PlanLoadError(
                modifier = Modifier.padding(padding),
                error = state.planError.orEmpty(),
                onRetry = viewModel::loadPlan
            )
            else -> WorkoutPlanContent(
                modifier = Modifier.padding(padding),
                state = state,
                viewModel = viewModel,
                onStartWorkout = onStartWorkout,
                onCreatePlan = { showPlanDialog = true },
                onRequestDeleteWorkout = { workoutPendingDelete = it }
            )
        }
    }

    if (showPlanDialog) {
        PlanNameDialog(
            replacesExisting = state.plan != null,
            onDismiss = { showPlanDialog = false },
            onConfirm = { name, template ->
                showPlanDialog = false
                viewModel.createPlan(name, template)
            }
        )
    }
    if (confirmDeletePlan) {
        ConfirmDialog(
            title = "Delete workout plan?",
            message = "This removes the locally saved plan and its completion history from this device.",
            confirmLabel = "Delete plan",
            onDismiss = { confirmDeletePlan = false },
            onConfirm = { confirmDeletePlan = false; viewModel.deletePlan() }
        )
    }
    workoutPendingDelete?.let { workout ->
        ConfirmDialog(
            title = "Remove ${workout.name}?",
            message = "This workout will no longer be scheduled on ${dayName(workout.weekday)}.",
            confirmLabel = "Remove",
            onDismiss = { workoutPendingDelete = null },
            onConfirm = { workoutPendingDelete = null; viewModel.removeWorkout(workout.id) }
        )
    }
    if (state.isExercisePickerOpen) {
        ExercisePickerSheet(
            exercises = state.exercises,
            loading = state.isLoadingExercises,
            error = state.exerciseLoadError,
            alreadySelectedIds = state.editor?.exercises?.mapNotNull { it.exerciseId }?.toSet().orEmpty(),
            onRetry = viewModel::loadExerciseCatalog,
            onDismiss = viewModel::closeExercisePicker,
            onAdd = viewModel::addExercisesToDraft
        )
    } else {
        state.editor?.let { editor ->
            WorkoutEditorSheet(
                editor = editor,
                isSaving = state.isSaving,
                onEditorChange = viewModel::updateEditor,
                onAddExercise = viewModel::openExercisePicker,
                onUpdateExercise = viewModel::updateDraftExercise,
                onMoveExercise = viewModel::moveDraftExercise,
                onRemoveExercise = viewModel::removeDraftExercise,
                onSave = viewModel::saveWorkout,
                onDismiss = { showDiscardEditorDialog = true }
            )
        }
    }
    if (showDiscardEditorDialog) {
        ConfirmDialog(
            title = "Discard workout changes?",
            message = "Your unsaved workout edits will be lost.",
            confirmLabel = "Discard",
            onDismiss = { showDiscardEditorDialog = false },
            onConfirm = { showDiscardEditorDialog = false; viewModel.dismissEditor() }
        )
    }
}

@Composable
private fun WorkoutPlanContent(
    modifier: Modifier,
    state: WorkoutPlanUiState,
    viewModel: WorkoutPlanViewModel,
    onStartWorkout: (String, String) -> Unit,
    onCreatePlan: () -> Unit,
    onRequestDeleteWorkout: (PlannedWorkout) -> Unit
) {
    val selectedWorkout = viewModel.selectedWorkout()
    val overview = viewModel.weeklyOverview()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DeviceOnlyNotice()
        }
        item {
            WeekControls(
                weekStart = state.weekStart,
                onPrevious = viewModel::showPreviousWeek,
                onNext = viewModel::showNextWeek,
                onToday = viewModel::returnToCurrentWeek
            )
        }
        item {
            WeeklyCalendar(
                weekStart = state.weekStart,
                selectedDate = state.selectedDate,
                scheduledWeekdays = state.plan?.workouts?.map { it.weekday }?.toSet().orEmpty(),
                onDateClick = viewModel::selectDate
            )
        }
        if (state.plan == null) {
            item {
                EmptyState(
                    title = "Build your weekly schedule",
                    body = "Create a plan, then assign workouts and exercises to the days that work for you.",
                    action = "Create plan",
                    onAction = onCreatePlan
                )
            }
        } else {
            item { WeeklyOverview(overview) }
            item {
                Text(
                    state.selectedDate.format(fullDateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                AnimatedContent(targetState = selectedWorkout, label = "selected workout") { workout ->
                    if (workout == null) {
                        NoWorkoutCard(onAdd = viewModel::openCreateWorkout)
                    } else {
                        WorkoutSummaryCard(
                            workout = workout,
                            completed = viewModel.isWorkoutCompleted(workout.id),
                            onStart = { onStartWorkout(workout.id, state.selectedDate.toString()) },
                            onEdit = { viewModel.openEditWorkout(workout) },
                            onDuplicate = { viewModel.openDuplicateWorkout(workout) },
                            onComplete = { viewModel.markWorkoutCompleted(workout) },
                            onDelete = { onRequestDeleteWorkout(workout) }
                        )
                    }
                }
            }
            selectedWorkout?.let { workout ->
                item { Text("Exercises", style = MaterialTheme.typography.titleLarge) }
                if (workout.exercises.isEmpty()) {
                    item {
                        DarkCard(Modifier.fillMaxWidth()) {
                            EmptyState(
                                title = "No exercises yet",
                                body = "Edit this workout to choose exercises from the available library.",
                                action = "Add exercises",
                                onAction = { viewModel.openEditWorkout(workout); viewModel.openExercisePicker() }
                            )
                        }
                    }
                } else {
                    items(workout.exercises, key = { it.id }) { exercise ->
                        ExercisePlanRow(
                            exercise = exercise,
                            completed = viewModel.isExerciseCompleted(workout.id, exercise.id),
                            onToggle = { viewModel.toggleExerciseCompleted(workout, exercise.id) },
                            onEdit = { viewModel.openEditWorkout(workout) }
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DeviceOnlyNotice() {
    AssistChip(
        onClick = {},
        label = { Text("Saved on this device") },
        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun WeekControls(weekStart: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous week")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Training week", style = MaterialTheme.typography.labelLarge)
            Text(
                "${weekStart.format(weekFormatter)} – ${weekStart.plusDays(6).format(weekFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToday) { Text("Today") }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next week")
            }
        }
    }
}

@Composable
private fun WeeklyCalendar(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    scheduledWeekdays: Set<Int>,
    onDateClick: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(7) { index ->
            val date = weekStart.plusDays(index.toLong())
            val selected = date == selectedDate
            val hasWorkout = date.dayOfWeek.value in scheduledWeekdays
            val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(container)
                    .clickable(role = Role.Tab) { onDateClick(date) }
                    .padding(vertical = 10.dp, horizontal = 2.dp)
                    .semantics { contentDescription = "${date.format(fullDateFormatter)}${if (hasWorkout) ", training day" else ", rest day"}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = content,
                    maxLines = 1
                )
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = content)
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (hasWorkout) content else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f))
                )
            }
        }
    }
}

@Composable
private fun WeeklyOverview(overview: WeeklyTrainingOverview) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("This week", style = MaterialTheme.typography.titleLarge)
            Text("${overview.completionPercent}% complete", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverviewMetric(Modifier.weight(1f), overview.sessions.toString(), "Sessions")
            OverviewMetric(Modifier.weight(1f), overview.exercises.toString(), "Exercises")
            OverviewMetric(Modifier.weight(1f), overview.completedSessions.toString(), "Completed")
            OverviewMetric(Modifier.weight(1f), "${overview.completionPercent}%", "Progress")
        }
    }
}

@Composable
private fun OverviewMetric(modifier: Modifier, value: String, label: String) {
    DarkCard(modifier.heightIn(min = 82.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun NoWorkoutCard(onAdd: () -> Unit) {
    DarkCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Rest day", style = MaterialTheme.typography.titleLarge)
            Text("No workout is assigned to this day. Add one when you are ready to train.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add workout") }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    workout: PlannedWorkout,
    completed: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    DarkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(dayName(workout.weekday).uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(workout.name, style = MaterialTheme.typography.headlineMedium)
                    Text(workout.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (completed) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Workout completed", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                WorkoutMeta(Icons.Default.Timer, "${workout.estimatedDurationMinutes} min")
                WorkoutMeta(Icons.Default.FitnessCenter, "${workout.exercises.size} exercises")
            }
            workout.notes.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(), enabled = workout.exercises.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (workout.exercises.isEmpty()) "Add exercises to start" else "Start workout")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Edit, null, Modifier.size(17.dp)); Text(" Edit") }
                TextButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, null, Modifier.size(17.dp)); Text(" Copy") }
                TextButton(onClick = onComplete, enabled = !completed, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Check, null, Modifier.size(17.dp)); Text(if (completed) " Done" else " Complete") }
            }
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
                Text(" Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun WorkoutMeta(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExercisePlanRow(exercise: PlannedExercise, completed: Boolean, onToggle: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = completed, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${exercise.prescription} · ${exercise.restSeconds}s rest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                exercise.notes.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            Icon(Icons.Default.Edit, contentDescription = "Edit exercise", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WorkoutPlanLoading(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun PlanLoadError(modifier: Modifier, error: String, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DarkCard(Modifier.padding(24.dp).fillMaxWidth()) {
            EmptyState("Workout plan unavailable", error, "Retry", onRetry)
        }
    }
}

@Composable
private fun PlanNameDialog(
    replacesExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, WorkoutPlanTemplate) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var template by rememberSaveable { mutableStateOf(WorkoutPlanTemplate.CUSTOM) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (replacesExisting) "Create new weekly plan" else "Create workout plan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (replacesExisting) Text("Your current locally saved plan will be replaced only after you confirm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plan name") },
                    singleLine = true,
                    isError = attemptedSave && name.isBlank(),
                    supportingText = if (attemptedSave && name.isBlank()) ({ Text("A plan name is required.") }) else null
                )
                Text("Schedule template", style = MaterialTheme.typography.labelLarge)
                WorkoutPlanTemplate.entries.forEach { option ->
                    FilterChip(
                        selected = template == option,
                        onClick = { template = option },
                        label = { Text(option.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(template.preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { attemptedSave = true; if (name.isNotBlank()) onConfirm(name, template) }) { Text(if (replacesExisting) "Replace plan" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutEditorSheet(
    editor: WorkoutEditorState,
    isSaving: Boolean,
    onEditorChange: ((WorkoutEditorState) -> WorkoutEditorState) -> Unit,
    onAddExercise: () -> Unit,
    onUpdateExercise: (PlannedExercise) -> Unit,
    onMoveExercise: (String, Int) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxHeight(.92f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (editor.workoutId == null) if (editor.isDuplicate) "Duplicate workout" else "Add workout" else "Edit workout", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = editor.name,
                onValueChange = { value -> onEditorChange { it.copy(name = value, nameError = null) } },
                label = { Text("Workout name") },
                isError = editor.nameError != null,
                supportingText = editor.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Training category", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                workoutCategories.forEach { category ->
                    FilterChip(
                        selected = editor.category == category,
                        onClick = { onEditorChange { it.copy(category = category) } },
                        label = { Text(category) }
                    )
                }
            }
            Text("Assigned day", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..7).forEach { weekday ->
                    FilterChip(
                        selected = editor.weekday == weekday,
                        onClick = { onEditorChange { it.copy(weekday = weekday) } },
                        label = { Text(dayName(weekday).take(3)) }
                    )
                }
            }
            OutlinedTextField(
                value = editor.estimatedDuration,
                onValueChange = { value -> onEditorChange { it.copy(estimatedDuration = value.filter(Char::isDigit), durationError = null) } },
                label = { Text("Estimated duration (minutes)") },
                isError = editor.durationError != null,
                supportingText = editor.durationError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = editor.notes,
                onValueChange = { value -> onEditorChange { it.copy(notes = value) } },
                label = { Text("Workout notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Exercises", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = onAddExercise) { Icon(Icons.Default.Add, null, Modifier.size(17.dp)); Text(" Add") }
            }
            if (editor.exercises.isEmpty()) {
                Text("Choose exercises to add them to this workout.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                editor.exercises.forEachIndexed { index, exercise ->
                    DraftExerciseEditor(
                        exercise = exercise,
                        canMoveUp = index > 0,
                        canMoveDown = index < editor.exercises.lastIndex,
                        onChange = onUpdateExercise,
                        onMove = { onMoveExercise(exercise.id, it) },
                        onRemove = { onRemoveExercise(exercise.id) }
                    )
                }
            }
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Save workout")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DraftExerciseEditor(
    exercise: PlannedExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (PlannedExercise) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit
) {
    DarkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    if (exercise.category.isNotBlank()) Text(exercise.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.AutoMirrored.Filled.Sort, "Move exercise up", Modifier.size(19.dp)) }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Default.ArrowDownward, "Move exercise down", Modifier.size(19.dp)) }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, "Remove exercise", tint = MaterialTheme.colorScheme.error) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberEditor("Sets", exercise.sets, Modifier.weight(1f)) { onChange(exercise.copy(sets = it)) }
                if (exercise.holdDurationSeconds != null) {
                    NumberEditor("Seconds", exercise.holdDurationSeconds, Modifier.weight(1f)) { onChange(exercise.copy(holdDurationSeconds = it, repetitions = null)) }
                } else {
                    NumberEditor("Reps", exercise.repetitions ?: 0, Modifier.weight(1f)) { onChange(exercise.copy(repetitions = it, holdDurationSeconds = null)) }
                }
                NumberEditor("Rest", exercise.restSeconds, Modifier.weight(1f)) { onChange(exercise.copy(restSeconds = it)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = exercise.holdDurationSeconds == null,
                    onClick = { if (exercise.holdDurationSeconds != null) onChange(exercise.copy(repetitions = 8, holdDurationSeconds = null)) },
                    label = { Text("Reps") }
                )
                FilterChip(
                    selected = exercise.holdDurationSeconds != null,
                    onClick = { if (exercise.holdDurationSeconds == null) onChange(exercise.copy(repetitions = null, holdDurationSeconds = 10)) },
                    label = { Text("Hold") }
                )
            }
            OutlinedTextField(
                value = exercise.notes,
                onValueChange = { onChange(exercise.copy(notes = it)) },
                label = { Text("Exercise notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1
            )
        }
    }
}

@Composable
private fun NumberEditor(label: String, value: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.takeIf { number -> number > 0 }?.let(onChange) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    loading: Boolean,
    error: String?,
    alreadySelectedIds: Set<Long>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: (Set<Long>) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val visibleExercises = exercises.filter { exercise ->
        exercise.id !in alreadySelectedIds &&
            exercise.name.contains(query, ignoreCase = true) &&
            (category == null || exercise.category.name == category)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxHeight(.9f).imePadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Add exercises", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close exercise picker") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                listOf("PUSH", "PULL", "CORE", "LEGS", "SKILL", "ISOMETRIC").forEach { value ->
                    FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value.lowercase().replaceFirstChar(Char::titlecase)) })
                }
            }
            when {
                loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null && exercises.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Exercise library unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onRetry) { Text("Retry") }
                    }
                }
                visibleExercises.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (exercises.isEmpty()) "No exercises are available yet." else "No exercises match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(visibleExercises, key = { it.id }) { exercise ->
                        ExercisePickRow(
                            exercise = exercise,
                            checked = exercise.id in selectedIds,
                            onClick = {
                                selectedIds = selectedIds.toMutableSet().also {
                                    if (!it.add(exercise.id)) it.remove(exercise.id)
                                }
                            }
                        )
                    }
                }
            }
            Button(onClick = { onAdd(selectedIds) }, enabled = selectedIds.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Add ${selectedIds.size} exercise${if (selectedIds.size == 1) "" else "s"}")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExercisePickRow(exercise: Exercise, checked: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onClick() })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(exercise.category.name.lowercase().replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun dayName(weekday: Int): String = java.time.DayOfWeek.of(weekday)
    .getDisplayName(TextStyle.FULL, Locale.getDefault())
