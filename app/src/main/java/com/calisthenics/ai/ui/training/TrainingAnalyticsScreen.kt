package com.calisthenics.ai.ui.training

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
import androidx.compose.material.icons.filled.*
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
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.theme.*

private val FireOrange = Color(0xFFFF6B4A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingAnalyticsScreen(
    onBack: () -> Unit,
    onExerciseClick: (Long) -> Unit,
    onManagePrs: (() -> Unit)? = null,
    viewModel: TrainingAnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val streakColor = if (LocalAppColors.current.isDark) Warning else FireOrange

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
                title = {
                    Text(
                        "Training Analytics",
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
                actions = {
                    IconButton(onClick = { viewModel.loadAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryText)
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
            state.overview == null && state.weekly == null -> {
                EmptyState(
                    title = "Analytics Unavailable",
                    body = state.error ?: "Complete workout sessions to unlock training analytics.",
                    action = "Retry",
                    onAction = { viewModel.loadAll() }
                )
            }
            else -> {
                val overview = state.overview
                val weekly = state.weekly
                val insights = state.insights

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Streak & Consistency Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Elevated)
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = streakColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "Consistency & Streaks",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText
                                        )
                                    }
                                    PillBadge(
                                        "${overview?.currentStreakDays ?: 0} Day Streak",
                                        isAccent = true
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        label = "Current",
                                        value = "${overview?.currentStreakDays ?: 0}d",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Longest",
                                        value = "${overview?.longestStreakDays ?: 0}d",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "This Month",
                                        value = "${overview?.activeDaysThisMonth ?: 0}d",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Freq",
                                        value = "%.1f/w".format(insights?.weeklyFrequency ?: 0.0),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Training Insights Card
                    if (insights != null && insights.insights.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Elevated)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Insights, contentDescription = "Insights", tint = Accent, modifier = Modifier.size(20.dp))
                                        Text(
                                            "Training Insights",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText
                                        )
                                    }

                                    insights.insights.forEach { insight ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 6.dp)
                                                    .size(6.dp)
                                                    .background(Accent, CircleShape)
                                            )
                                            Text(
                                                insight,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = PrimaryText,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Weekly Overview Card
                    if (weekly != null) {
                        item {
                            DarkCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Weekly Performance",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText
                                        )
                                        val dateRange = if (weekly.startDate.isNotBlank() && weekly.endDate.isNotBlank()) {
                                            "${weekly.startDate.takeLast(5)} - ${weekly.endDate.takeLast(5)}"
                                        } else {
                                            "This Week"
                                        }
                                        Text(
                                            dateRange,
                                            fontSize = 12.sp,
                                            color = SecondaryText
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatCard(
                                            label = "Workouts",
                                            value = weekly.totalWorkouts.toString(),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatCard(
                                            label = "Total Volume",
                                            value = "${weekly.totalVolumeKg.toInt()} kg",
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatCard(
                                            label = "Reps",
                                            value = weekly.totalReps.toString(),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Comparison vs previous week
                                    if (weekly.comparisonVsPreviousWeek.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            weekly.comparisonVsPreviousWeek["volumeImprovementPercent"]?.let { pct ->
                                                Text(
                                                    "Volume: ${if (pct >= 0) "+" else ""}${"%.1f".format(pct)}% vs last wk",
                                                    fontSize = 12.sp,
                                                    color = if (pct >= 0) Accent else Error
                                                )
                                            }
                                            weekly.comparisonVsPreviousWeek["workoutsDifference"]?.let { diff ->
                                                Text(
                                                    "Workouts: ${if (diff >= 0) "+" else ""}${diff.toInt()} vs last wk",
                                                    fontSize = 12.sp,
                                                    color = if (diff >= 0) Accent else SecondaryText
                                                )
                                            }
                                        }
                                    }

                                    // Weekday frequency visual
                                    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        daysOfWeek.forEach { dayName ->
                                            val count = weekly.workoutsPerDay[dayName] ?: 0L
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            color = if (count > 0) Accent else Elevated,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (count > 0) {
                                                        Text(
                                                            count.toString(),
                                                            color = Ink,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(dayName.take(1), fontSize = 10.sp, color = SecondaryText)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Personal Records Section
                    val recentPRs = overview?.recentPRs ?: emptyList()
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Warning, modifier = Modifier.size(20.dp))
                                Text(
                                    "Personal Records (${overview?.totalPersonalRecords ?: recentPRs.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )
                            }
                            if (onManagePrs != null) {
                                TextButton(onClick = onManagePrs) {
                                    Text("Manage All", color = Accent, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (recentPRs.isEmpty()) {
                        item {
                            DarkCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("No personal records set yet. Keep training!", color = SecondaryText)
                                    if (onManagePrs != null) {
                                        Button(
                                            onClick = onManagePrs,
                                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                        ) {
                                            Text("Track New PR")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(recentPRs, key = { it.id }) { pr ->
                            val isShared = state.sharedPrs.any { it.personalRecord.id == pr.id }
                            val sharedRecord = state.sharedPrs.find { it.personalRecord.id == pr.id }

                            DarkCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pr.exerciseId?.let { onExerciseClick(it) } }
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                pr.exerciseName.ifEmpty { "Exercise Record" },
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryText,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (pr.achievedAt.isNotBlank()) {
                                                Text(
                                                    pr.achievedAt,
                                                    fontSize = 11.sp,
                                                    color = SecondaryText
                                                )
                                            }
                                        }

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
                                            OutlinedButton(
                                                onClick = { viewModel.shareRecordWithCoach(pr.id) },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Accent)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Share with Coach", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    // Coach Feedback (if any)
                                    if (sharedRecord?.coachFeedback != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Ink, RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    "Coach Feedback (${sharedRecord.coachName}):",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Accent
                                                )
                                                Text(
                                                    sharedRecord.coachFeedback,
                                                    fontSize = 12.sp,
                                                    color = PrimaryText
                                                )
                                            }
                                        }
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
