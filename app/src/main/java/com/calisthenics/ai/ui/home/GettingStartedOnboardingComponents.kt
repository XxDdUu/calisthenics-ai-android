package com.calisthenics.ai.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.GettingStartedDay
import com.calisthenics.ai.domain.model.GettingStartedDayProgress
import com.calisthenics.ai.domain.model.GettingStartedDayStatus
import com.calisthenics.ai.domain.model.GettingStartedEnrollment
import com.calisthenics.ai.domain.model.GettingStartedExercise
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.GettingStartedScheduleConflict
import com.calisthenics.ai.domain.model.OnboardingStatus
import com.calisthenics.ai.ui.theme.LocalAppColors

/**
 * Onboarding Modal Dialog displayed above the Home screen for eligible athletes.
 * Shows program title, short introduction, and a CTA to view the program.
 * Does not expose detailed workout schedules.
 */
@Composable
fun GettingStartedOnboardingModal(
    program: GettingStartedProgram? = null,
    onViewProgram: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row: Badge & Dismiss (X)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.getting_started_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Dedicated close (X) button with distinct touch area
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.getting_started_dismiss_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Title and Short Introduction
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = program?.title ?: stringResource(R.string.getting_started_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = program?.description ?: stringResource(R.string.getting_started_card_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action CTA Button
                Button(
                    onClick = onViewProgram,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.getting_started_explore_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Animated onboarding container that seamlessly handles transitions between
 * the expanded onboarding card, compact persistent banner, and dismissed state.
 */
@Composable
fun GettingStartedOnboardingHost(
    onboardingStatus: OnboardingStatus,
    shouldShowCard: Boolean,
    shouldShowBanner: Boolean,
    onViewProgram: () -> Unit,
    onDismissCard: () -> Unit,
    onDismissBanner: () -> Unit,
    program: GettingStartedProgram? = null,
    modifier: Modifier = Modifier
) {
    if (shouldShowCard) {
        GettingStartedOnboardingModal(
            program = program,
            onViewProgram = onViewProgram,
            onDismiss = onDismissCard
        )
    }

    AnimatedVisibility(
        visible = shouldShowBanner,
        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250)),
        modifier = modifier
    ) {
        GettingStartedCompactBanner(
            program = program,
            onViewProgram = onViewProgram,
            onDismiss = onDismissBanner
        )
    }
}

/**
 * Initial expanded onboarding card shown only once for newly registered athletes.
 */
@Composable
fun GettingStartedExpandedCard(
    program: GettingStartedProgram? = null,
    onViewProgram: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Badge & Dismiss (X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.getting_started_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Dedicated close (X) button with distinct touch area
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.getting_started_dismiss_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title and Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = program?.title ?: stringResource(R.string.getting_started_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = program?.description ?: stringResource(R.string.getting_started_card_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Row
            Button(
                onClick = onViewProgram,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.getting_started_explore_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DayPill(
    dayNum: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Day $dayNum",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact, persistent banner placed at the absolute top of Home screen.
 */
@Composable
fun GettingStartedCompactBanner(
    program: GettingStartedProgram? = null,
    onViewProgram: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProgram),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "View 3-Day Program",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Close button (X) for permanent dismissal
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.getting_started_dismiss_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Detailed 3-Day Program Modal Bottom Sheet.
 * Displays dynamically created Admin program, and allows athletes to voluntarily enroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GettingStartedProgramBottomSheet(
    program: GettingStartedProgram?,
    onDismiss: () -> Unit,
    onEnroll: () -> Unit = {},
    onCompleteDay: (Int) -> Unit = {},
    isEnrolling: Boolean = false,
    onStartDay: (Int) -> Unit = {},
    isStartingDay: Boolean = false,
    enrollmentError: String? = null,
    onDismissError: () -> Unit = {},
    onExploreAiPlanner: () -> Unit = {},
    onCreateWorkoutPlan: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appColors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = program?.title ?: stringResource(R.string.getting_started_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = program?.description ?: stringResource(R.string.getting_started_sheet_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.getting_started_close_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!enrollmentError.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = enrollmentError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissError) {
                                Text(stringResource(R.string.loc_dismiss))
                            }
                        }
                    }
                }
            }

            // Enrollment / Status Card
            item {
                val enrollment = program?.enrollment
                val isEnrolled = enrollment != null

                if (enrollment?.isCompleted == true) {
                    GettingStartedCompletedProgramCard(
                        enrollment = enrollment,
                        onExploreAiPlanner = {
                            onDismiss()
                            onExploreAiPlanner()
                        },
                        onCreateWorkoutPlan = {
                            onDismiss()
                            onCreateWorkoutPlan()
                        }
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnrolled) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isEnrolled) Icons.Default.FitnessCenter else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val statusTitle = when {
                                        isEnrolled -> stringResource(
                                            R.string.getting_started_progress_summary,
                                            enrollment?.completedDaysCount ?: 0,
                                            enrollment?.totalDays ?: 3
                                        )
                                        else -> "Ready to Get Started?"
                                    }
                                    Text(
                                        text = statusTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val statusSubtitle = when {
                                        enrollment?.addedToWorkoutPlan == true ->
                                            "Scheduled in your Workout Plan alongside your existing routine."
                                        isEnrolled ->
                                            "Independent 3-day track • Existing Workout Plan unchanged."
                                        else ->
                                            "Enroll to track your 3-day progress independently or add it to your Workout Plan."
                                    }
                                    Text(
                                        text = statusSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!isEnrolled) {
                                    Button(
                                        onClick = onEnroll,
                                        enabled = !isEnrolling,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        if (isEnrolling) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text(
                                                stringResource(R.string.getting_started_enroll_cta),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (enrollment != null && !enrollment.addedToWorkoutPlan) {
                                OutlinedButton(
                                    onClick = onEnroll,
                                    enabled = !isEnrolling,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.getting_started_enroll_yes),
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (appColors.isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dynamic 3 Days
            if (program != null && program.days.isNotEmpty()) {
                items(program.days) { day ->
                    val enrollmentCompleted = program.enrollment?.isCompleted == true
                    DynamicWorkoutDayCard(
                        day = day,
                        isEnrolled = program.enrollment != null,
                        isProgramCompleted = enrollmentCompleted,
                        isStartingDay = isStartingDay,
                        onStartDay = { onStartDay(day.dayNumber) }
                    )
                }
            }

            // Close CTA
            item {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.getting_started_got_it),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun GettingStartedEnrollmentDialogs(
    showEnrollChoiceDialog: Boolean,
    showConflictDialog: Boolean,
    conflicts: List<GettingStartedScheduleConflict>,
    isBusy: Boolean,
    onDismissChoice: () -> Unit,
    onSelectAddToWorkoutPlan: () -> Unit,
    onSelectJustStart: () -> Unit,
    onDismissConflict: () -> Unit,
    onConfirmConflictAdd: () -> Unit
) {
    if (showEnrollChoiceDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) onDismissChoice() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.getting_started_enroll_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.getting_started_enroll_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSelectAddToWorkoutPlan,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.getting_started_enroll_yes),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onSelectJustStart,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.getting_started_enroll_no),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissChoice, enabled = !isBusy) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) onDismissConflict() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.getting_started_conflict_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.getting_started_conflict_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    conflicts.take(4).forEach { conflict ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${conflict.date} • Day ${conflict.gettingStartedDayNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Overlaps with: ${conflict.existingWorkoutTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirmConflictAdd, enabled = !isBusy) {
                    Text(stringResource(R.string.getting_started_conflict_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConflict, enabled = !isBusy) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun GettingStartedDayStatusPill(
    status: GettingStartedDayStatus,
    modifier: Modifier = Modifier
) {
    val completedGreen = Color(0xFF16A34A)
    val inProgressAmber = Color(0xFFD97706)
    val (bg, fg, labelRes) = when (status) {
        GettingStartedDayStatus.COMPLETED -> Triple(
            completedGreen.copy(alpha = 0.16f),
            completedGreen,
            R.string.getting_started_completed_badge
        )
        GettingStartedDayStatus.IN_PROGRESS -> Triple(
            inProgressAmber.copy(alpha = 0.16f),
            inProgressAmber,
            R.string.getting_started_in_progress_badge
        )
        GettingStartedDayStatus.NOT_STARTED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.string.getting_started_not_started_badge
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun GettingStartedActivePlanCard(
    program: GettingStartedProgram,
    startingDayNumber: Int?,
    onStartDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val enrollment = program.enrollment ?: return
    if (!enrollment.addedToWorkoutPlan || enrollment.isCompleted) return
    val dayProgressMap = enrollment.dayProgress.associateBy { it.dayNumber }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "TEMPORARY 3-DAY PROGRAM",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(
                            R.string.getting_started_progress_summary,
                            enrollment.completedDaysCount,
                            enrollment.totalDays
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            program.days.forEach { day ->
                val p = dayProgressMap[day.dayNumber]
                val effectiveStatus = p?.status ?: day.status
                val scheduledDate = p?.scheduledDate ?: day.scheduledDate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Day ${day.dayNumber} • ${day.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GettingStartedDayStatusPill(status = effectiveStatus)
                            if (!scheduledDate.isNullOrBlank()) {
                                Text(
                                    text = scheduledDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (effectiveStatus != GettingStartedDayStatus.COMPLETED) {
                        val isBusyThisDay = startingDayNumber == day.dayNumber
                        Button(
                            onClick = { onStartDay(day.dayNumber) },
                            enabled = startingDayNumber == null,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isBusyThisDay) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (effectiveStatus == GettingStartedDayStatus.IN_PROGRESS) {
                                        stringResource(R.string.resume_workout)
                                    } else {
                                        stringResource(R.string.common_start)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GettingStartedCompletedProgramCard(
    enrollment: GettingStartedEnrollment,
    onExploreAiPlanner: () -> Unit,
    onCreateWorkoutPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedGreen = Color(0xFF16A34A)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, completedGreen.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = completedGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = enrollment.programTitle ?: stringResource(R.string.getting_started_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                GettingStartedDayStatusPill(status = GettingStartedDayStatus.COMPLETED)
            }

            Text(
                text = stringResource(R.string.getting_started_completed_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.getting_started_completed_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.getting_started_progress_summary,
                        enrollment.completedDaysCount.coerceAtLeast(enrollment.totalDays),
                        enrollment.totalDays
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = completedGreen
                )
                enrollment.completedAt?.take(10)?.let { date ->
                    Text(
                        text = "Completed $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExploreAiPlanner,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.getting_started_cta_ai_planner),
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onCreateWorkoutPlan,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.getting_started_cta_create_plan),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicWorkoutDayCard(
    day: GettingStartedDay,
    isEnrolled: Boolean,
    isProgramCompleted: Boolean,
    isStartingDay: Boolean,
    onStartDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedGreen = Color(0xFF16A34A)
    val isDayCompleted = day.status == GettingStartedDayStatus.COMPLETED || day.completed
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDayCompleted) completedGreen.copy(alpha = 0.09f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            1.dp,
            if (isDayCompleted) completedGreen.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Day ${day.dayNumber}: ${day.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!day.description.isNullOrBlank()) {
                        Text(
                            text = day.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!day.scheduledDate.isNullOrBlank()) {
                        Text(
                            text = "Scheduled: ${day.scheduledDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                GettingStartedDayStatusPill(
                    status = if (isDayCompleted) GettingStartedDayStatus.COMPLETED else day.status
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.exercises.forEach { ex ->
                    DynamicExerciseRow(ex)
                }
            }

            if (isEnrolled && !isDayCompleted && !isProgramCompleted) {
                Button(
                    onClick = onStartDay,
                    enabled = !isStartingDay,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (day.status == GettingStartedDayStatus.IN_PROGRESS) {
                            stringResource(R.string.getting_started_resume_day, day.dayNumber)
                        } else {
                            stringResource(R.string.getting_started_start_day, day.dayNumber)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicExerciseRow(exercise: GettingStartedExercise) {
    val targetText = if (exercise.durationSeconds != null && exercise.durationSeconds > 0) {
        "${exercise.sets} sets × ${exercise.durationSeconds}s"
    } else {
        "${exercise.sets} sets × ${exercise.reps ?: 10} reps"
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$targetText • ${exercise.restSeconds}s rest",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (!exercise.instructions.isNullOrBlank()) {
            Text(
                text = exercise.instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
