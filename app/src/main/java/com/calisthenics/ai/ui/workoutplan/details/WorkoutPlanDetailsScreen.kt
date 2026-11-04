package com.calisthenics.ai.ui.workoutplan.details

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.calisthenics.ai.ui.theme.Accent
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
                        text = uiState.plan?.name ?: "Plan Details",
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
                            contentDescription = "Back",
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
                            contentDescription = "Rename Plan",
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
                    LoadingView(message = "Loading plan details...")
                }
                uiState.error != null && uiState.plan == null -> {
                    ErrorView(
                        message = uiState.error ?: "Failed to load plan",
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
                            Text(
                                text = "Weekly Schedule",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            WeeklyScheduleRow(
                                days = plan.days,
                                selectedDayOfWeek = uiState.selectedDayOfWeek,
                                dayExercises = uiState.dayExercises,
                                onSelectDay = viewModel::selectDay
                            )
                        }

                        // C. Selected Day Header + Add Exercise CTA
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${uiState.selectedDayOfWeek.displayName} · ${
                                            when {
                                                selectedDay == null -> "Not scheduled"
                                                selectedDay.sessionType.isRest -> "Rest Day"
                                                else -> selectedDay.sessionType.displayName
                                            }
                                        }",
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText
                                        )
                                    )
                                    Text(
                                        text = when {
                                            selectedDay == null -> "This weekday is not configured in this plan"
                                            selectedDay.sessionType.isRest -> "Rest and recovery"
                                            else -> "${currentExercises.size} exercises scheduled"
                                        },
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                                            color = SecondaryText
                                        )
                                    )
                                }

                                if (selectedDay != null && !selectedDay.sessionType.isRest) {
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
                                            text = "Add Exercise",
                                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // D. Exercise List for Selected Day
                        if (selectedDay?.sessionType?.isRest == true) {
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
                                        text = "Add Exercise",
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

    // Delete Exercise Confirmation Dialog
    uiState.exerciseToDelete?.let { assignment ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteExerciseDialog,
            title = {
                Text(
                    text = "Remove Exercise?",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Remove \"${assignment.exercise.name}\" from ${uiState.selectedDayOfWeek.displayName}?",
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
                        Text("Remove", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteExerciseDialog) {
                    Text("Cancel", color = SecondaryText)
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
                Text("Rename Plan", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.editPlanName,
                        onValueChange = viewModel::updateEditPlanName,
                        placeholder = { Text("Plan Name", color = SecondaryText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Color(0xFF404040),
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
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeEditPlanDialog) {
                    Text("Cancel", color = SecondaryText)
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
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
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
                    text = "${plan.trainingDayCount} training days / week",
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
                text = "Structured 7-day calisthenics progression cycle.",
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
            val isRest = day?.sessionType?.isRest == true
            val count = day?.id?.let { dayExercises[it]?.size } ?: 0

            Box(
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Surface else Elevated)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Accent else Color(0xFF2E2E2E),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectDay(dayOfWeek) }
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayOfWeek.shortName.uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Accent else SecondaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = day?.sessionType?.displayName ?: "Rest",
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
                            contentDescription = "Rest",
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
                    // Order Badge
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
                            )
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
                                contentDescription = "Move Up",
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
                                contentDescription = "Move Down",
                                tint = if (canMoveDown) SecondaryText else SecondaryText.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Exercise",
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Exercise",
                            tint = Error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF2B2B2B), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Prescription details (Sets, Reps / Hold, Rest)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prescription
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

                // Rest Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rest: ${assignment.restSeconds}s",
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
                        text = "Notes: ${assignment.notes}",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryText
                        )
                    )
                }
            }
        }
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
            text = "Rest & Recovery Day",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Scheduled rest period to allow muscle repair, CNS recovery, and tissue adaptation.",
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
            text = "No Exercises on $dayName",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add calisthenics skills, progressions, or reps to plan your workout session.",
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
            Text("Add Exercise", fontWeight = FontWeight.Bold)
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
                        isEdit -> "Edit Exercise"
                        selectedExercise != null -> "Configure Exercise"
                        else -> "Select Exercise"
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", color = SecondaryText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedExercise == null) {
                // STEP 1: Search & Catalog Selection
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search exercises...", color = SecondaryText) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryText)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Color(0xFF404040),
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
                        label = { Text("All") },
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
                            text = if (searchQuery.isNotBlank()) "No exercises match \"$searchQuery\""
                            else "No exercises available.",
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
                                Text("Change", color = Accent)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise Type Selection
                Text(
                    text = "Exercise Type *",
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
                            text = "Repetition-based",
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
                            text = "Duration-based",
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
                        Text("Sets *", color = SecondaryText, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
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
                                unfocusedBorderColor = Color(0xFF404040),
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
                            text = if (isDuration) "Duration (sec) *" else "Reps *",
                            color = SecondaryText,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = if (isDuration) holdDurationInput else repsInput,
                            onValueChange = if (isDuration) onHoldDurationChange else onRepsChange,
                            placeholder = { Text(if (isDuration) "e.g. 15" else "e.g. 8", color = SecondaryText) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText,
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Color(0xFF404040),
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
                    Text("Rest time (seconds) *", color = SecondaryText, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
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
                            unfocusedBorderColor = Color(0xFF404040),
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
                    Text("Notes / Cues (optional)", color = SecondaryText, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = onNotesChange,
                        placeholder = { Text("e.g. Protracted scapula, hollow body position", color = SecondaryText) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Color(0xFF404040),
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
                            text = if (isEdit) "Update Exercise" else "Save Exercise to Plan",
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
                    .background(Color(0xFF2B2B2B))
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
