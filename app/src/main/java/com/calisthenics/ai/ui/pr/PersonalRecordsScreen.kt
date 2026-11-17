package com.calisthenics.ai.ui.pr

import com.calisthenics.ai.R

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
import androidx.compose.ui.res.stringResource
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
                        stringResource(R.string.personal_records),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = PrimaryText)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh), tint = PrimaryText)
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_pr))
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
                        title = stringResource(R.string.no_personal_records),
                        body = stringResource(R.string.loc_log_pr_description),
                        action = stringResource(R.string.loc_add_personal_record),
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
                                val label = if (filter == "ALL") stringResource(R.string.loc_all_prs) else filter.replace("MAX_", "").replace('_', ' ')
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
                                unit = state.chartHistory.firstOrNull()?.unit ?: stringResource(R.string.loc_units),
                                title = stringResource(R.string.loc_progression_title, state.selectedChartExerciseName ?: stringResource(R.string.exercise))
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
                                    PillBadge(stringResource(R.string.loc_prs_count, records.size), isAccent = true)
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
                                            contentDescription = stringResource(R.string.view_chart),
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
                                onShare = { viewModel.openShareSheet(pr) }
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
            title = { Text(stringResource(R.string.delete_personal_record), color = PrimaryText) },
            text = {
                Text(
                    stringResource(R.string.loc_delete_pr_message, pr.displayFormattedValue, pr.exerciseName),
                    color = SecondaryText
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text(stringResource(R.string.cancel), color = SecondaryText)
                }
            },
            containerColor = Elevated
        )
    }

    // Share PR Recipient Modal Bottom Sheet
    state.sharingPr?.let { prToShare ->
        val alreadySharedIds = remember(state.sharedPrs, prToShare.id) {
            state.sharedPrs
                .filter { it.personalRecord.id == prToShare.id }
                .map { it.coachUserId }
                .toSet()
        }
        SharePrBottomSheet(
            pr = prToShare,
            recipients = state.shareRecipients,
            alreadySharedRecipientIds = alreadySharedIds,
            isLoadingRecipients = state.isLoadingRecipients,
            isSharing = state.isSharingPr,
            errorMessage = state.shareError,
            onDismiss = { viewModel.dismissShareSheet() },
            onConfirmShare = { recipientId ->
                viewModel.shareWithCoach(prToShare.id, recipientId)
            }
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
                        tint = Warning,
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
                    stringResource(R.string.loc_achieved_date, pr.achievedAt.take(10)),
                    fontSize = 12.sp,
                    color = SecondaryText
                )
                if (pr.workoutSessionId != null) {
                    Text(stringResource(R.string.from_workout_session), fontSize = 11.sp, color = SecondaryText)
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
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.common_share), modifier = Modifier.size(16.dp), tint = SecondaryText)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.common_share), fontSize = 12.sp, color = SecondaryText)
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit), modifier = Modifier.size(16.dp), tint = SecondaryText)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.common_edit), fontSize = 12.sp, color = SecondaryText)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.common_delete), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
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
                    text = if (editingPr != null) stringResource(R.string.loc_edit_personal_record) else stringResource(R.string.loc_new_personal_record),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )

                // Exercise selection / entry
                Column {
                    Text(stringResource(R.string.exercise), fontSize = 12.sp, color = SecondaryText, fontWeight = FontWeight.SemiBold)
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
                            placeholder = { Text(stringResource(R.string.choose_or_type_exercise), color = SecondaryText) },
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
                    Text(stringResource(R.string.pr_type), fontSize = 12.sp, color = SecondaryText, fontWeight = FontWeight.SemiBold)
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
                        label = { Text(stringResource(R.string.value)) },
                        placeholder = { Text(stringResource(R.string.loc_example_40)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitInput,
                        onValueChange = { unitInput = it },
                        label = { Text(stringResource(R.string.unit)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Date input
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text(stringResource(R.string.achieved_date)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Optional Notes
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(stringResource(R.string.notes_optional)) },
                    placeholder = { Text(stringResource(R.string.loc_example_pr_notes)) },
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
                        Text(stringResource(R.string.cancel), color = SecondaryText)
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
                            Text(if (editingPr != null) stringResource(R.string.loc_update_pr) else stringResource(R.string.loc_save_pr))
                        }
                    }
                }
            }
        }
    }
}
