package com.calisthenics.ai.ui.coach

import androidx.compose.foundation.background
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

private val GoldStar = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProgressScreen(
    onBack: () -> Unit,
    viewModel: ClientProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSectionTab by remember { mutableIntStateOf(0) } // 0: PRs & Progression, 1: Shared PRs, 2: Exercise History

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
                        "Client Analytics & PRs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
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
                    title = "Progress Unavailable",
                    body = state.error ?: "Client has not completed any recorded workouts yet.",
                    action = "Retry",
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
                                    "Training Consistency & Volume",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        label = "Workouts",
                                        value = (overview?.totalWorkoutsCompleted ?: 0).toString(),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Streak",
                                        value = "${overview?.currentStreakDays ?: 0}d",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Active Days",
                                        value = "${overview?.activeDaysThisMonth ?: 0}d",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Volume",
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
                            listOf("Client PRs & Charts", "Shared PRs & Feedback", "Exercise Sessions").forEachIndexed { index, title ->
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
                                        title = "Client PR Progression"
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
                                            Text("No personal records recorded by this client yet.", color = SecondaryText, fontSize = 13.sp)
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
                                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldStar, modifier = Modifier.size(18.dp))
                                                        Text(pr.displayFormattedValue, fontWeight = FontWeight.Bold, color = Accent, fontSize = 16.sp)
                                                    }
                                                    PillBadge(pr.recordType.replace("MAX_", "").replace('_', ' '), isAccent = false)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Achieved: ${pr.achievedAt.take(10)}", fontSize = 12.sp, color = SecondaryText)
                                                    pr.improvementPercentage?.let { pct ->
                                                        if (pct > 0) Text("+$pct% improvement", fontSize = 11.sp, color = Accent)
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
                                    "Shared PRs & Feedback (${state.sharedPrs.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            }

                            if (state.sharedPrs.isEmpty()) {
                                item {
                                    DarkCard(Modifier.fillMaxWidth()) {
                                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                            Text("No PRs shared by client yet.", color = SecondaryText, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                items(state.sharedPrs, key = { it.id }) { share ->
                                    var feedbackInput by remember(share.id) { mutableStateOf(share.coachFeedback ?: "") }

                                    DarkCard(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        share.personalRecord.exerciseName.ifEmpty { "Exercise Record" },
                                                        fontWeight = FontWeight.Bold,
                                                        color = PrimaryText,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    if (share.sharedAt.isNotBlank()) {
                                                        Text("Shared ${share.sharedAt}", fontSize = 11.sp, color = SecondaryText)
                                                    }
                                                }
                                                PillBadge(
                                                    share.personalRecord.recordType.replace("MAX_", "").replace('_', ' ').ifEmpty { "PR" },
                                                    isAccent = true
                                                )
                                            }

                                            Text(
                                                share.personalRecord.displayFormattedValue,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Accent
                                            )

                                            // Coach feedback input / display
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedTextField(
                                                    value = feedbackInput,
                                                    onValueChange = { feedbackInput = it },
                                                    placeholder = { Text("Leave coach feedback on this PR...", fontSize = 12.sp, color = SecondaryText) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    maxLines = 3
                                                )
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                    Button(
                                                        onClick = { viewModel.submitFeedback(share.id, feedbackInput) },
                                                        enabled = !state.feedbackSubmitting && feedbackInput.isNotBlank(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                                    ) {
                                                        Icon(Icons.Default.Send, contentDescription = "Submit", modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Submit Feedback", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Section 2: Exercise Sessions & Volume Charts
                            state.exerciseProgress?.let { progress ->
                                if (progress.chartData.isNotEmpty()) {
                                    item {
                                        InteractiveProgressChart(
                                            dataPoints = progress.chartData,
                                            selectedFilter = state.selectedFilter,
                                            onFilterSelected = { viewModel.onFilterSelected(it) },
                                            unit = "kg",
                                            title = "${progress.exerciseName} Training Volume"
                                        )
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
