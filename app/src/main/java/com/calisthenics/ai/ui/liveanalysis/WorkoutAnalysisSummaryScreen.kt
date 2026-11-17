package com.calisthenics.ai.ui.liveanalysis

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calisthenics.ai.ui.components.markdown.AiMarkdownText
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AiPerformanceSummary
import com.calisthenics.ai.domain.model.DetectedFormError
import com.calisthenics.ai.domain.model.ErrorSeverity
import com.calisthenics.ai.domain.model.ExerciseFormMetric
import com.calisthenics.ai.domain.model.LiveRepRecord
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.theme.LocalAppColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutAnalysisSummaryScreen(
    onBackClick: () -> Unit,
    onViewHistoryClick: () -> Unit = {},
    onDoneClick: () -> Unit = onBackClick,
    viewModel: WorkoutAnalysisSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val appColors = LocalAppColors.current
    val context = LocalContext.current

    fun shareResults(session: LiveSessionRecord) {
        val shareText = viewModel.buildShareSummaryText(session)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${session.exerciseDisplayName} - Workout Summary")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Workout Results"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.workout_analysis_summary_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    uiState.session?.let { session ->
                        IconButton(onClick = { shareResults(session) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.summary_action_share),
                                tint = appColors.accent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading && uiState.session == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = appColors.accent)
                    }
                }

                uiState.error != null && uiState.session == null -> {
                    EmptyState(
                        title = stringResource(R.string.history_unavailable),
                        body = uiState.error ?: stringResource(R.string.could_not_load_history),
                        action = stringResource(R.string.common_retry),
                        onAction = viewModel::loadSummary
                    )
                }

                uiState.session != null -> {
                    val session = uiState.session!!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
                    ) {
                        // SECTION A: Performance Overview
                        item {
                            PerformanceOverviewSection(session = session)
                        }

                        // SECTION B: Exercise-Specific Form Analysis
                        item {
                            FormAnalysisSection(session = session)
                        }

                        // SECTION C: Error Detection Summary
                        item {
                            ErrorDetectionSummarySection(errors = session.detectedErrors)
                        }

                        // SECTION D: Rep-by-Rep Breakdown Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.summary_section_rep_breakdown),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (session.reps.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.summary_rep_inspect_hint),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = appColors.secondaryText
                                    )
                                }
                            }
                        }

                        if (session.reps.isEmpty()) {
                            item {
                                val isHoldExercise = session.exerciseType.contains("PLANCHE", ignoreCase = true) ||
                                    session.exerciseType.contains("FRONT_LEVER", ignoreCase = true) ||
                                    session.exerciseType.contains("HANDSTAND", ignoreCase = true) ||
                                    session.exerciseType.contains("L_SIT", ignoreCase = true)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = appColors.accent
                                        )
                                        Text(
                                            text = if (isHoldExercise) {
                                                stringResource(R.string.summary_isometric_hold_note)
                                            } else {
                                                stringResource(R.string.summary_no_reps_recorded)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(session.reps, key = { it.repNumber }) { rep ->
                                RepSummaryRowCard(
                                    rep = rep,
                                    onClick = { viewModel.selectRep(rep) }
                                )
                            }
                        }

                        // SECTION E: AI Performance Recommendations (Async & Non-Blocking)
                        item {
                            AiRecommendationsSection(
                                isLoading = uiState.isAiLoading,
                                status = uiState.aiStatus,
                                summary = uiState.aiSummary,
                                onRetry = { viewModel.fetchAiSummaryAsync(forceRetry = true) }
                            )
                        }

                        // SECTION F: Actions
                        item {
                            SummaryActionsSection(
                                onViewHistory = onViewHistoryClick,
                                onShare = { shareResults(session) },
                                onDone = onDoneClick
                            )
                        }
                    }
                }
            }
        }
    }

    // Detailed Rep Inspection Modal Bottom Sheet
    val selectedRep = uiState.selectedRep
    val activeSession = uiState.session
    if (selectedRep != null && activeSession != null) {
        RepInspectionBottomSheet(
            rep = selectedRep,
            exerciseDisplayName = activeSession.exerciseDisplayName,
            originalVideoUrl = activeSession.originalVideoUrl,
            annotatedVideoUrl = activeSession.annotatedVideoUrl,
            onDismiss = { viewModel.selectRep(null) }
        )
    }
}

