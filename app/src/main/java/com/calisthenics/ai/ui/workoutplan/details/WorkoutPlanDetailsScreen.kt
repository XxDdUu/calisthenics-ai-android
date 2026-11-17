package com.calisthenics.ai.ui.workoutplan.details

import com.calisthenics.ai.R

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.media.ExerciseMediaBorder
import com.calisthenics.ai.ui.media.ExerciseStoryViewer
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlanDetailsScreen(
    onBackClick: () -> Unit,
    viewModel: WorkoutPlanDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.plan?.name ?: stringResource(R.string.loc_plan_details_title),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    IconButton(
                        onClick = { viewModel.openEditPlanDialog() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.loc_rename_plan),
                            tint = Accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Ink,
                    titleContentColor = PrimaryText
                )
            )
        },
        containerColor = Ink
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.plan == null -> {
                    LoadingView(message = stringResource(R.string.loc_loading_plan_details))
                }
                uiState.error != null && uiState.plan == null -> {
                    ErrorView(
                        message = uiState.error ?: stringResource(R.string.loc_failed_load_plan),
                        onRetry = { viewModel.loadPlanDetails() }
                    )
                }
                uiState.plan != null -> {
                    val plan = uiState.plan!!
                    val selectedDay = uiState.selectedDay
                    val currentExercises = uiState.currentDayExercises

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // A. Plan Header Card
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            PlanHeaderCard(plan = plan)
                        }

                        // B. Weekly Schedule
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.loc_weekly_schedule_title),
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )
                                )
                                OutlinedButton(
                                    onClick = viewModel::openCustomWorkoutSheet,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.loc_custom))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            WeeklyScheduleRow(
                                days = plan.days,
                                selectedDayOfWeek = uiState.selectedDayOfWeek,
                                dayExercises = uiState.dayExercises,
                                startDate = uiState.parsedStartDate,
                                onSelectDay = viewModel::selectDay
                            )
                        }

                        // C. Selected Day Header + Add Exercise CTA
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            val selectedDateLabel = WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(
                                uiState.selectedDayOfWeek,
                                uiState.parsedStartDate
                            )
                            val workoutTitle = when {
                                selectedDay == null -> stringResource(R.string.loc_not_scheduled)
                                selectedDay.isRestDay || selectedDay.sessionType.isRest -> stringResource(R.string.rest_day_title)
                                else -> selectedDay.name ?: selectedDay.sessionType.displayName
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (selectedDateLabel != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Accent.copy(alpha = 0.15f))
                                                    .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = selectedDateLabel,
                                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Accent
                                                    )
                                                )
                                            }
                                            Text(
                                                text = workoutTitle,
                                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryText
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "${uiState.selectedDayOfWeek.displayName} · $workoutTitle",
                                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryText
                                            )
                                        )
                                    }
                                    Text(
                                        text = when {
                                            selectedDay == null -> stringResource(R.string.loc_weekday_not_configured)
                                            selectedDay.isRestDay || selectedDay.sessionType.isRest -> stringResource(R.string.loc_rest_and_recovery)
                                            else -> selectedDay.description?.takeIf { it.isNotBlank() }
                                                ?: stringResource(R.string.loc_exercises_scheduled, currentExercises.size)
                                        },
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                                            color = SecondaryText
                                        )
                                    )
                                }

                                TextButton(onClick = viewModel::editSelectedWorkoutDay) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Text(stringResource(R.string.loc_edit_day))
                                }

                                if (selectedDay != null && !selectedDay.isRestDay && !selectedDay.sessionType.isRest) {
                                    Button(
                                        onClick = { viewModel.openAddExerciseSheet() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Accent,
                                            contentColor = Ink
                                        ),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.add_exercise),
                                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // D. Exercise List for Selected Day
                        if (selectedDay?.isRestDay == true || selectedDay?.sessionType?.isRest == true) {
                            item {
                                RestDayView()
                            }
                        } else if (currentExercises.isEmpty()) {
                            item {
                                EmptyDayExerciseView(
                                    dayName = uiState.selectedDayOfWeek.displayName,
                                    onAddClick = { viewModel.openAddExerciseSheet() }
                                )
                            }
                        } else {
                            items(currentExercises, key = { it.id }) { assignment ->
                                val index = currentExercises.indexOf(assignment)
                                PlanExerciseCard(
                                    assignment = assignment,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < currentExercises.size - 1,
                                    onMoveUp = if (index > 0) { { viewModel.moveExerciseUp(assignment) } } else null,
                                    onMoveDown = if (index < currentExercises.size - 1) { { viewModel.moveExerciseDown(assignment) } } else null,
                                    onEditClick = { viewModel.openEditExerciseSheet(assignment) },
                                    onDeleteClick = { viewModel.confirmDeleteExercise(assignment) }
                                )
                            }

                            item {
                                OutlinedButton(
                                    onClick = { viewModel.openAddExerciseSheet() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Accent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.add_exercise),
                                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Exercise Bottom Sheet
    if (uiState.isAddExerciseSheetOpen) {
        AddEditExerciseBottomSheet(
            isEdit = uiState.assignmentToEdit != null,
            catalogExercises = uiState.filteredCatalogExercises,
            isLoadingCatalog = uiState.isLoadingCatalog,
            searchQuery = uiState.searchQuery,
            selectedCategory = uiState.selectedCategory,
            selectedExercise = uiState.selectedExerciseForAdd,
            prescriptionType = uiState.prescriptionType,
            setsInput = uiState.setsInput,
            repsInput = uiState.repsInput,
            holdDurationInput = uiState.holdDurationInput,
            restSecondsInput = uiState.restSecondsInput,
            notesInput = uiState.notesInput,
            validationError = uiState.validationError,
            isSubmitting = uiState.isSubmittingExercise,
            onSearchChange = viewModel::onSearchQueryChanged,
            onCategorySelect = viewModel::onCategoryFilterSelected,
            onSelectExercise = viewModel::onExerciseSelectedForAdd,
            onBackToCatalog = viewModel::onBackToCatalogSelection,
            onPrescriptionTypeChange = viewModel::setPrescriptionType,
            onSetsChange = viewModel::updateSets,
            onRepsChange = viewModel::updateReps,
            onHoldDurationChange = viewModel::updateHoldDuration,
            onRestSecondsChange = viewModel::updateRestSeconds,
            onNotesChange = viewModel::updateNotes,
            onSave = viewModel::submitExerciseAssignment,
            onDismiss = viewModel::closeAddExerciseSheet
        )
    }

    uiState.customWorkoutEditor?.let { editor ->
        val unavailableDays = uiState.plan?.days.orEmpty()
            .filter { day ->
                !day.isRestDay && !day.sessionType.isRest && day.dayOfWeek != editor.sourceDay
            }
            .map { it.dayOfWeek }
            .toSet()
        CustomWorkoutBottomSheet(
            editor = editor,
            unavailableDays = unavailableDays,
            isConfirmingDelete = uiState.isConfirmingCustomWorkoutDelete,
            onNameChange = { name ->
                viewModel.updateCustomWorkoutEditor { it.copy(name = name) }
            },
            onTypeChange = { type ->
                viewModel.updateCustomWorkoutEditor { it.copy(workoutType = type) }
            },
            onTrainingFocusChange = { focus ->
                viewModel.updateCustomWorkoutEditor { it.copy(trainingFocus = focus) }
            },
            onDayToggle = viewModel::toggleCustomWorkoutDay,
            onRestDayChange = { rest ->
                viewModel.updateCustomWorkoutEditor { it.copy(isRestDay = rest) }
            },
            onSave = viewModel::saveCustomWorkout,
            onCancel = viewModel::dismissCustomWorkoutSheet,
            onRequestDelete = viewModel::requestDeleteCustomWorkout,
            onCancelDelete = viewModel::cancelDeleteCustomWorkout,
            onConfirmDelete = viewModel::confirmDeleteCustomWorkout
        )
    }

    // Delete Exercise Confirmation Dialog
    uiState.exerciseToDelete?.let { assignment ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteExerciseDialog,
            title = {
                Text(
                    text = stringResource(R.string.loc_remove_exercise_question),
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.loc_remove_exercise_from_day, assignment.exercise.name, uiState.selectedDayOfWeek.displayName),
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::executeDeleteExercise,
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    enabled = !uiState.isDeletingExercise
                ) {
                    if (uiState.isDeletingExercise) {
                        CircularProgressIndicator(
                            color = PrimaryText,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.common_remove), color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteExerciseDialog) {
                    Text(stringResource(R.string.cancel), color = SecondaryText)
                }
            },
            containerColor = Elevated,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Rename Plan Dialog
    if (uiState.isEditPlanDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::closeEditPlanDialog,
            title = {
                Text(stringResource(R.string.loc_rename_plan), color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.editPlanName,
                        onValueChange = viewModel::updateEditPlanName,
                        placeholder = { Text(stringResource(R.string.loc_plan_name_label), color = SecondaryText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.editPlanError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.editPlanError ?: "",
                            color = Error,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::submitUpdatePlan,
                    enabled = !uiState.isUpdatingPlan && uiState.editPlanName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink
                    )
                ) {
                    if (uiState.isUpdatingPlan) {
                        CircularProgressIndicator(color = Ink, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeEditPlanDialog) {
                    Text(stringResource(R.string.cancel), color = SecondaryText)
                }
            },
            containerColor = Elevated,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun PlanHeaderCard(plan: UserWorkoutPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = plan.splitType.displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Text(
                    text = stringResource(R.string.loc_training_days_per_week, plan.trainingDayCount),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        color = SecondaryText
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = plan.name,
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.loc_structured_cycle),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    color = SecondaryText
                )
            )
        }
    }
}

@Composable
fun WeeklyScheduleRow(
    days: List<UserWorkoutPlanDay>,
    selectedDayOfWeek: WorkoutDayOfWeek,
    dayExercises: Map<Long, List<UserWorkoutPlanExercise>>,
    startDate: java.time.LocalDate? = null,
    onSelectDay: (WorkoutDayOfWeek) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WorkoutDayOfWeek.entries.forEach { dayOfWeek ->
            val day = days.firstOrNull { it.dayOfWeek == dayOfWeek }
            val isSelected = dayOfWeek == selectedDayOfWeek
            val isRest = day?.isRestDay == true || day?.sessionType?.isRest == true
            val count = day?.id?.let { dayExercises[it]?.size } ?: 0
            val dateLabel = WorkoutPlanDateFormatter.formatWorkoutDayDateLabel(dayOfWeek, startDate)

            Box(
                modifier = Modifier
                    .width(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Surface else Elevated)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Accent else Border,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectDay(dayOfWeek) }
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dateLabel ?: dayOfWeek.shortName.uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Accent else SecondaryText
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when {
                            day == null || isRest -> stringResource(R.string.loc_rest_label_short)
                            !day.name.isNullOrBlank() -> day.name
                            else -> day.sessionType.displayName
                        },
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            color = if (isRest) SecondaryText.copy(alpha = 0.5f) else PrimaryText,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Exercise count badge or rest icon
                    if (isRest) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = stringResource(R.string.loc_rest_content_description),
                            tint = SecondaryText.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Accent.copy(alpha = 0.2f) else Surface)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$count",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Accent else SecondaryText
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanExerciseCard(
    assignment: UserWorkoutPlanExercise,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var storyStartIndex by rememberSaveable(assignment.id) { mutableStateOf<Int?>(null) }

    ExerciseMediaBorder(
        hasMedia = assignment.exercise.mediaItems.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${assignment.displayOrder}",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = assignment.exercise.name,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (assignment.exercise.targetMuscles.isNotBlank()) {
                                Text(
                                    text = assignment.exercise.targetMuscles,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                        color = SecondaryText
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onMoveUp != null || onMoveDown != null) {
                            IconButton(
                                onClick = { onMoveUp?.invoke() },
                                enabled = canMoveUp,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = stringResource(R.string.loc_move_up),
                                    tint = if (canMoveUp) SecondaryText else SecondaryText.copy(alpha = 0.25f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveDown?.invoke() },
                                enabled = canMoveDown,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = stringResource(R.string.loc_move_down),
                                    tint = if (canMoveDown) SecondaryText else SecondaryText.copy(alpha = 0.25f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.loc_edit_exercise),
                                tint = SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.loc_remove_exercise),
                                tint = Error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (assignment.exercise.mediaItems.isNotEmpty()) {
                            IconButton(
                                onClick = { storyStartIndex = 0 },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.loc_play_exercise_media),
                                    tint = Accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Border, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = assignment.prescription,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.loc_rest_seconds_assignment, assignment.restSeconds),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                color = SecondaryText
                            )
                        )
                    }
                }

                if (!assignment.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.loc_notes_label, assignment.notes),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                                color = SecondaryText
                            )
                        )
                    }
                }

            }
        }
    }

    storyStartIndex?.let { index ->
        ExerciseStoryViewer(
            mediaItems = assignment.exercise.mediaItems,
            initialIndex = index,
            onDismiss = { storyStartIndex = null }
        )
    }
}

