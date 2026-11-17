package com.calisthenics.ai.ui.exercise

import com.calisthenics.ai.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.InteractiveProgressChart
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    viewModel: ExerciseProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.progress?.exerciseName ?: stringResource(R.string.loc_exercise_progress),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        },
        containerColor = Ink
    ) { padding ->
        when {
            state.isLoading && state.progress == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            state.error != null && state.progress == null -> {
                EmptyState(
                    title = stringResource(R.string.data_unavailable),
                    body = state.error ?: stringResource(R.string.loc_could_not_load_exercise_progress),
                    action = stringResource(R.string.common_retry),
                    onAction = { viewModel.load() }
                )
            }
            state.progress != null -> {
                val progress = state.progress!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category & Last Performed
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillBadge(progress.exerciseCategory.ifEmpty { stringResource(R.string.loc_general) }, isAccent = true)
                            progress.lastPerformedDate?.let {
                                Text(
                                    stringResource(R.string.loc_last_session_date, it),
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )
                            }
                        }
                    }

                    // Stats Grid
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                label = stringResource(R.string.workouts),
                                value = progress.totalSessionsCompleted.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = stringResource(R.string.loc_total_sets),
                                value = progress.totalSetsCompleted.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                label = stringResource(R.string.loc_total_reps),
                                value = progress.totalRepsCompleted.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Best Set & PR Summary Card
                    item {
                        DarkCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = stringResource(R.string.loc_pr_short), tint = Accent, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.personal_best), fontWeight = FontWeight.Bold, color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    progress.bestSetSummary ?: stringResource(R.string.loc_no_sets_recorded_yet),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Accent
                                )
                            }
                        }
                    }

                    // Progressive Overload Comparison Card
                    progress.overloadComparison?.let { comp ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Elevated)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.loc_progressive_overload),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PrimaryText
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(
                                                    if (comp.improved) Accent.copy(alpha = 0.2f) else Error.copy(alpha = 0.2f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (comp.improved) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = if (comp.improved) Accent else Error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "${if (comp.percentageChange >= 0) "+" else ""}${"%.1f".format(comp.percentageChange)}%",
                                                color = if (comp.improved) Accent else Error,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(stringResource(R.string.previous_session), fontSize = 11.sp, color = SecondaryText)
                                            Text(
                                                "${comp.previousValue} ${comp.metricName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = PrimaryText,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(stringResource(R.string.latest_session), fontSize = 11.sp, color = SecondaryText)
                                            Text(
                                                "${comp.currentValue} ${comp.metricName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Metric Selection Tabs
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                ProgressMetricType.WEIGHT,
                                ProgressMetricType.REPS,
                                ProgressMetricType.DURATION,
                                ProgressMetricType.VOLUME
                            ).forEach { metric ->
                                val isSelected = state.selectedMetric == metric
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) Accent else Elevated,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.onMetricSelected(metric) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = metric.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Ink else PrimaryText
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Chart
                    item {
                        InteractiveProgressChart(
                            dataPoints = viewModel.getMetricChartPoints(),
                            selectedFilter = state.selectedFilter,
                            onFilterSelected = viewModel::onFilterSelected,
                            unit = state.selectedMetric.unit,
                            title = stringResource(R.string.loc_progression_title, state.selectedMetric.label)
                        )
                    }

                    // Exercise History Section Header
                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.loc_session_history_count, progress.history.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }

                    // History items
                    if (progress.history.isEmpty()) {
                        item {
                            DarkCard(Modifier.fillMaxWidth()) {
                                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.no_sessions_recorded), color = SecondaryText)
                                }
                            }
                        }
                    } else {
                        items(progress.history, key = { it.sessionId }) { session ->
                            DarkCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSessionClick(session.sessionId) }
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            session.sessionTitle,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            session.workoutDate,
                                            fontSize = 12.sp,
                                            color = SecondaryText
                                        )
                                    }

                                    // Sets breakdown
                                    session.sets.forEach { s ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                buildString {
                                                    append("Set ${s.setNumber}: ")
                                                    if (s.reps != null) append("${s.reps} reps")
                                                    if (s.durationSeconds != null) append(" · ${s.durationSeconds}s")
                                                    if (s.weightKg != null && s.weightKg > 0) append(" @ +${s.weightKg} kg")
                                                },
                                                fontSize = 13.sp,
                                                color = if (s.completed) PrimaryText else SecondaryText
                                            )
                                            if (s.isPr) {
                                                PillBadge(
                                                    text = "PR",
                                                    isAccent = true
                                                )
                                            }
                                        }
                                    }

                                    if (session.totalVolume > 0) {
                                        Text(
                                            stringResource(R.string.loc_volume_kg, session.totalVolume.toInt()),
                                            fontSize = 11.sp,
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
