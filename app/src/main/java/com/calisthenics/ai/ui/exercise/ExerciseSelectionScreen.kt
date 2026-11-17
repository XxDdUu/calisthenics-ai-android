package com.calisthenics.ai.ui.exercise

import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.QuotaEntitlement
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.navigation.FloatingBottomBarDefaults
import com.calisthenics.ai.ui.theme.LocalAppColors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionScreen(
    onSelectExercise: (String) -> Unit,
    onOpenLiveAnalysis: (String) -> Unit,
    onBackClick: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onSelectVideoExercise: (Long, String, String) -> Unit = { _, _, _ -> },
    onOpenAnalysisResult: (Long) -> Unit = {},
    viewModel: ExerciseSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasResumedOnce by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasResumedOnce) {
                    viewModel.refreshQuotaAndHistory()
                } else {
                    hasResumedOnce = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.choose_exercise)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.live_analysis_history),
                            tint = MaterialTheme.colorScheme.onBackground
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
            uiState.isLoading -> {
                LoadingView(
                    message = stringResource(R.string.loc_loading_exercises),
                    modifier = Modifier.padding(padding)
                )
            }

            uiState.error != null -> {
                ErrorView(
                    message = uiState.error ?: "Failed to load exercises",
                    onRetry = { viewModel.loadExercises() },
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                val showExercises = uiState.selectedTab == AnalysisSelectionTab.ALL ||
                    uiState.selectedTab == AnalysisSelectionTab.EXERCISES
                val showHistory = uiState.selectedTab == AnalysisSelectionTab.ALL ||
                    uiState.selectedTab == AnalysisSelectionTab.HISTORY

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentPadding = PaddingValues(bottom = FloatingBottomBarDefaults.ContentBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Membership & Daily Quota Banner
                    item {
                        MembershipQuotaBanner(
                            quota = uiState.quota,
                            isLoading = uiState.isLoadingQuota,
                            onUpgradeOrSwitchClick = {
                                viewModel.openUpgradeDialog(UpgradeDialogReason.QUOTA_BANNER)
                            }
                        )
                    }

                    // 2. Section / Tab Filter
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnalysisTabChip(
                                label = "All",
                                selected = uiState.selectedTab == AnalysisSelectionTab.ALL,
                                onClick = { viewModel.selectTab(AnalysisSelectionTab.ALL) }
                            )
                            AnalysisTabChip(
                                label = "Exercises",
                                selected = uiState.selectedTab == AnalysisSelectionTab.EXERCISES,
                                onClick = { viewModel.selectTab(AnalysisSelectionTab.EXERCISES) }
                            )
                            AnalysisTabChip(
                                label = "History (${uiState.videoHistory.size})",
                                selected = uiState.selectedTab == AnalysisSelectionTab.HISTORY,
                                onClick = { viewModel.selectTab(AnalysisSelectionTab.HISTORY) }
                            )
                        }
                    }

                    if (showExercises) {
                        // 3. Live Analysis Card (with PRO lock when FREE)
                        item {
                            LiveAnalysisEntryCard(
                                liveAnalysisUnlocked = uiState.quota?.liveAnalysisUnlocked == true,
                                onClick = {
                                    if (uiState.quota?.liveAnalysisUnlocked == true) {
                                        onOpenLiveAnalysis("SQUAT")
                                    } else {
                                        viewModel.openUpgradeDialog(UpgradeDialogReason.LIVE_ANALYSIS_LOCKED)
                                    }
                                }
                            )
                        }

                        // 4. AI Video Analysis Core Exercises (SQUAT, PUSH_UP, PULL_UP)
                        item {
                            SectionTitleBlock(
                                title = "AI Video Analysis Exercises",
                                subtitle = "Upload a video for automated rep detection, biomechanical metrics & skeleton replay"
                            )
                        }

                        items(uiState.coreVideoExercises, key = { it.code }) { coreExercise ->
                            CoreVideoExerciseCard(
                                exercise = coreExercise,
                                onClick = {
                                    onSelectVideoExercise(
                                        coreExercise.exerciseId,
                                        coreExercise.displayName,
                                        coreExercise.code
                                    )
                                }
                            )
                        }

                        // 5. Other Catalog Skill Progressions
                        if (uiState.parentExercises.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                SectionTitleBlock(
                                    title = "Skill Progressions & Catalog",
                                    subtitle = "Select a skill family to view progressions"
                                )
                            }
                            items(uiState.parentExercises, key = { it.code }) { exercise ->
                                ExerciseCard(
                                    group = exercise,
                                    onClick = { onSelectExercise(exercise.code) }
                                )
                            }
                        }
                    }

                    if (showHistory) {
                        // 6. Video Analysis History Section
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitleBlock(
                                    title = "Video Analysis History",
                                    subtitle = "Recent uploaded videos & AI form reports",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.refreshQuotaAndHistory() }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh history",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (uiState.videoHistory.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (uiState.historyError != null) {
                                                "Could not load video history (${uiState.historyError}). Tap refresh to retry."
                                            } else {
                                                "No uploaded video analyses yet. Choose Squat, Push-up, or Pull-up above to start."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(uiState.videoHistory, key = { "history_${it.id}" }) { item ->
                                VideoHistoryItemCard(
                                    item = item,
                                    onClick = { onOpenAnalysisResult(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showUpgradeDialog) {
        MembershipUpgradeDialog(
            quota = uiState.quota,
            reason = uiState.upgradeDialogReason,
            isUpdating = uiState.isUpdatingMembership,
            errorMessage = uiState.membershipError,
            onSwitchTier = { targetTier -> viewModel.updateMembershipTier(targetTier) },
            onDismiss = { viewModel.dismissUpgradeDialog() }
        )
    }
}

@Composable
private fun AnalysisTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun MembershipQuotaBanner(
    quota: QuotaEntitlement?,
    isLoading: Boolean,
    onUpgradeOrSwitchClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    val isAdmin = quota?.isAdmin == true
    val isPro = quota?.isPro == true
    val tierLabel = quota?.membershipTier ?: "FREE"
    val remaining = quota?.remainingToday ?: 1
    val dailyLimit = quota?.dailyLimit ?: 1
    val accentColor = when {
        isAdmin -> appColors.accent
        isPro -> appColors.accent
        else -> appColors.warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (isAdmin) "ADMIN ACCESS" else "$tierLabel TIER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (isAdmin || quota?.isUnlimited == true) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = appColors.accent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Unlimited Access",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = appColors.accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else if (isPro) {
                    OutlinedButton(
                        onClick = onUpgradeOrSwitchClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Switch Tier",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = onUpgradeOrSwitchClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Upgrade to PRO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isAdmin || quota?.isUnlimited == true) {
                Text(
                    text = "Unlimited uploads today (${quota?.usedToday ?: 0} used)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.accent
                )
                Text(
                    text = "No daily quota limit • Resets 00:00 ICT (${quota?.timezone ?: "Asia/Ho_Chi_Minh"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (quota != null) {
                Text(
                    text = "$remaining/$dailyLimit uploads left today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (quota.isExhausted) appColors.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Resets 00:00 ICT (${quota.timezone})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = if (isLoading) "Checking daily upload quota..." else "Daily quota: 1 upload/day on FREE tier",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Resets 00:00 ICT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiveAnalysisEntryCard(
    liveAnalysisUnlocked: Boolean,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    val containerColor = if (liveAnalysisUnlocked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (!liveAnalysisUnlocked) {
            BorderStroke(1.dp, appColors.warning.copy(alpha = 0.45f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (liveAnalysisUnlocked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                appColors.warning.copy(alpha = 0.16f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (liveAnalysisUnlocked) Icons.Default.Videocam else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (liveAnalysisUnlocked) MaterialTheme.colorScheme.primary else appColors.warning
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.loc_live_analysis_upper),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!liveAnalysisUnlocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = appColors.warning.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "PRO Feature",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.warning,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (liveAnalysisUnlocked) {
                            stringResource(R.string.loc_open_camera_hint)
                        } else {
                            "Upgrade to PRO to unlock real-time camera form coaching"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (liveAnalysisUnlocked) {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                } else {
                    Icons.Default.Lock
                },
                contentDescription = null,
                tint = if (liveAnalysisUnlocked) MaterialTheme.colorScheme.primary else appColors.warning
            )
        }
    }
}

@Composable
private fun SectionTitleBlock(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoreVideoExerciseCard(
    exercise: CoreVideoExercise,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(appColors.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = appColors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = exercise.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = exercise.code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = exercise.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = exercise.cameraHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.accent
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.common_select),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VideoHistoryItemCard(
    item: AnalysisResult,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    val statusUpper = item.status.uppercase()
    val statusColor = when (statusUpper) {
        "COMPLETED" -> appColors.success
        "PROCESSING", "PENDING" -> appColors.warning
        else -> appColors.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = statusUpper,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val dateText = item.createdAt.substringBefore("T").ifBlank { "Recent upload" }
                val repsText = if (item.repCount > 0) {
                    " • ${item.validReps}/${item.repCount} valid reps"
                } else {
                    ""
                }
                Text(
                    text = "$dateText$repsText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.overallScore?.let { score ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${score.roundToInt()}/100",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open report",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MembershipUpgradeDialog(
    quota: QuotaEntitlement?,
    reason: UpgradeDialogReason,
    isUpdating: Boolean,
    errorMessage: String?,
    onSwitchTier: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isCurrentlyPro = quota?.isPro == true
    val targetTier = if (isCurrentlyPro) "FREE" else "PRO"
    val title = when {
        reason == UpgradeDialogReason.LIVE_ANALYSIS_LOCKED && !isCurrentlyPro -> "PRO Membership Required"
        isCurrentlyPro -> "Manage Membership Tier"
        else -> "Upgrade to PRO"
    }
    val body = when {
        reason == UpgradeDialogReason.LIVE_ANALYSIS_LOCKED && !isCurrentlyPro ->
            "Live Camera Analysis is a PRO feature. Upgrade to PRO to unlock real-time form coaching and 10 daily video analyses."
        isCurrentlyPro ->
            "You currently have PRO active (${quota?.remainingToday ?: 10}/${quota?.dailyLimit ?: 10} daily uploads, Live Analysis unlocked). You can switch back to FREE tier at any time."
        else ->
            "PRO unlocks 10 video analyses per day (vs 1/day on FREE) plus real-time Live Camera Analysis."
    }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSwitchTier(targetTier) },
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isCurrentlyPro) "Switch to FREE" else "Upgrade to PRO")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text(if (isCurrentlyPro) "Keep PRO" else "Not Now")
            }
        }
    )
}

@Composable
fun ExerciseCard(
    group: ParentExerciseGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.common_select),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
