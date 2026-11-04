package com.calisthenics.ai.ui.pr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.data.remote.dto.PersonalRecordDto
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.InteractiveProgressChart
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.theme.*
import java.time.LocalDate

private val GoldStar = Color(0xFFFFD700)
private val PR_TYPES = listOf(
    "MAX_WEIGHT" to "kg",
    "MAX_REPS" to "reps",
    "MAX_HOLD" to "seconds",
    "MAX_DISTANCE" to "km",
    "MAX_DURATION" to "seconds",
    "BODYWEIGHT" to "reps"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalRecordsScreen(
    onBack: () -> Unit,
    viewModel: PersonalRecordsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Personal Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddPrForm() },
                containerColor = Accent,
                contentColor = Ink
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add PR")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Ink
    ) { padding ->
        when {
            state.isLoading && state.prs.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            state.prs.isEmpty() && !state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding)) {
                    EmptyState(
                        title = "No Personal Records Yet",
                        body = "Log your best calisthenics feats, weighted records, and isometric holds.",
                        action = "Add Personal Record",
                        onAction = { viewModel.openAddPrForm() }
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Type filter chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filterOptions = listOf("ALL") + PR_TYPES.map { it.first }
                            filterOptions.forEach { filter ->
                                val label = if (filter == "ALL") "All PRs" else filter.replace("MAX_", "").replace('_', ' ')
                                FilterChip(
                                    selected = state.selectedPrTypeFilter.equals(filter, ignoreCase = true),
                                    onClick = { viewModel.selectPrTypeFilter(filter) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // PR Progression Chart (if exercise selected or available)
                    if (state.chartDataPoints.isNotEmpty()) {
                        item {
                            InteractiveProgressChart(
                                dataPoints = state.chartDataPoints,
                                selectedFilter = state.chartFilter,
                                onFilterSelected = { viewModel.setChartFilter(it) },
                                unit = state.chartHistory.firstOrNull()?.unit ?: "units",
                                title = "${state.selectedChartExerciseName ?: "Exercise"} Progression"
                            )
                        }
                    }

                    // PRs grouped by exercise
                    state.prsGroupedByExercise.forEach { (exerciseName, records) ->
                        item(key = "header_$exerciseName") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    exerciseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PillBadge("${records.size} PRs", isAccent = true)
                                    IconButton(
                                        onClick = {
                                            viewModel.selectChartExercise(
                                                exerciseName,
                                                records.firstOrNull()?.exerciseId
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ShowChart,
                                            contentDescription = "View Chart",
                                            tint = if (state.selectedChartExerciseName == exerciseName) Accent else SecondaryText
                                        )
                                    }
                                }
                            }
                        }

                        items(records, key = { it.id }) { pr ->
                            PrCard(
                                pr = pr,
                                onEdit = { viewModel.openEditPrForm(pr) },
                                onDelete = { viewModel.promptDelete(pr) },
                                onShare = { viewModel.shareWithCoach(pr.id) }
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit PR Dialog Form
    if (state.isFormOpen) {
        PrFormDialog(
            editingPr = state.editingPr,
            exercises = state.exercises,
            isSaving = state.isSaving,
            onDismiss = { viewModel.closeForm() },
            onSave = { exId, exName, type, value, unit, date, notes ->
                viewModel.savePr(exId, exName, type, value, unit, date, notes)
            }
        )
    }

    // Delete Confirmation Dialog
    state.deletingPr?.let { pr ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Personal Record", color = PrimaryText) },
            text = {
                Text(
                    "Are you sure you want to delete this PR (${pr.displayFormattedValue} for ${pr.exerciseName})? This cannot be undone.",
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel", color = SecondaryText)
                }
            },
            containerColor = Elevated
        )
    }
}

@Composable
private fun PrCard(
    pr: PersonalRecordDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    DarkCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        contentDescription = null,
                        tint = GoldStar,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        pr.displayFormattedValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )
                    pr.improvementPercentage?.let { pct ->
                        if (pct > 0) {
                            Text(
                                "+${pct}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Accent,
                                modifier = Modifier
                                    .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                PillBadge(
                    pr.recordType.replace("MAX_", "").replace('_', ' ').ifEmpty { "PR" },
                    isAccent = false
                )
            }

            // Achieved Date & Session info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Achieved: ${pr.achievedAt.take(10)}",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
                if (pr.workoutSessionId != null) {
                    Text("from workout session", fontSize = 11.sp, color = SecondaryText)
                }
            }

            // Optional Notes
            pr.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    notes,
                    fontSize = 12.sp,
                    color = PrimaryText.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Ink, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp), tint = SecondaryText)
                    Spacer(Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp, color = SecondaryText)
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = SecondaryText)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp, color = SecondaryText)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrFormDialog(
    editingPr: PersonalRecordDto?,
    exercises: List<Exercise>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, Double, String, String, String?) -> Unit
) {
    var selectedExerciseId by remember { mutableStateOf(editingPr?.exerciseId) }
    var exerciseNameInput by remember { mutableStateOf(editingPr?.exerciseName ?: "") }
    var selectedPrType by remember {
        mutableStateOf(editingPr?.recordType?.ifBlank { "MAX_WEIGHT" } ?: "MAX_WEIGHT")
    }
    var valueInput by remember {
        mutableStateOf(if (editingPr != null) editingPr.recordValue.toString() else "")
    }
    var unitInput by remember {
        val initialUnit = editingPr?.unit ?: PR_TYPES.find { it.first == selectedPrType }?.second ?: "kg"
        mutableStateOf(initialUnit)
    }
    var dateInput by remember {
        mutableStateOf(editingPr?.achievedAt?.take(10) ?: LocalDate.now().toString())
    }
    var notesInput by remember { mutableStateOf(editingPr?.notes ?: "") }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (editingPr != null) "Edit Personal Record" else "New Personal Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                // Exercise selection / entry
                Column {
                    Text("Exercise", fontSize = 12.sp, color = SecondaryText, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = exerciseDropdownExpanded,
                        onExpandedChange = { exerciseDropdownExpanded = !exerciseDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = exerciseNameInput,
                            onValueChange = {
                                exerciseNameInput = it
                                selectedExerciseId = exercises.find { ex -> ex.name.equals(it, ignoreCase = true) }?.id
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            placeholder = { Text("Choose or type exercise name", color = SecondaryText) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exerciseDropdownExpanded) },
                            singleLine = true
                        )
                        if (exercises.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = exerciseDropdownExpanded,
                                onDismissRequest = { exerciseDropdownExpanded = false }
                            ) {
                                exercises.forEach { ex ->
                                    DropdownMenuItem(
                                        text = { Text(ex.name) },
                                        onClick = {
                                            selectedExerciseId = ex.id
                                            exerciseNameInput = ex.name
                                            exerciseDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // PR Type selection
                Column {
                    Text("PR Type", fontSize = 12.sp, color = SecondaryText, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PR_TYPES.forEach { (type, defaultUnit) ->
                            val isSelected = selectedPrType.equals(type, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPrType = type
                                    unitInput = defaultUnit
                                },
                                label = { Text(type.replace("MAX_", "").replace('_', ' '), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Value and Unit Inputs Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = valueInput,
                        onValueChange = { valueInput = it },
                        label = { Text("Value") },
                        placeholder = { Text("e.g. 40") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitInput,
                        onValueChange = { unitInput = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Date input
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Achieved Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Optional Notes
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. Strict form, +40kg dumbbell") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text("Cancel", color = SecondaryText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val v = valueInput.toDoubleOrNull() ?: 0.0
                            val isoDate = if (dateInput.length == 10) "${dateInput}T00:00:00Z" else dateInput
                            onSave(
                                selectedExerciseId,
                                exerciseNameInput,
                                selectedPrType,
                                v,
                                unitInput,
                                isoDate,
                                notesInput.ifBlank { null }
                            )
                        },
                        enabled = !isSaving && valueInput.toDoubleOrNull() != null && exerciseNameInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                        } else {
                            Text(if (editingPr != null) "Update PR" else "Save PR")
                        }
                    }
                }
            }
        }
    }
}

