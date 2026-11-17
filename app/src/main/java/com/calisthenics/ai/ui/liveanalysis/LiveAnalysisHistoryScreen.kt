package com.calisthenics.ai.ui.liveanalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.R
import com.calisthenics.ai.data.local.AccountSessionGuard
import com.calisthenics.ai.data.local.AccountSessionManager
import com.calisthenics.ai.domain.model.LiveAnalysisSummary
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
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
import kotlin.math.roundToInt

data class LiveAnalysisHistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<LiveSessionRecord> = emptyList(),
    val summary: LiveAnalysisSummary? = null,
    val selectedFilter: String = "ALL",
    val error: String? = null
) {
    val filteredSessions: List<LiveSessionRecord>
        get() = if (selectedFilter == "ALL") {
            sessions
        } else {
            sessions.filter { it.exerciseType.equals(selectedFilter, ignoreCase = true) }
        }

    val availableExerciseTypes: List<String>
        get() {
            val fromSessions = sessions.map { it.exerciseType.uppercase() }.distinct()
            val defaults = listOf("SQUAT", "PUSH_UP")
            return (defaults + fromSessions).distinct()
        }
}

@HiltViewModel
class LiveAnalysisHistoryViewModel @Inject constructor(
    private val repository: LiveAnalysisRepository,
    private val accountSessionManager: AccountSessionManager = AccountSessionManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveAnalysisHistoryUiState())
    val uiState: StateFlow<LiveAnalysisHistoryUiState> = _uiState.asStateFlow()

    private val sessionGuard = AccountSessionGuard(
        parentScope = viewModelScope,
        sessionManager = accountSessionManager,
        onSessionCleared = { _uiState.value = LiveAnalysisHistoryUiState() },
        onSessionActive = { loadHistory() }
    )

    init {
        sessionGuard.startObserving()
        loadHistory()
    }

    fun loadHistory() {
        sessionGuard.launchScoped { captured ->
            _uiState.update { it.copy(isLoading = true, error = null) }

            val sessionsResult = repository.getSessions()
            val summaryResult = repository.getHistorySummary()
            if (!sessionGuard.isCurrentSession(captured)) return@launchScoped

            if (sessionsResult.isSuccess) {
                val sessions = sessionsResult.getOrDefault(emptyList())
                val summary = summaryResult.getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        sessions = sessions,
                        summary = summary,
                        error = null
                    )
                }
            } else {
                val exception = sessionsResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception?.localizedMessage ?: "Could not load AI analysis history."
                    )
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAnalysisHistoryScreen(
    onBackClick: () -> Unit,
    onSessionClick: (String) -> Unit,
    onStartWorkout: () -> Unit = onBackClick,
    viewModel: LiveAnalysisHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val appColors = LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.live_analysis_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::loadHistory,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.common_retry)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading && uiState.sessions.isEmpty() -> {
                LoadingView(
                    message = stringResource(R.string.loc_loading_exercises),
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.error != null && uiState.sessions.isEmpty() -> {
                ErrorView(
                    message = uiState.error ?: stringResource(R.string.could_not_load_history),
                    onRetry = viewModel::loadHistory,
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.sessions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.no_live_analysis_sessions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.no_live_analysis_sessions_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onStartWorkout,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.start_workout))
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = FloatingBottomBarDefaults.ContentBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Overall Summary Metric Cards
                    item {
                        LiveHistorySummaryHeader(
                            summary = uiState.summary,
                            sessions = uiState.sessions
                        )
                    }

                    // 2. Exercise Filter Chips
                    item {
                        FilterChipsRow(
                            availableTypes = uiState.availableExerciseTypes,
                            selectedFilter = uiState.selectedFilter,
                            onSelectFilter = viewModel::setFilter
                        )
                    }

                    // 3. Filtered Session Cards List
                    val filtered = uiState.filteredSessions
                    if (filtered.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_sessions_recorded),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { session ->
                            LiveSessionHistoryCard(
                                session = session,
                                onClick = { onSessionClick(session.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveHistorySummaryHeader(
    summary: LiveAnalysisSummary?,
    sessions: List<LiveSessionRecord>
) {
    val totalSessions = summary?.totalSessions ?: sessions.size.toLong()
    val totalReps = summary?.totalReps ?: sessions.sumOf { it.totalReps }.toLong()
    val totalDurationSec = summary?.totalDurationSeconds ?: sessions.sumOf { it.durationSeconds }
    val avgScore = summary?.overallAverageScore
        ?: sessions.mapNotNull { it.averageScore }.takeIf { it.isNotEmpty() }?.average()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.workout_overview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(
                    label = stringResource(R.string.total_sessions),
                    value = totalSessions.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = stringResource(R.string.total_reps_label),
                    value = totalReps.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = stringResource(R.string.total_training_duration),
                    value = formatTotalDuration(totalDurationSec),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricItem(
                    label = stringResource(R.string.overall_avg_score),
                    value = avgScore?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) appColors.accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun FilterChipsRow(
    availableTypes: List<String>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        FilterChip(
            selected = selectedFilter == "ALL",
            onClick = { onSelectFilter("ALL") },
            label = { Text(text = stringResource(R.string.filter_all)) }
        )

        availableTypes.forEach { type ->
            val label = when (type.uppercase()) {
                "SQUAT" -> stringResource(R.string.filter_squat)
                "PUSH_UP" -> stringResource(R.string.filter_push_up)
                else -> type.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }
            }
            FilterChip(
                selected = selectedFilter.equals(type, ignoreCase = true),
                onClick = { onSelectFilter(type) },
                label = { Text(text = label) }
            )
        }
    }
}

@Composable
private fun LiveSessionHistoryCard(
    session: LiveSessionRecord,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    val score = session.averageScore ?: 0.0

    val scoreColor = when {
        score >= 80.0 -> appColors.success
        score >= 60.0 -> appColors.warning
        else -> appColors.error
    }

    val exerciseLabel = when (session.exerciseType.uppercase()) {
        "SQUAT" -> stringResource(R.string.filter_squat)
        "PUSH_UP" -> stringResource(R.string.filter_push_up)
        else -> session.exerciseDisplayName
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Exercise Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = exerciseLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formatDateTime(session.createdAt ?: session.startedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Reps badge
                        Text(
                            text = "${session.totalReps} reps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Duration badge
                        Text(
                            text = formatDuration(session.durationSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Joint angle preview if available
                        val angle = session.deepestJointAngle ?: session.deepestKneeAngle
                        if (angle != null) {
                            Text(
                                text = "Min: ${angle.roundToInt()}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Score Badge + Navigation Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (session.averageScore != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = scoreColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${session.averageScore.roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
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

private fun formatDuration(seconds: Double): String {
    val totalSec = seconds.roundToInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return if (mins > 0) {
        "${mins}m ${secs}s"
    } else {
        "${secs}s"
    }
}

private fun formatTotalDuration(seconds: Double): String {
    val totalSec = seconds.roundToInt()
    val hours = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return when {
        hours > 0 -> "${hours}h ${mins}m"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}
