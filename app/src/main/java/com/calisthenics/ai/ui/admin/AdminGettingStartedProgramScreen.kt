package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AdminExerciseDraft
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGettingStartedProgramScreen(
    onBack: () -> Unit,
    adminNavigation: AdminNavigationCallbacks = AdminNavigationCallbacks(onOverview = onBack),
    viewModel: AdminGettingStartedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showExerciseDialog by rememberSaveable { mutableStateOf(false) }
    var editingExerciseIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val currentDay = state.draft.days.getOrNull(state.selectedDayIndex)

    AdminConsoleScaffold(
        currentSection = AdminNavSection.GETTING_STARTED,
        title = "Getting Started Program",
        subtitle = "3-day onboarding program & audience campaign",
        navigation = adminNavigation,
        onBack = onBack,
        headerActions = {
            if (state.program != null) {
                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Program", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) {
    Scaffold(
        containerColor = Ink,
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = viewModel::saveProgram,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Save Program", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications / Messages
            if (state.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::dismissMessages) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            if (state.successMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.successMessage!!,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::dismissMessages) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            // Publication Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Publication Status", style = MaterialTheme.typography.labelMedium)
                                val isPublished = state.program?.published == true
                                val statusText = when {
                                    state.program == null -> "NOT SAVED"
                                    isPublished -> "PUBLISHED (v${state.program!!.version})"
                                    else -> "DRAFT / UNPUBLISHED"
                                }
                                val statusColor = when {
                                    state.program == null -> MaterialTheme.colorScheme.outline
                                    isPublished -> Color(0xFF4CAF50)
                                    else -> Color(0xFFFFA000)
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            if (state.program != null) {
                                if (state.program!!.published) {
                                    OutlinedButton(
                                        onClick = viewModel::unpublishProgram,
                                        enabled = !state.isSaving
                                    ) {
                                        Text("Unpublish")
                                    }
                                } else {
                                    Button(
                                        onClick = viewModel::publishProgram,
                                        enabled = !state.isSaving
                                    ) {
                                        Text("Publish")
                                    }
                                }
                            }
                        }

                        if (state.program?.creatorName != null) {
                            Text(
                                text = "Created by: ${state.program!!.creatorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val audienceLabel = when (state.program?.targetAudience) {
                            com.calisthenics.ai.domain.model.TargetAudience.LAST_14_DAYS -> "Joined within last 14 days"
                            com.calisthenics.ai.domain.model.TargetAudience.LAST_30_DAYS -> "Joined within last 30 days"
                            else -> "All Athletes"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Audience: $audienceLabel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Audience Targeting Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Target Audience", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Select which athlete accounts will automatically receive this onboarding program.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.draft.targetAudience == com.calisthenics.ai.domain.model.TargetAudience.ALL_ATHLETES,
                                onClick = { viewModel.updateTargetAudience(com.calisthenics.ai.domain.model.TargetAudience.ALL_ATHLETES) },
                                label = { Text("All Athletes") }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.draft.targetAudience == com.calisthenics.ai.domain.model.TargetAudience.LAST_30_DAYS,
                                onClick = { viewModel.updateTargetAudience(com.calisthenics.ai.domain.model.TargetAudience.LAST_30_DAYS) },
                                label = { Text("Joined within 30 days") }
                            )
                            FilterChip(
                                selected = state.draft.targetAudience == com.calisthenics.ai.domain.model.TargetAudience.LAST_14_DAYS,
                                onClick = { viewModel.updateTargetAudience(com.calisthenics.ai.domain.model.TargetAudience.LAST_14_DAYS) },
                                label = { Text("Joined within 14 days") }
                            )
                        }
                    }
                }
            }

            // Program Overview Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Program Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = state.draft.title,
                            onValueChange = viewModel::updateProgramTitle,
                            label = { Text("Program Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.draft.description,
                            onValueChange = viewModel::updateProgramDescription,
                            label = { Text("Program Description") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }
            }

            // 3-Day Tabs
            item {
                TabRow(
                    selectedTabIndex = state.selectedDayIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    (0..2).forEach { index ->
                        Tab(
                            selected = state.selectedDayIndex == index,
                            onClick = { viewModel.selectDay(index) },
                            text = { Text("Day ${index + 1}", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            // Current Day Configuration
            if (currentDay != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Day ${currentDay.dayNumber} Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = currentDay.title,
                                onValueChange = { viewModel.updateDayTitle(state.selectedDayIndex, it) },
                                label = { Text("Day Title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentDay.description,
                                onValueChange = { viewModel.updateDayDescription(state.selectedDayIndex, it) },
                                label = { Text("Day Focus / Description") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // Exercises Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exercises (${currentDay.exercises.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = {
                                editingExerciseIndex = null
                                showExerciseDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Exercise")
                        }
                    }
                }

                // Exercises List
                if (currentDay.exercises.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No exercises added yet for Day ${currentDay.dayNumber}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    itemsIndexed(currentDay.exercises) { index, exercise ->
                        AdminExerciseItemCard(
                            index = index,
                            exercise = exercise,
                            onEdit = {
                                editingExerciseIndex = index
                                showExerciseDialog = true
                            },
                            onDelete = {
                                viewModel.removeExercise(state.selectedDayIndex, index)
                            }
                        )
                    }
                }
            }
        }
    }
    }

    if (showExerciseDialog && currentDay != null) {
        val initialExercise = editingExerciseIndex?.let { currentDay.exercises.getOrNull(it) }
        ExerciseEditDialog(
            initial = initialExercise,
            availableExercises = state.availableExercises,
            onDismiss = { showExerciseDialog = false },
            onConfirm = { editedDraft ->
                viewModel.addOrUpdateExercise(state.selectedDayIndex, editedDraft, editingExerciseIndex)
                showExerciseDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Program?") },
            text = { Text("This will permanently remove the Getting Started program and unpublish it for all athletes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProgram()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminExerciseItemCard(
    index: Int,
    exercise: AdminExerciseDraft,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val targetText = if (exercise.durationSeconds != null && exercise.durationSeconds > 0) {
                    "${exercise.sets} sets × ${exercise.durationSeconds}s"
                } else {
                    "${exercise.sets} sets × ${exercise.reps ?: 10} reps"
                }
                Text(
                    text = "$targetText • ${exercise.restSeconds}s rest",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (exercise.instructions.isNotBlank()) {
                    Text(
                        text = exercise.instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseEditDialog(
    initial: AdminExerciseDraft?,
    availableExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (AdminExerciseDraft) -> Unit
) {
    var selectedExerciseId by remember { mutableStateOf(initial?.exerciseId) }
    var exerciseName by remember { mutableStateOf(initial?.exerciseName ?: "") }
    var sets by remember { mutableStateOf(initial?.sets?.toString() ?: "3") }
    var reps by remember { mutableStateOf(initial?.reps?.toString() ?: "10") }
    var durationSeconds by remember { mutableStateOf(initial?.durationSeconds?.toString() ?: "") }
    var restSeconds by remember { mutableStateOf(initial?.restSeconds?.toString() ?: "60") }
    var instructions by remember { mutableStateOf(initial?.instructions ?: "") }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Exercise" else "Edit Exercise", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Exercise selection from library
                if (availableExercises.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = exerciseName,
                            onValueChange = {
                                exerciseName = it
                                selectedExerciseId = null
                            },
                            label = { Text("Exercise Name / Library") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            availableExercises.take(15).forEach { ex ->
                                DropdownMenuItem(
                                    text = { Text(ex.name) },
                                    onClick = {
                                        exerciseName = ex.name
                                        selectedExerciseId = ex.id
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Sets") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationSeconds,
                        onValueChange = { durationSeconds = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Secs") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Rest (seconds)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (exerciseName.isNotBlank()) {
                        val parsedSets = sets.toIntOrNull() ?: 3
                        val parsedReps = reps.toIntOrNull()
                        val parsedDuration = durationSeconds.toIntOrNull()
                        val parsedRest = restSeconds.toIntOrNull() ?: 60

                        val draft = AdminExerciseDraft(
                            exerciseId = selectedExerciseId,
                            exerciseName = exerciseName.trim(),
                            sets = parsedSets,
                            reps = parsedReps,
                            durationSeconds = parsedDuration,
                            restSeconds = parsedRest,
                            instructions = instructions.trim(),
                            displayOrder = initial?.displayOrder ?: 0
                        )
                        onConfirm(draft)
                    }
                },
                enabled = exerciseName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
