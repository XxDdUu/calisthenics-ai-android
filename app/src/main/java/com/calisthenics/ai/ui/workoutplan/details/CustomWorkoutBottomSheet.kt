package com.calisthenics.ai.ui.workoutplan.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType

private val selectableWorkoutTypes = listOf(
    WorkoutSessionType.PUSH,
    WorkoutSessionType.PULL,
    WorkoutSessionType.LEGS,
    WorkoutSessionType.UPPER,
    WorkoutSessionType.LOWER,
    WorkoutSessionType.FULL_BODY,
    WorkoutSessionType.BENT_ARM,
    WorkoutSessionType.STRAIGHT_ARM,
    WorkoutSessionType.PLANCHE,
    WorkoutSessionType.FRONT_LEVER,
    WorkoutSessionType.HANDSTAND,
    WorkoutSessionType.SKILL_TRAINING,
    WorkoutSessionType.STRENGTH,
    WorkoutSessionType.HYPERTROPHY,
    WorkoutSessionType.MOBILITY,
    WorkoutSessionType.CONDITIONING,
    WorkoutSessionType.OTHER
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWorkoutBottomSheet(
    editor: CustomWorkoutEditorState,
    unavailableDays: Set<WorkoutDayOfWeek>,
    isConfirmingDelete: Boolean,
    onNameChange: (String) -> Unit,
    onTypeChange: (WorkoutSessionType?) -> Unit,
    onTrainingFocusChange: (String) -> Unit,
    onDayToggle: (WorkoutDayOfWeek) -> Unit,
    onRestDayChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!editor.isSaving) onCancel() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (editor.sourceDay == null) "Custom workout" else "Edit workout",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rest Day", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "No workout is scheduled on the selected day(s).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = editor.isRestDay, onCheckedChange = onRestDayChange)
                    }

                    if (!editor.isRestDay) {
                        OutlinedTextField(
                            value = editor.name,
                            onValueChange = onNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Workout name") },
                            placeholder = { Text("e.g. Planche + Push") },
                            singleLine = true,
                            isError = editor.validationError == "Workout name is required.",
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Workout type (optional)", style = MaterialTheme.typography.titleSmall)
                            selectableWorkoutTypes.chunked(3).forEach { rowTypes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowTypes.forEach { type ->
                                        FilterChip(
                                            selected = editor.workoutType == type,
                                            onClick = {
                                                onTypeChange(if (editor.workoutType == type) null else type)
                                            },
                                            label = { Text(type.displayName) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - rowTypes.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editor.trainingFocus,
                            onValueChange = onTrainingFocusChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Training focus (optional)") },
                            placeholder = {
                                Text("Describe your training focus...")
                            },
                            minLines = 3,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { keyboardController?.hide() }
                            )
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Assign to day(s)", style = MaterialTheme.typography.titleSmall)
                        WorkoutDayOfWeek.entries.chunked(4).forEach { rowDays ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowDays.forEach { day ->
                                    val unavailable = day in unavailableDays && day !in editor.selectedDays
                                    FilterChip(
                                        selected = day in editor.selectedDays,
                                        onClick = { onDayToggle(day) },
                                        enabled = !unavailable && !editor.isSaving,
                                        label = { Text(day.shortName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(4 - rowDays.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        Text(
                            "Days that already have a workout must be edited before they can be reassigned.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            editor.validationError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (editor.sourceDay != null && !editor.isRestDay) {
                TextButton(
                    onClick = onRequestDelete,
                    enabled = !editor.isSaving,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Remove workout", color = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        onCancel()
                    },
                    enabled = !editor.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSave()
                    },
                    enabled = !editor.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (editor.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Workout")
                    }
                }
            }
        }
    }

    if (isConfirmingDelete) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Remove this workout?") },
            text = { Text("The workout assignment will be removed from its selected day(s).") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) { Text("Cancel") }
            }
        )
    }
}