@Composable
fun RestDayView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Nightlight,
                contentDescription = null,
                tint = SecondaryText.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.loc_rest_recovery_day),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.loc_rest_recovery_description),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                color = SecondaryText
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyDayExerciseView(
    dayName: String,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = SecondaryText.copy(alpha = 0.5f),
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.loc_no_exercises_on_day, dayName),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.loc_add_skills_hint),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                color = SecondaryText
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.add_exercise), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExerciseBottomSheet(
    isEdit: Boolean,
    catalogExercises: List<Exercise>,
    isLoadingCatalog: Boolean,
    searchQuery: String,
    selectedCategory: ExerciseCategory?,
    selectedExercise: Exercise?,
    prescriptionType: ExercisePrescriptionType,
    setsInput: String,
    repsInput: String,
    holdDurationInput: String,
    restSecondsInput: String,
    notesInput: String,
    validationError: String?,
    isSubmitting: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (ExerciseCategory?) -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onBackToCatalog: () -> Unit,
    onPrescriptionTypeChange: (ExercisePrescriptionType) -> Unit,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onHoldDurationChange: (String) -> Unit,
    onRestSecondsChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Elevated,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isEdit -> stringResource(R.string.loc_edit_exercise_title)
                        selectedExercise != null -> stringResource(R.string.loc_configure_exercise)
                        else -> stringResource(R.string.loc_select_exercise)
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close), color = SecondaryText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedExercise == null) {
                // STEP 1: Search & Catalog Selection
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.loc_search_exercises_ellipsis), color = SecondaryText) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryText)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border,
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelect(null) },
                        label = { Text(stringResource(R.string.common_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            selectedLabelColor = Ink,
                            containerColor = Surface,
                            labelColor = SecondaryText
                        )
                    )
                    ExerciseCategory.entries.filter { it != ExerciseCategory.UNKNOWN }.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onCategorySelect(category) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent,
                                selectedLabelColor = Ink,
                                containerColor = Surface,
                                labelColor = SecondaryText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Catalog List
                if (isLoadingCatalog) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent)
                    }
                } else if (catalogExercises.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotBlank()) stringResource(R.string.loc_no_search_exercises, searchQuery)
                            else stringResource(R.string.loc_no_exercises_available_period),
                            color = SecondaryText,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(catalogExercises, key = { it.id }) { exercise ->
                            CatalogExerciseItem(
                                exercise = exercise,
                                onSelect = { onSelectExercise(exercise) }
                            )
                        }
                    }
                }
            } else {
                // STEP 2: Configure Exercise Parameters
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedExercise.name,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            )
                            if (selectedExercise.targetMuscles.isNotBlank()) {
                                Text(
                                    text = selectedExercise.targetMuscles,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                        color = SecondaryText
                                    )
                                )
                            }
                        }
                        if (!isEdit) {
                            TextButton(onClick = onBackToCatalog) {
                                Text(stringResource(R.string.loc_change), color = Accent)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise Type Selection
                Text(
                    text = stringResource(R.string.loc_exercise_type_required),
                    color = SecondaryText,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isRep = prescriptionType == ExercisePrescriptionType.REPETITIONS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRep) Accent else Color.Transparent)
                            .clickable { onPrescriptionTypeChange(ExercisePrescriptionType.REPETITIONS) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.loc_repetition_based),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isRep) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRep) Ink else SecondaryText
                            )
                        )
                    }

                    val isDur = prescriptionType == ExercisePrescriptionType.DURATION
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDur) Accent else Color.Transparent)
                            .clickable { onPrescriptionTypeChange(ExercisePrescriptionType.DURATION) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.loc_duration_based),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isDur) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDur) Ink else SecondaryText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sets & (Reps OR Duration) Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.loc_sets_required), color = SecondaryText, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = setsInput,
                            onValueChange = onSetsChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText,
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val isDuration = prescriptionType == ExercisePrescriptionType.DURATION
                        Text(
                            text = if (isDuration) stringResource(R.string.loc_duration_reps_required) else stringResource(R.string.loc_reps_required),
                            color = SecondaryText,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = if (isDuration) holdDurationInput else repsInput,
                            onValueChange = if (isDuration) onHoldDurationChange else onRepsChange,
                            placeholder = { Text(if (isDuration) stringResource(R.string.loc_example_15) else stringResource(R.string.loc_example_8), color = SecondaryText) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText,
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rest Seconds
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.loc_rest_time_required), color = SecondaryText, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = restSecondsInput,
                        onValueChange = onRestSecondsChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.loc_notes_cues_optional), color = SecondaryText, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = onNotesChange,
                        placeholder = { Text(stringResource(R.string.loc_example_cues), color = SecondaryText) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border,
                            focusedContainerColor = Surface,
                            unfocusedContainerColor = Surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = validationError,
                        color = Error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = onSave,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Ink, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isEdit) stringResource(R.string.loc_update_exercise) else stringResource(R.string.loc_save_exercise_to_plan),
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogExerciseItem(
    exercise: Exercise,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                )
                if (exercise.targetMuscles.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exercise.targetMuscles,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            color = SecondaryText
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Elevated)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = exercise.difficulty.name,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
