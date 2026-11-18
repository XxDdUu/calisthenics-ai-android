package com.calisthenics.ai.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.domain.model.AdminPage
import com.calisthenics.ai.domain.model.BulkExerciseGenerationItem
import com.calisthenics.ai.domain.model.BulkExerciseGenerationJob
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.CardBackground
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Info
import com.calisthenics.ai.ui.theme.Warning

private val BULK_PRESET_QUANTITIES = listOf(50, 100, 150, 200)
private val BULK_TRAINING_TYPES = listOf("BOTH", "CALISTHENICS", "GYM")
private val BULK_CATEGORIES = listOf(
    "PUSH", "PULL", "LEGS", "CORE", "SKILL", "ISOMETRIC",
    "FLEXIBILITY", "MOBILITY", "CHEST", "BACK", "SHOULDERS",
    "BICEPS", "TRICEPS", "GLUTES", "FULL_BODY", "CARDIO"
)

@Composable
fun BulkExerciseGenerationStatusCard(
    job: BulkExerciseGenerationJob,
    onInspectItems: (String?) -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = when (job.status.uppercase()) {
        "COMPLETED" -> Accent
        "FAILED" -> Error
        "PARTIAL" -> Warning
        else -> Info
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Bulk Job ${job.jobUid} • ${job.status}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = buildString {
                    append(job.trainingTypeFilter ?: "BOTH")
                    if (!job.categoriesFilter.isNullOrBlank()) {
                        append(" • ${job.categoriesFilter}")
                    }
                    append(" • Target: ${job.requestedQuantity}")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { (job.progressPercent.coerceIn(0, 100) / 100f) },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )

            Text(
                text = "Saved: ${job.savedCount}/${job.requestedQuantity} • Duplicates skipped: ${job.duplicateCount} • Failed: ${job.failedCount} • Batches: ${job.processedBatches}/${job.totalBatchesEstimated}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            if (job.statusMessage.isNotBlank()) {
                Text(
                    text = job.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { onInspectItems("SAVED") }) {
                    Text("Generated (${job.savedCount})")
                }
                if (job.duplicateCount > 0) {
                    OutlinedButton(onClick = { onInspectItems("DUPLICATE_SKIPPED") }) {
                        Text("Duplicates (${job.duplicateCount})")
                    }
                }
                if (job.canResume) {
                    Button(onClick = onResume) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Resume")
                    }
                }
            }
        }
    }
}

@Composable
fun BulkGenerationConfigDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onStart: (quantity: Int, trainingType: String, categories: List<String>, focus: String?) -> Unit
) {
    var quantityText by remember { mutableStateOf("100") }
    var selectedTrainingType by remember { mutableStateOf("BOTH") }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var focusText by remember { mutableStateOf("") }

    val parsedQty = quantityText.toIntOrNull()?.coerceIn(1, 500) ?: 100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Bulk Exercise Generation") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "All generated non-duplicate exercises are created in DRAFT + AI_RECOMMENDED status (never auto-published).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Quantity (50–200+ supported)", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BULK_PRESET_QUANTITIES.forEach { preset ->
                        FilterChip(
                            selected = parsedQty == preset,
                            onClick = { quantityText = preset.toString() },
                            label = { Text(preset.toString()) }
                        )
                    }
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { raw -> quantityText = raw.filter(Char::isDigit).take(4) },
                    label = { Text("Custom quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Training Discipline", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BULK_TRAINING_TYPES.forEach { type ->
                        FilterChip(
                            selected = selectedTrainingType == type,
                            onClick = { selectedTrainingType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Text("Category Filter (empty = all)", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BULK_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = cat in selectedCategories,
                            onClick = {
                                selectedCategories = if (cat in selectedCategories) {
                                    selectedCategories - cat
                                } else {
                                    selectedCategories + cat
                                }
                            },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(
                    value = focusText,
                    onValueChange = { focusText = it },
                    label = { Text("Optional focus (e.g. quad, planche, hypertrophy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && (quantityText.toIntOrNull() ?: 0) > 0,
                onClick = {
                    onStart(
                        parsedQty,
                        selectedTrainingType,
                        selectedCategories.toList(),
                        focusText.takeIf { it.isNotBlank() }
                    )
                }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Generate $parsedQty")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BulkGenerationItemsDialog(
    job: BulkExerciseGenerationJob,
    itemsPage: AdminPage<BulkExerciseGenerationItem>,
    isLoading: Boolean,
    outcomeFilter: String?,
    onSelectOutcome: (String?) -> Unit,
    onPageChange: (Int) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val outcomes = listOf(
        "SAVED" to "Generated",
        "DUPLICATE_SKIPPED" to "Duplicates",
        null to "All"
    )
    val selectedTabIdx = outcomes.indexOfFirst { it.first == outcomeFilter }.coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Job ${job.jobUid} Inspector") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabRow(selectedTabIndex = selectedTabIdx) {
                    outcomes.forEachIndexed { idx, (value, label) ->
                        Tab(
                            selected = idx == selectedTabIdx,
                            onClick = { onSelectOutcome(value) },
                            text = { Text(label, maxLines = 1) }
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                } else if (itemsPage.content.isEmpty()) {
                    Text(
                        text = "No items recorded for this filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(itemsPage.content, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = item.exerciseId != null) {
                                        item.exerciseId?.let(onOpenExercise)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = buildString {
                                        item.sequenceNumber?.let { append("#$it ") }
                                        append(item.exerciseName)
                                        item.exerciseId?.let { append(" (ID #$it)") }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${item.trainingType ?: "-"} • ${item.category ?: "-"} • ${item.difficulty ?: "-"} • Batch ${item.batchNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                item.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (item.outcome == "DUPLICATE_SKIPPED") Warning else Accent
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        enabled = itemsPage.page > 0 && !isLoading,
                        onClick = { onPageChange(itemsPage.page - 1) }
                    ) {
                        Text("Prev")
                    }
                    Text(
                        text = "Page ${itemsPage.page + 1} / ${itemsPage.totalPages.coerceAtLeast(1)} (${itemsPage.totalElements})",
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextButton(
                        enabled = itemsPage.hasNext && !isLoading,
                        onClick = { onPageChange(itemsPage.page + 1) }
                    ) {
                        Text("Next")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