@Composable
private fun PerformanceOverviewSection(session: LiveSessionRecord) {
    val appColors = LocalAppColors.current
    val overall = session.resolvedOverallScore
    val scoreColor = scoreColorFor(overall)
    val isHold = session.exerciseType.contains("PLANCHE", ignoreCase = true) ||
        session.exerciseType.contains("FRONT_LEVER", ignoreCase = true) ||
        session.exerciseType.contains("HANDSTAND", ignoreCase = true) ||
        session.exerciseType.contains("L_SIT", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(appColors.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = appColors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = session.exerciseDisplayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatDateTime(session.startedAt ?: session.completedAt ?: session.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = appColors.secondaryText
                        )
                    }
                }

                // Overall Score Pill / Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.14f))
                        .border(2.5.dp, scoreColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = overall?.let { String.format(Locale.US, "%.0f", it) } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = scoreColor
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Primary stats grid (Reps / Hold, Duration, Confidence)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isHold && session.totalReps == 0) {
                    OverviewStatItem(
                        label = stringResource(R.string.summary_peak_hold),
                        value = session.peakHoldDurationSeconds?.let { String.format(Locale.US, "%.1fs", it) } ?: "—",
                        highlight = true
                    )
                } else {
                    OverviewStatItem(
                        label = stringResource(R.string.loc_total_reps),
                        value = "${session.totalReps}",
                        highlight = true
                    )
                }

                OverviewStatItem(
                    label = stringResource(R.string.loc_total_duration),
                    value = formatDuration(session.durationSeconds)
                )

                OverviewStatItem(
                    label = stringResource(R.string.summary_analysis_confidence),
                    value = session.analysisConfidence?.let { "${(it * 100).toInt()}%" }
                        ?: stringResource(R.string.summary_metric_unavailable)
                )
            }

            // Secondary score & success/fail row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallStatColumn(
                    label = stringResource(R.string.loc_average_score),
                    value = session.averageScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
                )
                SmallStatColumn(
                    label = stringResource(R.string.correct_reps),
                    value = "${session.correctReps}",
                    color = Color(0xFF4CAF50)
                )
                SmallStatColumn(
                    label = stringResource(R.string.incorrect_reps),
                    value = "${session.incorrectReps}",
                    color = if (session.incorrectReps > 0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface
                )
                SmallStatColumn(
                    label = stringResource(R.string.loc_peak_score),
                    value = session.peakScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
                )
            }
        }
    }
}

@Composable
private fun FormAnalysisSection(session: LiveSessionRecord) {
    val appColors = LocalAppColors.current
    val metrics = session.exerciseMetrics

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = appColors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.summary_section_form_analysis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            if (metrics.isEmpty()) {
                Text(
                    text = "Detailed biomechanical component breakdown is unavailable for this session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.secondaryText
                )
            } else {
                metrics.forEach { metric ->
                    ExerciseMetricCard(metric = metric)
                }
            }
        }
    }
}

