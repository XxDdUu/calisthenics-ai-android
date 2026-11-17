package com.calisthenics.ai.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.components.EmptyState
import com.calisthenics.ai.ui.components.EndRelationshipDialog
import com.calisthenics.ai.ui.components.GenderProfileCard
import com.calisthenics.ai.ui.components.InfoRow
import com.calisthenics.ai.ui.components.PillBadge
import com.calisthenics.ai.ui.components.StatCard
import com.calisthenics.ai.ui.components.UserAvatar
import com.calisthenics.ai.ui.report.ReportUserSheet
import com.calisthenics.ai.ui.components.localizedLabel
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@Composable
fun ClientDetailScreen(
    onBack: () -> Unit,
    onChatClick: (Long, Long, String, String?) -> Unit,
    onViewPlan: (Long) -> Unit,
    onViewProgress: ((Long) -> Unit)? = null,
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showEndDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    state.clientProfile?.let { client ->
        if (showReportSheet) {
            ReportUserSheet(
                userId = client.id,
                userName = client.displayName.ifBlank { client.fullName },
                onDismiss = { showReportSheet = false }
            )
        }
    }

    EndRelationshipDialog(
        isOpen = showEndDialog,
        isLoading = state.isTerminating,
        onConfirm = {
            viewModel.terminateRelationship {
                showEndDialog = false
                onBack()
            }
        },
        onDismiss = { showEndDialog = false }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = PrimaryText)
            }
            Text(
                stringResource(R.string.loc_client_training_profile),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Spacer(Modifier.weight(1f))
            if (state.clientProfile != null) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options), tint = SecondaryText)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Elevated)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.report_user),
                                    color = PrimaryText
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showReportSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.end_relationship),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showEndDialog = true
                            }
                        )
                    }
                }
            }
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            state.error != null && state.clientProfile == null -> {
                EmptyState(
                    title = if (state.isForbidden) "Access Restricted" else "Profile Unavailable",
                    body = state.error ?: "Could not load client training profile.",
                    action = if (!state.isForbidden) "Retry" else null,
                    onAction = viewModel::load
                )
            }
            state.clientProfile != null -> {
                val client = state.clientProfile!!
                val overview = state.trainingOverview

                // Header Card
                DarkCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    UserAvatar(client.avatarUrl, modifier = Modifier.size(96.dp))
}

                        Text(
                            text = client.displayName.ifBlank { client.fullName },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillBadge("Active Client", isAccent = true)
                            client.experienceLevel?.takeIf(String::isNotBlank)?.let {
                                PillBadge(it.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase))
                            }
                        }

                        client.bio?.takeIf(String::isNotBlank)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Gender Row
                GenderProfileCard(gender = client.gender)

                // Athlete Body Metrics Section (Height & Weight)
                AthleteBodyMetricsSection(
                    heightCm = client.heightCm,
                    weightKg = client.weightKg
                )

                // Athlete Media Gallery (view-only for coach)
                ProfileMediaGallery(
                    mediaList = state.clientMedia,
                    isOwner = false
                )

                // Training Overview Metrics (Real Data)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.loc_training_progress),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = stringResource(R.string.loc_active_plans),
                            value = "${overview?.activePlansCount ?: client.activePlansCount}",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.loc_completed_sessions),
                            value = "${overview?.completedSessionsCount ?: client.completedSessionsCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Current Plan Card
                val planName = overview?.currentPlanName ?: client.currentPlanName
                val planId = overview?.currentPlanId ?: client.currentPlanId
                DarkCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Accent)
                            Text(
                                stringResource(R.string.loc_current_training_plan),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryText
                            )
                        }
                        if (!planName.isNullOrBlank()) {
                            Text(
                                text = planName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryText
                            )
                            if (planId != null) {
                                OutlinedButton(
                                    onClick = { onViewPlan(planId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.loc_view_assigned_plan_details))
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.loc_client_no_active_plan),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                    }
                }

                // Training & Goals Card
                DarkCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.loc_training_goals_preferences),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        InfoRow(stringResource(R.string.gender), client.gender.localizedLabel())
                        InfoRow("Primary Goals", client.primaryGoals)
                        InfoRow("Training Experience", client.trainingExperience)
                        InfoRow("Interests & Skills", client.trainingInterests)
                        InfoRow("Preferred Frequency", client.preferredFrequency)
                        InfoRow("Coaching Language", client.coachingLanguage)
                    }
                }

                // Recent Workout Activity Card
                DarkCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.loc_recent_workout_activity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )

                        val workouts = overview?.recentWorkouts.orEmpty()
                        if (workouts.isNotEmpty()) {
                            workouts.forEach { workout ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = workout.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryText
                                        )
                                        workout.completedAt?.let {
                                            Text(
                                                text = "Completed ${it.take(10)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SecondaryText
                                            )
                                        }
                                    }
                                    PillBadge("Completed", isAccent = true)
                                }
                            }
                        } else {
                            Text(
                                stringResource(R.string.loc_no_recent_workouts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                        }
                    }
                }

                // Actions Card
                onViewProgress?.let {
                    Button(
                        onClick = { it(client.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Elevated, contentColor = PrimaryText)
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.loc_view_training_progress_prs))
                    }
                }

                Button(
                    onClick = {
                        viewModel.startChat { conversationId ->
                            onChatClick(
                                conversationId,
                                client.id,
                                client.displayName.ifBlank { client.fullName },
                                client.avatarUrl
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isStartingChat,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                ) {
                    if (state.isStartingChat) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.loc_message_client))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
