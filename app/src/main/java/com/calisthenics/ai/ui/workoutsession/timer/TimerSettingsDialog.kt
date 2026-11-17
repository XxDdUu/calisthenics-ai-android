package com.calisthenics.ai.ui.workoutsession.timer

import com.calisthenics.ai.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerSettingsDialog(
    exerciseName: String,
    initialConfig: ExerciseTimerConfig,
    onDismiss: () -> Unit,
    onSave: (ExerciseTimerConfig) -> Unit
) {
    var countdownEnabled by remember { mutableStateOf(initialConfig.countdownEnabled) }
    var countdownDuration by remember { mutableStateOf(initialConfig.countdownDurationSeconds) }
    var customCountdownText by remember {
        mutableStateOf(
            if (initialConfig.countdownDurationSeconds !in listOf(3, 5, 10)) {
                initialConfig.countdownDurationSeconds.toString()
            } else ""
        )
    }

    var restEnabled by remember { mutableStateOf(initialConfig.restTimerEnabled) }
    var restDuration by remember { mutableStateOf(initialConfig.restDurationSeconds) }
    var customRestText by remember {
        mutableStateOf(
            if (initialConfig.restDurationSeconds !in listOf(30, 60, 90, 120, 180)) {
                initialConfig.restDurationSeconds.toString()
            } else ""
        )
    }

    var soundEnabled by remember { mutableStateOf(initialConfig.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(initialConfig.vibrationEnabled) }

    val countdownPresets = listOf(3, 5, 10)
    val restPresets = listOf(30, 60, 90, 120, 180)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.loc_timer_settings_exercise, exerciseName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Countdown Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.loc_countdown_before_hold),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.loc_get_ready_before_timing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = countdownEnabled,
                            onCheckedChange = { countdownEnabled = it }
                        )
                    }

                    if (countdownEnabled) {
                        Text(stringResource(R.string.loc_duration), style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            countdownPresets.forEach { preset ->
                                FilterChip(
                                    selected = countdownDuration == preset && customCountdownText.isBlank(),
                                    onClick = {
                                        countdownDuration = preset
                                        customCountdownText = ""
                                    },
                                    label = { Text(stringResource(R.string.loc_seconds_short, preset)) }
                                )
                            }
                            FilterChip(
                                selected = customCountdownText.isNotBlank() || countdownDuration !in countdownPresets,
                                onClick = {
                                    if (customCountdownText.isBlank()) {
                                        customCountdownText = countdownDuration.toString()
                                    }
                                },
                                label = { Text(stringResource(R.string.loc_custom)) }
                            )
                        }

                        if (customCountdownText.isNotBlank() || countdownDuration !in countdownPresets) {
                            OutlinedTextField(
                                value = customCountdownText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(3)
                                    customCountdownText = filtered
                                    filtered.toIntOrNull()?.let {
                                        if (it in 1..60) countdownDuration = it
                                    }
                                },
                                label = { Text(stringResource(R.string.loc_seconds_1_60)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Rest Timer Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.loc_auto_rest_timer),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.loc_auto_rest_timer_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = restEnabled,
                            onCheckedChange = { restEnabled = it }
                        )
                    }

                    if (restEnabled) {
                        Text(stringResource(R.string.loc_rest_duration), style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            restPresets.forEach { preset ->
                                FilterChip(
                                    selected = restDuration == preset && customRestText.isBlank(),
                                    onClick = {
                                        restDuration = preset
                                        customRestText = ""
                                    },
                                    label = { Text(stringResource(R.string.loc_seconds_short, preset)) }
                                )
                            }
                            FilterChip(
                                selected = customRestText.isNotBlank() || restDuration !in restPresets,
                                onClick = {
                                    if (customRestText.isBlank()) {
                                        customRestText = restDuration.toString()
                                    }
                                },
                                label = { Text(stringResource(R.string.loc_custom)) }
                            )
                        }

                        if (customRestText.isNotBlank() || restDuration !in restPresets) {
                            OutlinedTextField(
                                value = customRestText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(4)
                                    customRestText = filtered
                                    filtered.toIntOrNull()?.let {
                                        if (it in 5..3600) restDuration = it
                                    }
                                },
                                label = { Text(stringResource(R.string.loc_seconds_5_3600)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Signals & Feedback
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.loc_feedback),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.loc_sound_feedback))
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.loc_vibration_feedback))
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { vibrationEnabled = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalCountdown = if (countdownEnabled) {
                        customCountdownText.toIntOrNull() ?: countdownDuration
                    } else countdownDuration
                    val finalRest = if (restEnabled) {
                        customRestText.toIntOrNull() ?: restDuration
                    } else restDuration

                    onSave(
                        ExerciseTimerConfig(
                            countdownEnabled = countdownEnabled,
                            countdownDurationSeconds = finalCountdown.coerceIn(1, 60),
                            restTimerEnabled = restEnabled,
                            restDurationSeconds = finalRest.coerceIn(5, 3600),
                            soundEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

