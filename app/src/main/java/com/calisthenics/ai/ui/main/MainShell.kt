package com.calisthenics.ai.ui.main

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.home.HomeViewModel
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import com.calisthenics.ai.ui.today.TodayWorkoutSection

@Composable
fun HomeDashboard(
    onAnalysis: () -> Unit,
    onTraining: () -> Unit,
    onHistoryItem: (Long) -> Unit,
    onWorkoutDetails: (Long, WorkoutDayOfWeek) -> Unit,
    onStartWorkout: (Long, Long, String, Long?) -> Unit,
    onWorkoutHistory: () -> Unit,
    onResumeWorkout: (WorkoutSession) -> Unit,
    onAnalytics: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            TodayWorkoutSection(
                onViewDetails = onWorkoutDetails,
                onStartWorkout = onStartWorkout,
                onViewPlans = onTraining,
                onViewHistory = onWorkoutHistory,
                onResumeSession = onResumeWorkout
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DarkCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoGraph, contentDescription = null)
                        Text("AI Analysis", style = MaterialTheme.typography.titleMedium)
                        Text("Review your form", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onAnalysis) { Text("Start") }
                    }
                }
                DarkCard(
                    Modifier
                        .weight(1f)
                        .then(if (onAnalytics != null) Modifier.clickable { onAnalytics() } else Modifier)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null)
                        Text("Analytics", style = MaterialTheme.typography.titleMedium)
                        Text("Track Overload & PRs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { onAnalytics?.invoke() }) { Text("View") }
                    }
                }
            }
        }
        item { Text("Recent activity", style = MaterialTheme.typography.titleLarge) }
        when {
            state.isLoadingHistory -> item { CircularProgressIndicator(Modifier.size(28.dp)) }
            state.error != null -> item {
                EmptyState("History unavailable", state.error ?: "Could not load history", "Retry", viewModel::loadHistory)
            }
            state.history.isEmpty() -> item {
                EmptyState("Your history is clear", "Complete an analysis to see form results here.")
            }
            else -> items(state.history.take(5), key = { it.id }) { result ->
                RecentAnalysisRow(result, onClick = { onHistoryItem(result.id) })
            }
        }
        item { Spacer(Modifier.size(12.dp)) }
    }
}

@Composable
private fun RecentAnalysisRow(result: AnalysisResult, onClick: () -> Unit) {
    DarkCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(result.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text(result.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            result.overallScore?.let { Text("${it.toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, body: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DarkCard(Modifier.fillMaxWidth()) {
            EmptyState("Not connected yet", "This feature is ready for integration. No local or fabricated data is shown.")
        }
    }
}
