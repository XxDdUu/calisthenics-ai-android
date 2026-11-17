package com.calisthenics.ai.ui.liveanalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.LiveRepRecord
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.theme.LocalAppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class LiveAnalysisHistoryDetailUiState(
    val isLoading: Boolean = true,
    val session: LiveSessionRecord? = null,
    val error: String? = null
)

@HiltViewModel
class LiveAnalysisHistoryDetailViewModel @Inject constructor(
    private val repository: LiveAnalysisRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _uiState = MutableStateFlow(LiveAnalysisHistoryDetailUiState())
    val uiState: StateFlow<LiveAnalysisHistoryDetailUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun loadSession() {
        if (sessionId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Session ID is missing.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getSessionDetails(sessionId)
                .onSuccess { sessionRecord ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            session = sessionRecord,
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Could not load session details."
                        )
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAnalysisHistoryDetailScreen(
    onBackClick: () -> Unit,
    viewModel: LiveAnalysisHistoryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val appColors = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.workout_history_detail),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = appColors.accent)
                    }
                }

                uiState.error != null -> {
                    EmptyState(
                        title = stringResource(R.string.history_unavailable),
                        body = uiState.error ?: stringResource(R.string.could_not_load_history),
                        action = stringResource(R.string.common_retry),
                        onAction = viewModel::loadSession
                    )
                }

                uiState.session != null -> {
                    val session = uiState.session!!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Section A: Workout Overview
                        item {
                            OverviewSection(session = session)
                        }

                        // Section B: Performance Metrics
                        item {
                            PerformanceMetricsSection(session = session)
                        }

                        // Section C: AI Biomechanical Analysis
                        item {
                            AiAnalysisSection(session = session)
                        }

                        // Section D: Rep-by-Rep Breakdown Header
                        item {
                            Text(
                                text = stringResource(R.string.rep_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Rep Cards
                        if (session.reps.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "No individual rep records were captured for this session.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(session.reps, key = { it.repNumber }) { rep ->
                                RepDetailCard(rep = rep)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewSection(session: LiveSessionRecord) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = appColors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = session.exerciseDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (session.status == "COMPLETED") Color(0xFF2E7D32) else appColors.accent
                ) {
                    Text(
                        text = session.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = stringResource(R.string.loc_total_reps),
                    value = "${session.totalReps}",
                    highlight = true
                )
                MetricColumn(
                    label = stringResource(R.string.loc_total_duration),
                    value = formatDuration(session.durationSeconds)
                )
                MetricColumn(
                    label = "Date",
                    value = formatDateTime(session.startedAt ?: session.completedAt)
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricsSection(session: LiveSessionRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.performance_metrics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = stringResource(R.string.loc_average_score),
                    value = session.averageScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
                )
                MetricColumn(
                    label = stringResource(R.string.loc_peak_score),
                    value = session.peakScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
                )
                MetricColumn(
                    label = stringResource(R.string.loc_lowest_score),
                    value = session.lowestScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = stringResource(R.string.correct_reps),
                    value = "${session.correctReps}",
                    color = Color(0xFF4CAF50)
                )
                MetricColumn(
                    label = stringResource(R.string.incorrect_reps),
                    value = "${session.incorrectReps}",
                    color = if (session.incorrectReps > 0) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface
                )
                MetricColumn(
                    label = stringResource(R.string.form_consistency),
                    value = session.formConsistencyPercent?.let { "$it%" } ?: "—"
                )
            }

            val avgAngle = session.averageJointAngle ?: session.averageKneeAngle
            val deepestAngle = session.deepestJointAngle ?: session.deepestKneeAngle
            if (avgAngle != null || deepestAngle != null) {
                val isPushUp = session.exerciseType.equals("PUSH_UP", ignoreCase = true)
                val avgLabel = if (isPushUp) stringResource(R.string.average_elbow_angle) else stringResource(R.string.average_knee_angle)
                val deepestLabel = if (isPushUp) stringResource(R.string.deepest_elbow_angle) else stringResource(R.string.deepest_knee_angle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    avgAngle?.let {
                        MetricColumn(
                            label = avgLabel,
                            value = String.format(Locale.US, "%.1f°", it)
                        )
                    }
                    deepestAngle?.let {
                        MetricColumn(
                            label = deepestLabel,
                            value = String.format(Locale.US, "%.1f°", it)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAnalysisSection(session: LiveSessionRecord) {
    val appColors = LocalAppColors.current

    val uniqueMistakes = session.reps
        .mapNotNull { it.feedback }
        .filter { it.isNotBlank() && !it.contains("good", ignoreCase = true) && !it.contains("full depth", ignoreCase = true) }
        .distinct()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_analysis),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.movement_quality),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (session.movementQuality) {
                        "EXCELLENT" -> Color(0xFF2E7D32)
                        "GOOD" -> Color(0xFF388E3C)
                        "MODERATE" -> Color(0xFFF57C00)
                        null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        else -> Color(0xFFD32F2F)
                    }
                ) {
                    Text(
                        text = session.movementQuality ?: "UNAVAILABLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Mistakes Section
            Text(
                text = stringResource(R.string.mistakes_detected),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uniqueMistakes.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_mistakes_detected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (mistake in uniqueMistakes) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = mistake,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Recommendations for next workout
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = appColors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.next_workout_recommendations),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = generateRecommendations(session),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RepDetailCard(rep: LiveRepRecord) {
    var isExpanded by remember { mutableStateOf(false) }

    val scoreColor = when {
        (rep.score ?: 0.0) >= 80.0 -> Color(0xFF4CAF50)
        (rep.score ?: 0.0) >= 60.0 -> Color(0xFFFFA000)
        else -> Color(0xFFE57373)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "#${rep.repNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column {
                        rep.depth?.let { depth ->
                            Text(
                                text = "Depth: $depth",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        val repAngle = rep.minJointAngle ?: rep.minElbowAngle ?: rep.minKneeAngle
                        repAngle?.let { angle ->
                            Text(
                                text = String.format(Locale.US, "Min Angle: %.1f°", angle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rep.score?.let { score ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = scoreColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "${score.toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (!rep.feedback.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = rep.feedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                rep.durationMs?.let { dur ->
                    Text(
                        text = "Duration: ${String.format(Locale.US, "%.2fs", dur / 1000.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    highlight: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatDuration(seconds: Double): String {
    val totalSec = seconds.toLong()
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min > 0) "${min}m ${sec}s" else "${sec}s"
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

private fun generateRecommendations(session: LiveSessionRecord): String {
    val lowest = session.lowestScore ?: 100.0
    val shallowCount = session.reps.count { it.depth.equals("SHALLOW", ignoreCase = true) }
    val avgAngle = session.averageJointAngle ?: session.averageKneeAngle ?: 90.0
    val isPushUp = session.exerciseType.equals("PUSH_UP", ignoreCase = true)

    return buildString {
        if (shallowCount > 0) {
            if (isPushUp) {
                append("• Focus on full range of motion: lower your chest until elbows bend to 90 degrees or touch the floor.\n")
            } else {
                append("• Focus on full range of motion: aim for hips to drop below parallel with knee joints at the bottom position.\n")
            }
        }
        if (isPushUp && avgAngle > 95.0) {
            append("• Work on shoulder stability and controlled descent to reach standard 90° elbow depth.\n")
        } else if (!isPushUp && avgAngle > 100.0) {
            append("• Work on hip and ankle mobility stretches before squatting to ease reaching deeper bottom positions.\n")
        }
        if (lowest < 70.0) {
            append("• Keep your cadence smooth and controlled; avoid rushing transitions between descending and ascending phases.\n")
        }
        if (isEmpty()) {
            append("• Excellent technique maintained across all repetitions! Continue with progressive overload or higher tempo.")
        }
    }.trimEnd()
}
