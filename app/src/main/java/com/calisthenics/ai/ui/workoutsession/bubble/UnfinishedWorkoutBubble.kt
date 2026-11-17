package com.calisthenics.ai.ui.workoutsession.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText

@Composable
fun UnfinishedWorkoutBubbleHost(
    onResumeSession: (WorkoutSession) -> Unit,
    currentRoute: String?,
    modifier: Modifier = Modifier,
    viewModel: UnfinishedWorkoutBubbleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Do not show bubble on workout session itself or during auth/splash flows
    val shouldHideOnCurrentRoute = currentRoute == null ||
        currentRoute.startsWith("workout_session") ||
        currentRoute.startsWith("workout_summary") ||
        currentRoute == "splash" ||
        currentRoute.startsWith("auth") ||
        currentRoute.startsWith("password_reset") ||
        currentRoute == "authenticated_entry"

    if (state.isVisible && !shouldHideOnCurrentRoute) {
        val session = state.activeSession ?: return
        Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            UnfinishedWorkoutBubble(
                session = session,
                workoutTitle = state.workoutTitle,
                currentExerciseName = state.currentExerciseName,
                completedSets = state.completedSets,
                totalSets = state.totalSets,
                progress = state.progress,
                isMinimized = state.isMinimized,
                onResume = { onResumeSession(session) },
                onToggleMinimize = viewModel::toggleMinimized,
                onDismiss = viewModel::dismiss
            )
        }
    }
}

@Composable
fun UnfinishedWorkoutBubble(
    session: WorkoutSession,
    workoutTitle: String,
    currentExerciseName: String,
    completedSets: Int,
    totalSets: Int,
    progress: Float,
    isMinimized: Boolean,
    onResume: () -> Unit,
    onToggleMinimize: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(spring(dampingRatio = 0.8f)),
        exit = fadeOut() + scaleOut()
    ) {
        if (isMinimized) {
            // Compact pill view
            Box(
                modifier = modifier
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Elevated)
                    .border(1.5.dp, Accent, CircleShape)
                    .clickable(onClick = onToggleMinimize)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(26.dp),
                            color = Accent,
                            strokeWidth = 2.5.dp,
                            trackColor = Border,
                            strokeCap = StrokeCap.Round
                        )
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = workoutTitle.takeIf { it.isNotBlank() } ?: stringResource(R.string.active_workout_pill),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = onToggleMinimize,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UnfoldMore,
                            contentDescription = "Expand",
                            tint = SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Expanded focused card view
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .clickable(onClick = onResume),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Accent.copy(alpha = 0.8f)),
                colors = CardDefaults.cardColors(containerColor = Elevated)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header row: Icon, Title, Minimize & Dismiss buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workoutTitle.takeIf { it.isNotBlank() } ?: stringResource(R.string.workout_session),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentExerciseName.isNotBlank()) {
                                Text(
                                    text = currentExerciseName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleMinimize,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UnfoldLess,
                                    contentDescription = "Minimize",
                                    tint = SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Progress bar & Set counts
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (totalSets > 0) "$completedSets / $totalSets sets" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Accent
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Accent,
                            trackColor = Border,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    // Resume Action Button
                    Button(
                        onClick = onResume,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Ink
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.resume_workout),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
