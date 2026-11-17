package com.calisthenics.ai.ui.workoutplan.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.ui.components.UserAvatar
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.CardBackground
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error as ThemeError
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.SubtleBorder
import com.calisthenics.ai.ui.theme.Success
import com.calisthenics.ai.ui.theme.Surface as ThemeSurface
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private enum class WorkoutSelectionMode {
    EXISTING_PLAN,
    BUILD_FROM_EXERCISES
}

@Immutable
private data class ExerciseDraftEntry(
    val uid: String,
    val exercise: Exercise,
    val setsText: String = "3",
    val repsText: String = "10",
    val durationText: String = "30",
    val restSecondsText: String = "60",
    val useDuration: Boolean = false,
    val notes: String = ""
) {
    fun toConfiguredOrNull(): ConfiguredAssignmentExercise? {
        val sets = setsText.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
        val rest = restSecondsText.trim().toIntOrNull()?.takeIf { it >= 0 } ?: 60
        return if (useDuration) {
            val duration = durationText.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
            ConfiguredAssignmentExercise(
                exercise = exercise,
                sets = sets,
                reps = null,
                durationSeconds = duration,
                restSeconds = rest,
                notes = notes.trim()
            )
        } else {
            val reps = repsText.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
            ConfiguredAssignmentExercise(
                exercise = exercise,
                sets = sets,
                reps = reps,
                durationSeconds = null,
                restSeconds = rest,
                notes = notes.trim()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachAssignmentBottomSheet(
    relationships: List<CoachClientRelationship>,
    plans: List<UserWorkoutPlan>,
    selectedClientId: Long? = null,
    onSelectClient: (Long) -> Unit = {},
    isSubmitting: Boolean,
    error: String?,
    onAssign: (Long?, Long, () -> Unit) -> Unit,
    onDismiss: () -> Unit,
    availableExercises: List<Exercise> = emptyList(),
    isLoadingExercises: Boolean = false,
    exercisesError: String? = null,
    onRetryLoadExercises: () -> Unit = {},
    onClearError: () -> Unit = {},
    onAssignWithDetails: ((Long?, Long, String?, String?, String?, () -> Unit) -> Unit)? = null,
    onCreateAndAssignCustomWorkout: ((Long?, String, WorkoutSessionType, WorkoutDayOfWeek, List<ConfiguredAssignmentExercise>, String?, String?, String?, () -> Unit) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeClients = remember(relationships) {
        relationships.filter {
            it.status.equals("ACTIVE", ignoreCase = true) ||
                it.status.equals("CONNECTED", ignoreCase = true)
        }
    }

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var chosenClientId by rememberSaveable { mutableStateOf(selectedClientId) }
    var clientQuery by rememberSaveable { mutableStateOf("") }

    var workoutModeName by rememberSaveable { mutableStateOf(WorkoutSelectionMode.EXISTING_PLAN.name) }
    val workoutMode = WorkoutSelectionMode.valueOf(workoutModeName)
    var planQuery by rememberSaveable { mutableStateOf("") }
    var selectedPlanId by rememberSaveable { mutableStateOf<Long?>(null) }

    var customWorkoutName by rememberSaveable { mutableStateOf("") }
    var customDayName by rememberSaveable { mutableStateOf(WorkoutDayOfWeek.MONDAY.name) }
    var customSessionTypeName by rememberSaveable { mutableStateOf(WorkoutSessionType.FULL_BODY.name) }
    var exerciseSearchQuery by rememberSaveable { mutableStateOf("") }
    val customExercises = remember { mutableStateListOf<ExerciseDraftEntry>() }
    var draftCounter by remember { mutableIntStateOf(0) }

    var startDateInput by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var endDateInput by rememberSaveable { mutableStateOf("") }
    var coachNotesInput by rememberSaveable { mutableStateOf("") }
    var explicitConfirmed by remember { mutableStateOf(false) }

    val selectedClient = activeClients.firstOrNull {
        it.clientId == chosenClientId || it.id == chosenClientId
    }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    val selectedDay = WorkoutDayOfWeek.fromName(customDayName)
    val selectedSessionType = WorkoutSessionType.fromName(customSessionTypeName)

    val parsedCustomExercises = remember(customExercises.toList()) {
        customExercises.mapNotNull { it.toConfiguredOrNull() }
    }
    val allCustomExercisesValid = customExercises.isNotEmpty() &&
        parsedCustomExercises.size == customExercises.size

    val canProceedStep1 = selectedClient != null
    val canProceedStep2 = when (workoutMode) {
        WorkoutSelectionMode.EXISTING_PLAN -> selectedPlan != null
        WorkoutSelectionMode.BUILD_FROM_EXERCISES ->
            customWorkoutName.trim().isNotEmpty() && allCustomExercisesValid
    }

    val scheduleValidationError = remember(startDateInput, endDateInput) {
        validateAssignmentDates(startDateInput, endDateInput)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = sheetState,
        containerColor = Elevated,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Assign Workout",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        text = when (currentStep) {
                            0 -> "Step 1 of 3 • Select a client"
                            1 -> "Step 2 of 3 • Select or build a workout"
                            else -> "Step 3 of 3 • Review & confirm"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
                IconButton(
                    onClick = { if (!isSubmitting) onDismiss() },
                    enabled = !isSubmitting
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = SecondaryText
                    )
                }
            }

            // 3-Step Progress Indicator
            AssignmentStepIndicator(
                currentStep = currentStep,
                canVisitStep2 = canProceedStep1,
                canVisitStep3 = canProceedStep1 && canProceedStep2,
                onStepClick = { target ->
                    if (!isSubmitting) {
                        onClearError()
                        if (target < currentStep || (target == 1 && canProceedStep1) || (target == 2 && canProceedStep1 && canProceedStep2)) {
                            if (target == 2 && currentStep != 2) {
                                explicitConfirmed = false
                            }
                            currentStep = target
                        }
                    }
                }
            )

            HorizontalDivider(color = SubtleBorder, modifier = Modifier.padding(top = 12.dp))

            if (!error.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = ThemeError.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ThemeError.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = error,
                            color = ThemeError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearError) {
                            Text("Dismiss", color = ThemeError, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Main Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    0 -> StepSelectClientContent(
                        clients = activeClients,
                        query = clientQuery,
                        onQueryChange = { clientQuery = it },
                        selectedClientId = selectedClient?.clientId,
                        onSelectClient = { rel ->
                            onClearError()
                            if (chosenClientId != rel.clientId) {
                                explicitConfirmed = false
                            }
                            chosenClientId = rel.clientId
                            onSelectClient(rel.clientId)
                        }
                    )

                    1 -> StepSelectWorkoutContent(
                        selectedClient = selectedClient,
                        onChangeClient = {
                            onClearError()
                            currentStep = 0
                        },
                        mode = workoutMode,
                        onModeChange = { nextMode ->
                            onClearError()
                            explicitConfirmed = false
                            workoutModeName = nextMode.name
                        },
                        plans = plans,
                        planQuery = planQuery,
                        onPlanQueryChange = { planQuery = it },
                        selectedPlanId = selectedPlanId,
                        onSelectPlan = { id ->
                            onClearError()
                            explicitConfirmed = false
                            selectedPlanId = id
                        },
                        customWorkoutName = customWorkoutName,
                        onCustomWorkoutNameChange = {
                            explicitConfirmed = false
                            customWorkoutName = it
                        },
                        selectedDay = selectedDay,
                        onSelectDay = { customDayName = it.name },
                        selectedSessionType = selectedSessionType,
                        onSelectSessionType = { customSessionTypeName = it.name },
                        availableExercises = availableExercises,
                        isLoadingExercises = isLoadingExercises,
                        exercisesError = exercisesError,
                        onRetryLoadExercises = onRetryLoadExercises,
                        exerciseSearchQuery = exerciseSearchQuery,
                        onExerciseSearchChange = { exerciseSearchQuery = it },
                        customExercises = customExercises,
                        onAddExercise = { exercise ->
                            onClearError()
                            explicitConfirmed = false
                            draftCounter += 1
                            val isHold = exercise.category.name == "ISOMETRIC" || exercise.category.name == "SKILL"
                            customExercises.add(
                                ExerciseDraftEntry(
                                    uid = "${exercise.id}_$draftCounter",
                                    exercise = exercise,
                                    setsText = "3",
                                    repsText = if (isHold) "8" else "10",
                                    durationText = "20",
                                    restSecondsText = "60",
                                    useDuration = isHold
                                )
                            )
                        },
                        onUpdateExercise = { idx, updated ->
                            explicitConfirmed = false
                            customExercises[idx] = updated
                        },
                        onRemoveExercise = { idx ->
                            explicitConfirmed = false
                            customExercises.removeAt(idx)
                        }
                    )

                    else -> StepReviewAndAssignContent(
                        selectedClient = selectedClient,
                        workoutMode = workoutMode,
                        selectedPlan = selectedPlan,
                        customWorkoutName = customWorkoutName.trim(),
                        selectedDay = selectedDay,
                        selectedSessionType = selectedSessionType,
                        configuredExercises = customExercises,
                        startDate = startDateInput,
                        onStartDateChange = {
                            onClearError()
                            startDateInput = it
                        },
                        endDate = endDateInput,
                        onEndDateChange = {
                            onClearError()
                            endDateInput = it
                        },
                        notes = coachNotesInput,
                        onNotesChange = { coachNotesInput = it },
                        scheduleValidationError = scheduleValidationError,
                        explicitConfirmed = explicitConfirmed,
                        onConfirmedChange = { explicitConfirmed = it },
                        enabled = !isSubmitting
                    )
                }
            }

            // Bottom Action Bar
            HorizontalDivider(color = SubtleBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeSurface)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStep == 0) {
                            onDismiss()
                        } else {
                            onClearError()
                            currentStep -= 1
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.38f)
                ) {
                    if (currentStep > 0) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    } else {
                        Text("Cancel")
                    }
                }

                if (currentStep < 2) {
                    val canContinue = if (currentStep == 0) canProceedStep1 else canProceedStep2
                    Button(
                        onClick = {
                            onClearError()
                            if (currentStep == 0 && canProceedStep1) {
                                currentStep = 1
                            } else if (currentStep == 1 && canProceedStep2) {
                                explicitConfirmed = false
                                currentStep = 2
                            }
                        },
                        enabled = canContinue && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Ink
                        ),
                        modifier = Modifier.weight(0.62f)
                    ) {
                        Text(
                            text = if (currentStep == 0) "Continue to Workout" else "Continue to Review",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    val canSubmit = !isSubmitting &&
                        selectedClient != null &&
                        canProceedStep2 &&
                        scheduleValidationError == null &&
                        explicitConfirmed
                    Button(
                        onClick = {
                            val client = selectedClient ?: return@Button
                            val cleanStart = startDateInput.trim().ifBlank { null }
                            val cleanEnd = endDateInput.trim().ifBlank { null }
                            val cleanNotes = coachNotesInput.trim().ifBlank { null }
                            if (workoutMode == WorkoutSelectionMode.EXISTING_PLAN) {
                                val planId = selectedPlan?.id ?: return@Button
                                if (onAssignWithDetails != null) {
                                    onAssignWithDetails(
                                        client.clientId,
                                        planId,
                                        cleanStart,
                                        cleanEnd,
                                        cleanNotes,
                                        onDismiss
                                    )
                                } else {
                                    onAssign(client.clientId, planId, onDismiss)
                                }
                            } else {
                                val creator = onCreateAndAssignCustomWorkout ?: return@Button
                                creator(
                                    client.clientId,
                                    customWorkoutName.trim(),
                                    selectedSessionType,
                                    selectedDay,
                                    parsedCustomExercises,
                                    cleanStart,
                                    cleanEnd,
                                    cleanNotes,
                                    onDismiss
                                )
                            }
                        },
                        enabled = canSubmit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Ink
                        ),
                        modifier = Modifier.weight(0.62f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Ink
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assigning...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm & Assign", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentStepIndicator(
    currentStep: Int,
    canVisitStep2: Boolean,
    canVisitStep3: Boolean,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf(
        0 to "Client",
        1 to "Workout",
        2 to "Review"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEach { (stepIndex, label) ->
            val isActive = stepIndex == currentStep
            val isCompleted = stepIndex < currentStep
            val isReachable = when (stepIndex) {
                0 -> true
                1 -> canVisitStep2
                else -> canVisitStep3
            }
            val bgColor = when {
                isActive -> Accent.copy(alpha = 0.16f)
                isCompleted -> Success.copy(alpha = 0.12f)
                else -> CardBackground
            }
            val borderColor = when {
                isActive -> Accent
                isCompleted -> Success.copy(alpha = 0.6f)
                else -> Border
            }
            val textColor = when {
                isActive -> Accent
                isCompleted -> Success
                else -> SecondaryText
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = isReachable) { onStepClick(stepIndex) },
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) Success else if (isActive) Accent else Border),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Text(
                                text = "${stepIndex + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Ink else PrimaryText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StepSelectClientContent(
    clients: List<CoachClientRelationship>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedClientId: Long?,
    onSelectClient: (CoachClientRelationship) -> Unit
) {
    val filteredClients = remember(clients, query) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            clients
        } else {
            clients.filter {
                it.clientName.contains(clean, ignoreCase = true) ||
                    (it.clientExperienceLevel?.contains(clean, ignoreCase = true) == true) ||
                    it.status.contains(clean, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = SecondaryText)
            },
            placeholder = { Text("Search connected clients...") },
            shape = RoundedCornerShape(12.dp)
        )

        if (clients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonSearch,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No Active Clients Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Text(
                        text = "Connect with an athlete in Coach Connections before assigning a workout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                }
            }
        } else if (filteredClients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No clients match \"$query\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filteredClients, key = { it.id }) { client ->
                    val isSelected = selectedClientId == client.clientId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectClient(client) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Accent.copy(alpha = 0.14f) else CardBackground,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Accent else Border
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UserAvatar(url = client.clientAvatarUrl, modifier = Modifier.size(48.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = client.clientName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Success.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "Active Client",
                                            color = Success,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    client.clientExperienceLevel?.takeIf { it.isNotBlank() }?.let { level ->
                                        Text(
                                            text = level.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSelectWorkoutContent(
    selectedClient: CoachClientRelationship?,
    onChangeClient: () -> Unit,
    mode: WorkoutSelectionMode,
    onModeChange: (WorkoutSelectionMode) -> Unit,
    plans: List<UserWorkoutPlan>,
    planQuery: String,
    onPlanQueryChange: (String) -> Unit,
    selectedPlanId: Long?,
    onSelectPlan: (Long) -> Unit,
    customWorkoutName: String,
    onCustomWorkoutNameChange: (String) -> Unit,
    selectedDay: WorkoutDayOfWeek,
    onSelectDay: (WorkoutDayOfWeek) -> Unit,
    selectedSessionType: WorkoutSessionType,
    onSelectSessionType: (WorkoutSessionType) -> Unit,
    availableExercises: List<Exercise>,
    isLoadingExercises: Boolean,
    exercisesError: String?,
    onRetryLoadExercises: () -> Unit,
    exerciseSearchQuery: String,
    onExerciseSearchChange: (String) -> Unit,
    customExercises: List<ExerciseDraftEntry>,
    onAddExercise: (Exercise) -> Unit,
    onUpdateExercise: (Int, ExerciseDraftEntry) -> Unit,
    onRemoveExercise: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selected Client Banner
        if (selectedClient != null) {
            item {
                SelectedClientBanner(
                    client = selectedClient,
                    actionLabel = "Change",
                    onActionClick = onChangeClient
                )
            }
        }

        // Mode Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = mode == WorkoutSelectionMode.EXISTING_PLAN,
                    onClick = { onModeChange(WorkoutSelectionMode.EXISTING_PLAN) },
                    label = { Text("Existing Workout (${plans.size})") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent.copy(alpha = 0.16f),
                        selectedLabelColor = Accent
                    )
                )
                FilterChip(
                    selected = mode == WorkoutSelectionMode.BUILD_FROM_EXERCISES,
                    onClick = { onModeChange(WorkoutSelectionMode.BUILD_FROM_EXERCISES) },
                    label = { Text("Build with Exercises") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent.copy(alpha = 0.16f),
                        selectedLabelColor = Accent
                    )
                )
            }
        }

        if (mode == WorkoutSelectionMode.EXISTING_PLAN) {
            item {
                OutlinedTextField(
                    value = planQuery,
                    onValueChange = onPlanQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SecondaryText) },
                    placeholder = { Text("Search your workout plans...") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            val filteredPlans = plans.filter {
                planQuery.isBlank() ||
                    it.name.contains(planQuery.trim(), ignoreCase = true) ||
                    it.splitType.displayName.contains(planQuery.trim(), ignoreCase = true)
            }
            if (plans.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardBackground,
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FitnessCenter,
                                contentDescription = null,
                                tint = SecondaryText,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Saved Workouts Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Build a multi-exercise workout from your exercise library right here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                            OutlinedButton(onClick = { onModeChange(WorkoutSelectionMode.BUILD_FROM_EXERCISES) }) {
                                Text("Build with Exercises")
                            }
                        }
                    }
                }
            } else if (filteredPlans.isEmpty()) {
                item {
                    Text(
                        text = "No workout plans match \"$planQuery\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(filteredPlans, key = { it.id }) { plan ->
                    val isSelected = selectedPlanId == plan.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectPlan(plan.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Accent.copy(alpha = 0.14f) else CardBackground,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Accent else Border
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = plan.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryText
                                )
                                Text(
                                    text = "${plan.splitType.displayName} • ${plan.trainingDayCount} training days • ${plan.totalExerciseCount} exercises",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Accent
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Build with Exercises mode
            item {
                OutlinedTextField(
                    value = customWorkoutName,
                    onValueChange = onCustomWorkoutNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Workout Title *") },
                    placeholder = { Text("e.g., Upper Body Strength Day") },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Scheduled Day & Session Focus",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorkoutDayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day == selectedDay,
                                onClick = { onSelectDay(day) },
                                label = { Text(day.shortName) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            WorkoutSessionType.FULL_BODY,
                            WorkoutSessionType.PUSH,
                            WorkoutSessionType.PULL,
                            WorkoutSessionType.LEGS,
                            WorkoutSessionType.UPPER,
                            WorkoutSessionType.LOWER,
                            WorkoutSessionType.SKILL_TRAINING,
                            WorkoutSessionType.STRENGTH
                        ).forEach { type ->
                            FilterChip(
                                selected = type == selectedSessionType,
                                onClick = { onSelectSessionType(type) },
                                label = { Text(type.displayName) }
                            )
                        }
                    }
                }
            }

            // Configured Exercises Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Selected Exercises (${customExercises.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    if (customExercises.isEmpty()) {
                        Text(
                            text = "Add at least 1 exercise below",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                    }
                }
            }

            if (customExercises.isNotEmpty()) {
                itemsIndexed(customExercises, key = { _, item -> item.uid }) { index, item ->
                    ConfiguredExerciseCard(
                        index = index,
                        entry = item,
                        onUpdate = { updated -> onUpdateExercise(index, updated) },
                        onRemove = { onRemoveExercise(index) }
                    )
                }
            }

            // Exercise Catalog Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Exercise Catalog",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    OutlinedTextField(
                        value = exerciseSearchQuery,
                        onValueChange = onExerciseSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SecondaryText) },
                        placeholder = { Text("Search exercises by name or muscle...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            when {
                isLoadingExercises -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Accent)
                        }
                    }
                }

                !exercisesError.isNullOrBlank() -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardBackground,
                            border = BorderStroke(1.dp, ThemeError.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = exercisesError,
                                    color = ThemeError,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRetryLoadExercises) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                availableExercises.isEmpty() -> {
                    item {
                        Text(
                            text = "No exercises available in catalog yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                else -> {
                    val clean = exerciseSearchQuery.trim()
                    val filteredCatalog = if (clean.isEmpty()) {
                        availableExercises
                    } else {
                        availableExercises.filter {
                            it.name.contains(clean, ignoreCase = true) ||
                                it.targetMuscles.contains(clean, ignoreCase = true) ||
                                it.category.name.contains(clean, ignoreCase = true)
                        }
                    }
                    if (filteredCatalog.isEmpty()) {
                        item {
                            Text(
                                text = "No exercises match \"$clean\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    } else {
                        items(filteredCatalog, key = { it.id }) { exercise ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardBackground,
                                border = BorderStroke(1.dp, Border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryText
                                        )
                                        Text(
                                            text = "${exercise.category.name} • ${exercise.targetMuscles}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onAddExercise(exercise) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = "Add ${exercise.name}",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add")
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
private fun ConfiguredExerciseCard(
    index: Int,
    entry: ExerciseDraftEntry,
    onUpdate: (ExerciseDraftEntry) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${index + 1}. ${entry.exercise.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove exercise",
                        tint = ThemeError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !entry.useDuration,
                    onClick = { onUpdate(entry.copy(useDuration = false)) },
                    label = { Text("Reps") }
                )
                FilterChip(
                    selected = entry.useDuration,
                    onClick = { onUpdate(entry.copy(useDuration = true)) },
                    label = { Text("Hold (sec)") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = entry.setsText,
                    onValueChange = { onUpdate(entry.copy(setsText = it.filter { ch -> ch.isDigit() })) },
                    label = { Text("Sets") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                if (entry.useDuration) {
                    OutlinedTextField(
                        value = entry.durationText,
                        onValueChange = { onUpdate(entry.copy(durationText = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Sec") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = entry.repsText,
                        onValueChange = { onUpdate(entry.copy(repsText = it.filter { ch -> ch.isDigit() })) },
                        label = { Text("Reps") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                OutlinedTextField(
                    value = entry.restSecondsText,
                    onValueChange = { onUpdate(entry.copy(restSecondsText = it.filter { ch -> ch.isDigit() })) },
                    label = { Text("Rest (s)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            OutlinedTextField(
                value = entry.notes,
                onValueChange = { onUpdate(entry.copy(notes = it)) },
                label = { Text("Exercise notes / tempo (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
private fun StepReviewAndAssignContent(
    selectedClient: CoachClientRelationship?,
    workoutMode: WorkoutSelectionMode,
    selectedPlan: UserWorkoutPlan?,
    customWorkoutName: String,
    selectedDay: WorkoutDayOfWeek,
    selectedSessionType: WorkoutSessionType,
    configuredExercises: List<ExerciseDraftEntry>,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    scheduleValidationError: String?,
    explicitConfirmed: Boolean,
    onConfirmedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedClient != null) {
            item {
                SelectedClientBanner(
                    client = selectedClient,
                    actionLabel = null,
                    onActionClick = {}
                )
            }
        }

        // Selected Workout / Exercises Summary
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Workout Summary",
                        style = MaterialTheme.typography.labelLarge,
                        color = SecondaryText
                    )
                    if (workoutMode == WorkoutSelectionMode.EXISTING_PLAN && selectedPlan != null) {
                        Text(
                            text = selectedPlan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = "${selectedPlan.splitType.displayName} • ${selectedPlan.trainingDayCount} training days • ${selectedPlan.totalExerciseCount} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        val activeDays = selectedPlan.days.filter { !it.isRestDay && !it.sessionType.isRest }
                        if (activeDays.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                activeDays.take(4).forEach { day ->
                                    Text(
                                        text = "• ${day.dayOfWeek.shortName}: ${day.name?.takeIf { it.isNotBlank() } ?: day.sessionType.displayName} (${day.exercises.size} exercises)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PrimaryText
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = customWorkoutName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = "${selectedDay.displayName} • ${selectedSessionType.displayName} • ${configuredExercises.size} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        configuredExercises.forEachIndexed { idx, entry ->
                            val config = entry.toConfiguredOrNull()
                            val rx = if (config?.durationSeconds != null) {
                                "${config.sets} sets × ${config.durationSeconds}s"
                            } else {
                                "${config?.sets ?: entry.setsText} sets × ${config?.reps ?: entry.repsText} reps"
                            }
                            Text(
                                text = "${idx + 1}. ${entry.exercise.name} — $rx (${config?.restSeconds ?: entry.restSecondsText}s rest)",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryText
                            )
                            if (entry.notes.isNotBlank()) {
                                Text(
                                    text = "   Note: ${entry.notes.trim()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Schedule Section (Start Date + Deadline)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Schedule & Deadline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                    }

                    OutlinedTextField(
                        value = startDate,
                        onValueChange = onStartDateChange,
                        enabled = enabled,
                        singleLine = true,
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val today = LocalDate.now()
                        val tomorrow = today.plusDays(1)
                        val nextMon = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                        FilterChip(
                            selected = startDate.trim() == today.toString(),
                            onClick = { onStartDateChange(today.toString()) },
                            label = { Text("Today") }
                        )
                        FilterChip(
                            selected = startDate.trim() == tomorrow.toString(),
                            onClick = { onStartDateChange(tomorrow.toString()) },
                            label = { Text("Tomorrow") }
                        )
                        FilterChip(
                            selected = startDate.trim() == nextMon.toString(),
                            onClick = { onStartDateChange(nextMon.toString()) },
                            label = { Text("Next Monday") }
                        )
                    }

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = onEndDateChange,
                        enabled = enabled,
                        singleLine = true,
                        label = { Text("Deadline (Optional, YYYY-MM-DD)") },
                        placeholder = { Text("Leave blank for open-ended") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val baseDate = runCatching { LocalDate.parse(startDate.trim()) }.getOrDefault(LocalDate.now())
                        FilterChip(
                            selected = endDate.isBlank(),
                            onClick = { onEndDateChange("") },
                            label = { Text("No Deadline") }
                        )
                        FilterChip(
                            selected = endDate.trim() == baseDate.plusWeeks(2).toString(),
                            onClick = { onEndDateChange(baseDate.plusWeeks(2).toString()) },
                            label = { Text("+2 Weeks") }
                        )
                        FilterChip(
                            selected = endDate.trim() == baseDate.plusWeeks(4).toString(),
                            onClick = { onEndDateChange(baseDate.plusWeeks(4).toString()) },
                            label = { Text("+4 Weeks") }
                        )
                    }

                    if (!scheduleValidationError.isNullOrBlank()) {
                        Text(
                            text = scheduleValidationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeError
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        enabled = enabled,
                        minLines = 2,
                        maxLines = 4,
                        label = { Text("Message to Client (Optional)") },
                        placeholder = { Text("Add coaching instructions, goals, or check-in notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Explicit Confirmation Toggle
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { onConfirmedChange(!explicitConfirmed) },
                shape = RoundedCornerShape(12.dp),
                color = if (explicitConfirmed) Accent.copy(alpha = 0.12f) else CardBackground,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (explicitConfirmed) Accent else Border
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = explicitConfirmed,
                        onCheckedChange = { if (enabled) onConfirmedChange(it) },
                        colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Ink)
                    )
                    Text(
                        text = "I confirm assigning this workout to ${selectedClient?.clientName ?: "this client"}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedClientBanner(
    client: CoachClientRelationship,
    actionLabel: String?,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Accent.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UserAvatar(url = client.clientAvatarUrl, modifier = Modifier.size(38.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Assigned Client",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent
                )
                Text(
                    text = client.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
            }
            if (!actionLabel.isNullOrBlank()) {
                TextButton(onClick = onActionClick) {
                    Text(actionLabel, color = Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun validateAssignmentDates(startDate: String, endDate: String): String? {
    val cleanStart = startDate.trim()
    val cleanEnd = endDate.trim()
    val parsedStart = if (cleanStart.isNotEmpty()) {
        runCatching { LocalDate.parse(cleanStart) }.getOrElse {
            return "Start date must use YYYY-MM-DD format."
        }
    } else null
    val parsedEnd = if (cleanEnd.isNotEmpty()) {
        runCatching { LocalDate.parse(cleanEnd) }.getOrElse {
            return "Deadline must use YYYY-MM-DD format."
        }
    } else null
    if (parsedStart != null && parsedEnd != null && parsedEnd.isBefore(parsedStart)) {
        return "Deadline cannot be earlier than start date."
    }
    return null
}
