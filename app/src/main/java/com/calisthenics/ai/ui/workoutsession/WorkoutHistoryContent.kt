package com.calisthenics.ai.ui.workoutsession

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.theme.Accent
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    onSessionSelected: (Long) -> Unit,
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout history") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            state.error != null && state.sessions.isEmpty() -> EmptyState(
                title = "History unavailable",
                body = state.error ?: "Could not load workout history.",
                action = "Retry",
                onAction = viewModel::load
            )
            state.sessions.isEmpty() -> EmptyState(
                title = "No completed workouts yet",
                body = "Finished workouts will appear here."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.sessions, key = { it.id }) { session ->
                    WorkoutHistoryRow(session, onClick = { onSessionSelected(session.id) })
                }
                if (state.error != null) {
                    item { Text(state.error ?: "", color = MaterialTheme.colorScheme.error) }
                }
                if (state.page + 1 < state.totalPages) {
                    item {
                        OutlinedButton(
                            onClick = viewModel::loadMore,
                            enabled = !state.isLoadingMore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isLoadingMore) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                            else Text("Load more")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(session: WorkoutSession, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    session.title,
                    Modifier.padding(start = 9.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                formatWorkoutDate(session.workoutDate ?: session.completedAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${formatDuration(session.durationSeconds ?: 0)} · ${session.completedExercises} completed · " +
                    "${session.skippedExercises} skipped",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    onReturnToToday: () -> Unit,
    onViewHistory: () -> Unit,
    onResume: (WorkoutSession) -> Unit,
    viewModel: WorkoutSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.shareSuccessMessage, state.error) {
        state.shareSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout summary") },
                navigationIcon = {
                    IconButton(onClick = onReturnToToday) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to today") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            state.session == null -> EmptyState(
                title = "Summary unavailable",
                body = state.error ?: "Could not load this workout.",
                action = "Retry",
                onAction = viewModel::load
            )
            else -> {
                val session = requireNotNull(state.session)
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.prs.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF26210F)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.EmojiEvents,
                                                contentDescription = "PR",
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                "NEW PERSONAL RECORD!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD700)
                                            )
                                        }

                                        state.prs.forEach { pr ->
                                            val isShared = state.sharedPrs.any { it.personalRecord.id == pr.id }
                                            val sharedRecord = state.sharedPrs.find { it.personalRecord.id == pr.id }

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B170B))
                                            ) {
                                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            pr.exerciseName.ifEmpty { "Exercise" },
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                        PillBadge(
                                                            pr.recordType.replace("MAX_", "").replace('_', ' ').ifEmpty { "PR" },
                                                            isAccent = true
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            pr.displayFormattedValue,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Accent
                                                        )

                                                        if (isShared) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier
                                                                    .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Accent,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(
                                                                    "Shared with Coach",
                                                                    color = Accent,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = { viewModel.shareRecordWithCoach(pr.id) },
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                shape = RoundedCornerShape(8.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                                                                enabled = !state.isSharing
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text("Share with Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }

                                                    if (sharedRecord?.coachFeedback != null) {
                                                        Text(
                                                            "Coach: ${sharedRecord.coachFeedback}",
                                                            fontSize = 12.sp,
                                                            color = Color(0xFFFFD700),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(session.title, style = MaterialTheme.typography.headlineSmall)
                                    Text(formatWorkoutDate(session.workoutDate ?: session.completedAt))
                                    Text("Duration: ${formatDuration(session.durationSeconds ?: 0)}")
                                    Text("${session.completedExercises} completed · ${session.skippedExercises} skipped · ${session.unfinishedExercises} unfinished")
                                    Text("${session.completedSets} completed sets · ${session.totalReps} reps · ${session.totalHoldDurationSeconds}s holds")
                                }
                            }
                        }
                        items(session.exercises.sortedBy { it.orderIndex }, key = { it.id }) { exercise ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                                    Text(exercise.status.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() })
                                    exercise.sets.forEach { set ->
                                        Text(
                                            buildString {
                                                append("Set ${set.setNumber}: ")
                                                if (set.reps != null) append("${set.reps} reps")
                                                if (set.holdDurationSeconds != null) append("${if (set.reps != null) " · " else ""}${set.holdDurationSeconds}s hold")
                                                if (set.weightKg != null) append(" · +${set.weightKg} kg")
                                                if (set.assistanceWeightKg != null) append(" · ${set.assistanceWeightKg} kg assist")
                                                if (!set.completed) append(" · not completed")
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (session.status == "IN_PROGRESS") {
                        Button(onClick = { onResume(session) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Resume workout")
                        }
                    }
                    Button(onClick = onReturnToToday, modifier = Modifier.fillMaxWidth()) {
                        Text("Return to Today's Workout")
                    }
                    TextButton(onClick = onViewHistory, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Open workout history")
                    }
                }
            }
        }
    }
}

private fun formatWorkoutDate(value: String?): String {
    if (value.isNullOrBlank()) return "Workout date unavailable"
    return runCatching {
        LocalDate.parse(value.take(10))
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()))
    }.recoverCatching {
        ZonedDateTime.parse(value)
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()))
    }.getOrDefault(value)
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainder)
    else "%02d:%02d".format(minutes, remainder)
}
