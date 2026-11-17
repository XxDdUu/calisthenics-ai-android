package com.calisthenics.ai.ui.coach

import com.calisthenics.ai.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Send
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
import com.calisthenics.ai.ui.bodymetrics.BodyMetricType
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.InteractiveProgressChart
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProgressScreen(
    onBack: () -> Unit,
    initialTab: Int = 0,
    viewModel: ClientProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSectionTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) } // 0: PRs & Progression, 1: Shared PRs, 2: Exercise History

    LaunchedEffect(state.feedbackSuccessMessage, state.error) {
        state.feedbackSuccessMessage?.let {
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
                title = {
                    Text(
                        stringResource(R.string.loc_client_analytics_prs),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Ink
    ) { padding ->
        when {
            state.isLoading && state.overview == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            state.overview == null && state.exerciseProgress == null && state.clientPrs.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.loc_progress_unavailable),
                    body = state.error ?: stringResource(R.string.loc_client_no_workouts),
                    action = stringResource(R.string.common_retry),
                    onAction = { viewModel.loadData() }
                )
            }
            else -> {
                val overview = state.overview

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Overview stats
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Elevated)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    stringResource(R.string.loc_training_consistency_volume),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        label = stringResource(R.string.workouts),
                                        value = (overview?.totalWorkoutsCompleted ?: 0).toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = stringResource(R.string.streak),
                                        value = "${overview?.currentStreakDays ?: 0}d",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = stringResource(R.string.loc_active_days),
                                        value = "${overview?.activeDaysThisMonth ?: 0}d",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = stringResource(R.string.loc_volume),
                                        value = "${(overview?.totalVolumeKg ?: 0.0).toInt()}kg",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Section Navigation Tabs
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(stringResource(R.string.loc_client_prs_charts), stringResource(R.string.loc_shared_prs_feedback_tab), stringResource(R.string.loc_exercise_sessions_tab), stringResource(R.string.loc_body_metrics_tab)).forEachIndexed { index, title ->
                                FilterChip(
                                    selected = selectedSectionTab == index,
                                    onClick = { selectedSectionTab = index },
                                    label = { Text(title) }
                                )
                            }
                        }
                    }

                    when (selectedSectionTab) {
                        0 -> {
                            // Section 0: Client PRs & Progression Charts
                            if (state.clientPrChartPoints.isNotEmpty()) {
                                item {
                                    InteractiveProgressChart(
                                        dataPoints = state.clientPrChartPoints,
                                        selectedFilter = state.selectedFilter,
                                        onFilterSelected = { viewModel.onFilterSelected(it) },
                                        unit = state.clientPrHistory.firstOrNull()?.unit ?: "units",
                                        title = stringResource(R.string.loc_client_pr_progression)
                                    )
                                }
                            }

                            // Exercise selector pills
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.exercises.forEach { ex ->
                                        val isSelected = state.selectedExerciseId == ex.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.onExerciseSelected(ex.id) },
                                            label = { Text(ex.name, fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }

                            // Client PR records list
                            if (state.clientPrs.isEmpty()) {
                                item {
                                    DarkCard(Modifier.fillMaxWidth()) {
                                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                            Text(stringResource(R.string.loc_no_client_prs), color = SecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                state.clientPrsGrouped.forEach { (exerciseName, records) ->
                                    item(key = "coach_pr_header_$exerciseName") {
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
                                            PillBadge("${records.size} PRs", isAccent = true)
                                        }
                                    }

                                    items(records, key = { "coach_pr_${it.id}" }) { pr ->
                                        DarkCard(Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Warning, modifier = Modifier.size(18.dp))
                                                        Text(pr.displayFormattedValue, fontWeight = FontWeight.Bold, color = Accent, fontSize = 16.sp)
                                                    }
                                                    PillBadge(pr.recordType.replace("MAX_", "").replace('_', ' '), isAccent = false)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(stringResource(R.string.loc_achieved_date, pr.achievedAt.take(10)), fontSize = 12.sp, color = SecondaryText)
                                                    pr.improvementPercentage?.let { pct ->
                                                        if (pct > 0) Text(stringResource(R.string.loc_improvement_percent, pct.toString()), fontSize = 11.sp, color = Accent)
                                                    }
                                                }
                                                pr.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                                                    Text(notes, fontSize = 12.sp, color = SecondaryText)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Section 1: Shared PRs & Coach Feedback Section
                            item {
                                Text(
                                    stringResource(R.string.loc_shared_pr_feedback, state.sharedPrs.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            }

                            if (state.sharedPrs.isEmpty()) {
                                item {
                                    DarkCard(Modifier.fillMaxWidth()) {
                                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Text(stringResource(R.string.loc_no_shared_prs), color = SecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                items(state.sharedPrs, key = { it.id }) { share ->
                                    var feedbackInput by remember(share.id) { mutableStateOf(share.coachFeedback ?: "") }
                                    var selectedRating by remember(share.id) { mutableIntStateOf(share.rating ?: 5) }

                                    LaunchedEffect(share.id, share.isRead) {
                                        if (!share.isRead) {
                                            viewModel.markShareAsRead(share.id)
                                        }
                                    }

                                    DarkCard(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        share.personalRecord.exerciseName.ifEmpty { stringResource(R.string.loc_exercise_record_fallback) },
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryText,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    if (share.sharedAt.isNotBlank()) {
                                                        Text(stringResource(R.string.loc_shared_date, share.sharedAt.take(16)), fontSize = 11.sp, color = SecondaryText)
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    if (!share.isRead) {
                                                        PillBadge(stringResource(R.string.loc_new_badge), isAccent = true)
                                                    }
                                                    PillBadge(
                                                        share.personalRecord.recordType.replace("MAX_", "").replace('_', ' ').ifEmpty { "PR" },
                                                        isAccent = false
                                                    )
                                                }
                                            }

                                            Text(
                                                share.personalRecord.displayFormattedValue,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Accent
                                            )

                                            // Rating selection
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(stringResource(R.string.loc_performance_rating), fontSize = 12.sp, color = SecondaryText)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    (1..5).forEach { star ->
                                                        val isSelected = star <= selectedRating
                                                        Text(
                                                            text = if (isSelected) "★" else "☆",
                                                            color = if (isSelected) Warning else SecondaryText,
                                                            fontSize = 18.sp,
                                                            modifier = Modifier.clickable { selectedRating = star }
                                                        )
                                                    }
                                                }
                                            }

                                            // Coach feedback input / display
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedTextField(
                                                    value = feedbackInput,
                                                    onValueChange = { feedbackInput = it },
                                                    placeholder = { Text(stringResource(R.string.loc_leave_coach_feedback), fontSize = 12.sp, color = SecondaryText) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    maxLines = 3
                                                )
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                    Button(
                                                        onClick = { viewModel.submitFeedback(share.id, feedbackInput, selectedRating) },
                                                        enabled = !state.feedbackSubmitting && feedbackInput.isNotBlank(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                                    ) {
                                                        Icon(Icons.Default.Send, contentDescription = stringResource(R.string.loc_submit), modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(if (share.coachFeedback != null) stringResource(R.string.loc_submit_or_update_feedback) else stringResource(R.string.loc_submit_feedback_button), fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Section 2: Exercise Sessions & Volume Charts
                            state.exerciseProgress?.let { progress ->
                                if (progress.chartData.isNotEmpty()) {
                                    item {
                                        InteractiveProgressChart(
                                            dataPoints = progress.chartData,
                                            selectedFilter = state.selectedFilter,
                                            onFilterSelected = { viewModel.onFilterSelected(it) },
                                            unit = "kg",
                                            title = stringResource(R.string.loc_exercise_training_volume, progress.exerciseName)
                                        )
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Section 3: Client Body Metrics Tracking
                            val bodyMetrics = state.bodyMetrics
                            val currentMetrics = bodyMetrics?.currentMetrics
                            val measurements = bodyMetrics?.measurements.orEmpty()

                            // Current metrics summary card
                            item {
                                DarkCard(Modifier.fillMaxWidth()) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                stringResource(R.string.loc_current_body_status),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryText
                                            )
                                            currentMetrics?.measuredAt?.let { ts ->
                                                PillBadge(
                                                    text = stringResource(R.string.loc_updated_at, ts.take(10)),
                                                    isAccent = false
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            StatCard(
                                                label = stringResource(R.string.loc_height),
                                                value = bodyMetrics?.currentHeightCm?.let { "$it cm" } ?: stringResource(R.string.loc_not_set),
                                                modifier = Modifier.weight(1f)
                                            )
                                            StatCard(
                                                label = stringResource(R.string.loc_current_weight),
                                                value = currentMetrics?.weightKg?.let { "$it kg" } ?: stringResource(R.string.loc_not_recorded),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Interactive Chart section
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        stringResource(R.string.loc_body_metric_progression),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )

                                    // Metric switcher chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        BodyMetricType.entries.forEach { metric ->
                                            FilterChip(
                                                selected = state.selectedBodyMetric == metric,
                                                onClick = { viewModel.onBodyMetricSelected(metric) },
                                                label = { Text(stringResource(R.string.loc_metric_label, metric.displayName, metric.unit)) }
                                            )
                                        }
                                    }

                                    if (state.bodyMetricChartPoints.isNotEmpty()) {
                                        InteractiveProgressChart(
                                            dataPoints = state.bodyMetricChartPoints,
                                            selectedFilter = state.selectedBodyMetricFilter,
                                            onFilterSelected = { viewModel.onBodyMetricFilterSelected(it) },
                                            unit = state.selectedBodyMetric.unit,
                                            title = stringResource(R.string.loc_progress_named_title, state.selectedBodyMetric.displayName)
                                        )
                                    } else {
                                        DarkCard(Modifier.fillMaxWidth()) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    stringResource(R.string.loc_no_measurements_named, state.selectedBodyMetric.displayName.lowercase()),
                                                    color = SecondaryText,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Historical records list
                            item {
                                Text(
                                    stringResource(R.string.loc_recorded_measurements_count, measurements.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            }

                            if (measurements.isEmpty()) {
                                item {
                                    DarkCard(Modifier.fillMaxWidth()) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stringResource(R.string.loc_client_body_metrics_empty),
                                                color = SecondaryText,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Display most recent first in history list
                                items(measurements.reversed(), key = { it.id }) { measurement ->
                                    DarkCard(Modifier.fillMaxWidth()) {
                                        Column(
                                            Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = measurement.measuredAt.take(10),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryText
                                                )
                                                PillBadge(
                                                    text = "${measurement.weightKg} kg",
                                                    isAccent = true
                                                )
                                            }

                                            val details = listOfNotNull(
                                                measurement.bodyFatPercentage?.let { stringResource(R.string.loc_body_fat_label, it.toString()) },
                                                measurement.waistCm?.let { stringResource(R.string.loc_waist_label, it.toString()) },
                                                measurement.chestCm?.let { stringResource(R.string.loc_chest_label, it.toString()) },
                                                measurement.armCm?.let { stringResource(R.string.loc_arm_label, it.toString()) },
                                                measurement.thighCm?.let { stringResource(R.string.loc_thigh_label, it.toString()) }
                                            )

                                            if (details.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    details.forEach { text ->
                                                        Text(
                                                            text = text,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = SecondaryText,
                                                            modifier = Modifier
                                                                .background(Elevated, RoundedCornerShape(6.dp))
                                                                .border(1.dp, Border, RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