@Composable
private fun ExerciseMetricCard(metric: ExerciseFormMetric) {
    val isUnavailable = metric.score == null ||
        metric.status.equals("UNAVAILABLE", ignoreCase = true) ||
        metric.status.equals("INSUFFICIENT_DATA", ignoreCase = true)

    val scoreColor = scoreColorFor(metric.score)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isUnavailable) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = stringResource(R.string.summary_metric_unavailable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (metric.value != null) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", metric.value)}${metric.unit}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = scoreColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${metric.score?.toInt() ?: 0}/100",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (isUnavailable) {
            if (!metric.unavailableReason.isNullOrBlank()) {
                Text(
                    text = metric.unavailableReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val fraction = ((metric.score ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun ErrorDetectionSummarySection(errors: List<DetectedFormError>) {
    val appColors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = if (errors.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (errors.isEmpty()) Color(0xFF4CAF50) else Color(0xFFFFA000),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.summary_section_form_issues),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (errors.isNotEmpty()) {
                    Text(
                        text = "${errors.size} issue(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.secondaryText
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            if (errors.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.summary_no_form_issues),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                errors.forEach { issue ->
                    DetectedIssueRow(issue = issue)
                }
            }
        }
    }
}

@Composable
private fun DetectedIssueRow(issue: DetectedFormError) {
    val severityColor = when (issue.severity) {
        ErrorSeverity.HIGH -> Color(0xFFE53935)
        ErrorSeverity.MEDIUM -> Color(0xFFFFA000)
        ErrorSeverity.LOW -> Color(0xFF42A5F5)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .border(1.dp, severityColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = issue.errorName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = severityColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = issue.severity.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "${issue.occurrences}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        val contextInfo = buildString {
            if (issue.repNumbers.isNotEmpty()) {
                append("Reps: ${issue.repNumbers.joinToString(", ") { "#$it" }}")
            }
            if (issue.timestampsMs.isNotEmpty()) {
                if (isNotEmpty()) append("  •  ")
                append("At: ${issue.timestampsMs.take(4).joinToString(", ") { formatDurationMs(it) }}")
            }
        }
        if (contextInfo.isNotBlank()) {
            Text(
                text = contextInfo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!issue.suggestedCorrection.isNullOrBlank()) {
            Text(
                text = "Correction: ${issue.suggestedCorrection}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RepSummaryRowCard(
    rep: LiveRepRecord,
    onClick: () -> Unit
) {
    val scoreColor = scoreColorFor(rep.score)
    val passed = rep.isValid

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (passed) Color(0xFF4CAF50) else Color(0xFFFFA000)).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "#${rep.repNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (passed) Color(0xFF4CAF50) else Color(0xFFFFA000),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = rep.completionStatus ?: if (passed) "SUCCESS" else "NEEDS WORK",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        rep.timestampOffsetMs?.let { ts ->
                            Text(
                                text = "@ ${formatDurationMs(ts)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val subline = buildString {
                        rep.durationMs?.let { append("${String.format(Locale.US, "%.2fs", it / 1000.0)}") }
                        val angle = rep.minJointAngle ?: rep.minElbowAngle ?: rep.minKneeAngle
                        angle?.let {
                            if (isNotEmpty()) append(" • ")
                            append("Min ${String.format(Locale.US, "%.0f°", it)}")
                        }
                        if (rep.detectedErrors.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("${rep.detectedErrors.size} issue(s)")
                        }
                    }
                    if (subline.isNotBlank()) {
                        Text(
                            text = subline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scoreColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = rep.score?.let { "${it.toInt()}%" } ?: "—",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Inspect rep",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepInspectionBottomSheet(
    rep: LiveRepRecord,
    exerciseDisplayName: String,
    originalVideoUrl: String?,
    annotatedVideoUrl: String?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val videoUrl = annotatedVideoUrl ?: originalVideoUrl

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$exerciseDisplayName • Rep #${rep.repNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val scoreColor = scoreColorFor(rep.score)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scoreColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = rep.score?.let { "${it.toInt()}/100" } ?: "—",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallStatColumn(
                    label = "Status",
                    value = rep.completionStatus ?: if (rep.isValid) "SUCCESS" else "FAILED"
                )
                SmallStatColumn(
                    label = "Duration",
                    value = rep.durationMs?.let { String.format(Locale.US, "%.2fs", it / 1000.0) } ?: "—"
                )
                SmallStatColumn(
                    label = "Depth Rating",
                    value = rep.depth ?: "—"
                )
                val angle = rep.minJointAngle ?: rep.minElbowAngle ?: rep.minKneeAngle
                SmallStatColumn(
                    label = "Min Angle",
                    value = angle?.let { String.format(Locale.US, "%.1f°", it) } ?: "—"
                )
            }

            if (rep.timestampOffsetMs != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = appColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Session Timestamp: ${formatDurationMs(rep.timestampOffsetMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (rep.detectedErrors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Detected Issues in Rep #${rep.repNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    rep.detectedErrors.forEach { err ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = err, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (!rep.feedback.isNullOrBlank()) {
                Text(
                    text = "Feedback: ${rep.feedback}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Optional video playback jump-to-timestamp when video recording URL is available
            if (!videoUrl.isNullOrBlank()) {
                val targetMs = rep.timestampOffsetMs ?: 0L
                val exoPlayer = remember(videoUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(videoUrl))
                        prepare()
                        seekTo(targetMs)
                        playWhenReady = false
                    }
                }
                DisposableEffect(exoPlayer) {
                    onDispose { exoPlayer.release() }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                OutlinedButton(
                    onClick = {
                        exoPlayer.seekTo(targetMs)
                        exoPlayer.playWhenReady = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.summary_jump_to_video_timestamp, formatDurationMs(targetMs)))
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.common_close))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AiRecommendationsSection(
    isLoading: Boolean,
    status: String,
    summary: AiPerformanceSummary?,
    onRetry: () -> Unit
) {
    val appColors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = appColors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.summary_section_ai_recommendations),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            when {
                isLoading || status.equals("GENERATING", ignoreCase = true) -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = appColors.accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.summary_ai_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                status.equals("COMPLETED", ignoreCase = true) && summary != null -> {
                    if (!summary.overallAssessment.isNullOrBlank()) {
                        AiMarkdownText(
                            markdown = summary.overallAssessment,
                            color = MaterialTheme.colorScheme.onSurface,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                        )
                    }

                    if (summary.strengths.isNotEmpty()) {
                        AiBulletBlock(
                            title = stringResource(R.string.summary_ai_strengths),
                            items = summary.strengths,
                            accentColor = Color(0xFF4CAF50)
                        )
                    }

                    if (summary.weaknesses.isNotEmpty()) {
                        AiBulletBlock(
                            title = stringResource(R.string.summary_ai_weaknesses),
                            items = summary.weaknesses,
                            accentColor = Color(0xFFFFA000)
                        )
                    }

                    if (summary.recommendations.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.summary_ai_recommendations_list),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = appColors.accent
                            )
                            summary.recommendations.forEachIndexed { idx, rec ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.accent
                                    )
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (!summary.nextSessionFocus.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(appColors.accent.copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = appColors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.summary_ai_next_focus),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.accent
                                )
                                AiMarkdownText(
                                    markdown = summary.nextSessionFocus,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.summary_ai_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onRetry) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.common_retry),
                                tint = appColors.accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiBulletBlock(
    title: String,
    items: List<String>,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "•", color = accentColor, fontWeight = FontWeight.Bold)
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SummaryActionsSection(
    onViewHistory: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.summary_action_view_history), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.summary_action_share), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = appColors.accent,
                contentColor = appColors.onAccent
            )
        ) {
            Text(
                text = stringResource(R.string.summary_action_done),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun OverviewStatItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SmallStatColumn(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun scoreColorFor(score: Double?): Color {
    return when {
        score == null -> MaterialTheme.colorScheme.onSurfaceVariant
        score >= 80.0 -> Color(0xFF4CAF50)
        score >= 60.0 -> Color(0xFFFFA000)
        else -> Color(0xFFE57373)
    }
}

private fun formatDuration(seconds: Double): String {
    val totalSec = seconds.toLong().coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

private fun formatDateTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "—"
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy • HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        isoString.take(16).replace("T", " ")
    }
}
