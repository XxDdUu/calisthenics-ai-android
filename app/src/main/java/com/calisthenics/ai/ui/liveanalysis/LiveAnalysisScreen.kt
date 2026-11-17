package com.calisthenics.ai.ui.liveanalysis

import com.calisthenics.ai.R

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.util.Log
import android.util.Size
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeometrySize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.LiveFeedback
import com.calisthenics.ai.domain.model.LiveLandmark
import com.calisthenics.ai.domain.model.LiveScoringMetric
import com.calisthenics.ai.domain.model.LiveSessionSummary
import com.calisthenics.ai.ui.theme.LocalAppColors
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private data class JointConnection(val from: String, val to: String)

private val jointConnections = listOf(
    JointConnection("left_shoulder", "left_elbow"),
    JointConnection("left_elbow", "left_wrist"),
    JointConnection("left_shoulder", "left_hip"),
    JointConnection("left_hip", "left_knee"),
    JointConnection("left_knee", "left_ankle"),
    JointConnection("right_shoulder", "right_elbow"),
    JointConnection("right_elbow", "right_wrist"),
    JointConnection("right_shoulder", "right_hip"),
    JointConnection("right_hip", "right_knee"),
    JointConnection("right_knee", "right_ankle"),
    JointConnection("left_shoulder", "right_shoulder"),
    JointConnection("left_hip", "right_hip")
)

@Composable
fun LiveAnalysisScreen(
    exerciseCode: String,
    onBackClick: () -> Unit,
    onViewDetails: ((String) -> Unit)? = null,
    viewModel: LiveAnalysisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val appColors = LocalAppColors.current
    var isModalExpanded by remember { mutableStateOf(false) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    LaunchedEffect(exerciseCode) {
        if (exerciseCode.isNotBlank()) {
            viewModel.setExercise(exerciseCode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!cameraPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.loc_camera_required_live),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.allow_camera))
                }
            }
            return@Box
        }

        // Camera Preview Feed
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Pose Keypoint Skeleton Overlay
        if (uiState.showSkeleton) {
            PoseOverlay(
                landmarks = uiState.liveResult?.landmarks ?: emptyList(),
                modifier = Modifier.fillMaxSize()
            )
        }

        // Real-Time HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = MaterialTheme.colorScheme.onSurface)
                }

                // Phase Badge
                val currentPhase = uiState.liveResult?.currentState ?: uiState.liveResult?.phase ?: uiState.liveResult?.repPhase ?: "READY"
                PhaseBadge(phase = currentPhase)

                // Camera Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.toggleFlash() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.loc_toggle_flash),
                            tint = if (uiState.flashEnabled) appColors.accent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleCamera() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f), CircleShape)
                    ) {
                        Icon(Icons.Default.SwitchCamera, contentDescription = stringResource(R.string.loc_switch_camera), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Bottom Performance Analytics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isModalExpanded) {
                        CompactAnalysisControls(
                            score = uiState.liveResult?.currentScore ?: uiState.liveResult?.score,
                            repCount = uiState.liveResult?.repCount ?: 0,
                            isRunning = uiState.isRunning,
                            accentColor = appColors.accent,
                            stopColor = appColors.error,
                            onToggleRunning = { viewModel.toggleRunning() },
                            onExpand = { isModalExpanded = true }
                        )
                    } else {
                    // Header: Exercise Selector Combo Box & Loading Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExerciseSelectorComboBox(
                            selectedOption = uiState.currentExerciseOption,
                            availableExercises = uiState.availableExercises,
                            expanded = uiState.isExerciseDropdownExpanded,
                            onExpandChange = { viewModel.setDropdownExpanded(it) },
                            onSelectExercise = { viewModel.selectExercise(it) },
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.isAnalyzing && uiState.liveResult == null) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = appColors.accent)
                        }
                        Spacer(Modifier.width(4.dp))
                        Spacer(Modifier.weight(0.1f))
                        AnalysisExpandButton(
                            expanded = true,
                            onClick = { isModalExpanded = false }
                        )
                    }

                    // Camera View Recommended Angle Chip
                    CameraRecommendationChip(
                        recommendedView = uiState.currentExerciseOption.recommendedView
                    )

                    // Dedicated Squat Real-Time Telemetry Bar
                    if (uiState.exerciseCode.equals("SQUAT", ignoreCase = true)) {
                        SquatTelemetryRow(
                            currentState = uiState.liveResult?.currentState ?: uiState.liveResult?.repPhase ?: "STANDING",
                            kneeAngle = uiState.liveResult?.kneeAngle,
                            minKneeAngle = uiState.liveResult?.minKneeAngle,
                            depth = uiState.liveResult?.depth ?: "GOOD",
                            isTracking = uiState.liveResult?.isTracking ?: uiState.liveResult?.poseDetected ?: false
                        )
                    } else if (uiState.exerciseCode.equals("PUSH_UP", ignoreCase = true)) {
                        PushUpTelemetryRow(
                            currentState = uiState.liveResult?.currentState ?: uiState.liveResult?.repPhase ?: "TOP",
                            elbowAngle = uiState.liveResult?.elbowAngle ?: uiState.liveResult?.measurements?.get("elbow_angle"),
                            minElbowAngle = uiState.liveResult?.minElbowAngle ?: uiState.liveResult?.measurements?.get("min_elbow_angle"),
                            bodyAlignment = uiState.liveResult?.bodyAlignment ?: uiState.liveResult?.measurements?.get("body_alignment"),
                            depth = uiState.liveResult?.depth ?: "GOOD",
                            isTracking = uiState.liveResult?.isTracking ?: uiState.liveResult?.poseDetected ?: false
                        )
                    }

                    // Main Row: Circular Score Dial + Dynamic Reps/Duration + Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Circular Score Gauge with Confidence Ring
                        val scoreVal = uiState.liveResult?.currentScore ?: uiState.liveResult?.score
                        val scoreConf = uiState.liveResult?.scoreConfidence
                        CircularScoreGauge(
                            score = scoreVal,
                            confidence = scoreConf,
                            modifier = Modifier.size(80.dp)
                        )

                        // 2. Repetition / Hold Metrics Counter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            val isHoldExercise = uiState.currentExerciseOption.category.contains("HOLD", ignoreCase = true)
                            val repCount = uiState.liveResult?.repCount
                            val holdSec = uiState.liveResult?.holdDurationSec

                            if (!isHoldExercise || (repCount != null && repCount > 0)) {
                                Text(
                                    text = "${repCount ?: 0}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Black,
                                    color = appColors.primaryText
                                )
                                Text(
                                    text = stringResource(R.string.loc_reps_completed_upper),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.secondaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                val durationText = holdSec?.let { String.format("%.1fs", it) } ?: "0.0s"
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if ((holdSec ?: 0.0) > 0.0) appColors.success else appColors.primaryText
                                )
                                Text(
                                    text = stringResource(R.string.loc_hold_duration_upper),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.secondaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 3. Start / Stop Action Button
                        Button(
                            onClick = { viewModel.toggleRunning() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isRunning) appColors.error else appColors.accent
                            ),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (uiState.isRunning) R.string.loc_stop else R.string.common_start
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Detailed Technique Metrics Breakdown
                    val metrics = uiState.liveResult?.scoringMetrics.orEmpty()
                    if (metrics.isNotEmpty()) {
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(metrics) { metric ->
                                TechniqueMetricCard(metric = metric)
                            }
                        }
                    }

                    // Real-Time Coaching Feedback Banner
                    val feedbackItems = uiState.liveResult?.feedback.orEmpty()
                    val feedbackMessage = uiState.error
                        ?: feedbackItems.firstOrNull()?.message
                        ?: if (!uiState.isRunning) "Tap Start to begin live form analysis" else "Position body in camera view..."
                    val severity = if (uiState.error != null) "ERROR" else feedbackItems.firstOrNull()?.severity ?: "INFO"

                    FeedbackBanner(
                        message = feedbackMessage,
                        severity = severity
                    )
                    }
                }
            }
        }

        // Camera Processing Lifecycle Host
        if (previewView != null && cameraPermissionGranted) {
            CameraHost(
                previewView = previewView!!,
                exerciseCode = uiState.exerciseCode,
                sideView = uiState.currentExerciseOption.sideView,
                isRunning = uiState.isRunning,
                useFrontCamera = uiState.usingFrontCamera,
                flashEnabled = uiState.flashEnabled,
                viewModel = viewModel,
                lifecycleOwner = lifecycleOwner,
                context = context
            )
        }

        // Navigate straight to comprehensive WorkoutAnalysisSummaryScreen when workout stops
        val targetSummaryId = uiState.navigateToSummarySessionId
        LaunchedEffect(targetSummaryId) {
            if (targetSummaryId != null && onViewDetails != null) {
                viewModel.consumeSummaryNavigation()
                viewModel.resetSession()
                onViewDetails(targetSummaryId)
            }
        }

        // Fallback dialog only when no navigation callback is attached
        if (uiState.showCompletionDialog && uiState.completedSessionId != null && onViewDetails == null) {
            val completedSessionId = uiState.completedSessionId!!
            WorkoutCompletionDialog(
                reps = uiState.completedRepCount,
                exerciseName = uiState.completedExerciseName,
                isSaving = uiState.isSavingHistory,
                onViewDetails = {
                    viewModel.dismissCompletionDialog()
                    viewModel.resetSession()
                    onViewDetails?.invoke(completedSessionId)
                },
                onDone = {
                    viewModel.dismissCompletionDialog()
                    viewModel.resetSession()
                }
            )
        }

        if (uiState.isProRequiredError) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissProRequiredError() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = appColors.warning,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "PRO Membership Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = uiState.error ?: "Live Analysis requires a PRO membership. Upgrade to PRO to unlock real-time camera analysis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissProRequiredError()
                            onBackClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.accent)
                    ) {
                        Text(
                            text = "Go Back to Upgrade",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissProRequiredError() }) {
                        Text("Dismiss")
                    }
                }
            )
        }
    }
}

@Composable
private fun CompactAnalysisControls(
    score: Double?,
    repCount: Int,
    isRunning: Boolean,
    accentColor: Color,
    stopColor: Color,
    onToggleRunning: () -> Unit,
    onExpand: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score?.let { "%.0f".format(it) } ?: "--",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.loc_form_score),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = repCount.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.loc_rep_count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onToggleRunning,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) stopColor else accentColor
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 8.dp
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(
                    if (isRunning) R.string.loc_stop else R.string.common_start
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        AnalysisExpandButton(expanded = false, onClick = onExpand)
    }
}

@Composable
private fun AnalysisExpandButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                CircleShape
            )
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse analysis" else "Expand analysis",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExerciseSelectorComboBox(
    selectedOption: LiveExerciseOption,
    availableExercises: List<LiveExerciseOption>,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelectExercise: (LiveExerciseOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current

    Box(modifier = modifier) {
        Surface(
            onClick = { onExpandChange(!expanded) },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, appColors.accent.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(appColors.accent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = appColors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = selectedOption.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (selectedOption.category.uppercase()) {
                                "ISOMETRIC_HOLD" -> "Static Isometric Hold"
                                "HOLD_AND_REPETITION" -> "Hybrid Hold & Reps"
                                else -> "Repetition Counter & Form"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = appColors.accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.loc_select_exercise),
                    tint = appColors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier
                .width(300.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            availableExercises.forEach { exercise ->
                val isSelected = exercise.code.equals(selectedOption.code, ignoreCase = true)
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = exercise.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) appColors.accent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (exercise.category.uppercase()) {
                                        "ISOMETRIC_HOLD" -> "HOLD"
                                        "HOLD_AND_REPETITION" -> "HYBRID"
                                        else -> "REPS"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) appColors.accent else appColors.secondaryText
                                )
                            }
                            Text(
                                text = exercise.recommendedView,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = appColors.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (isSelected) appColors.accent else appColors.secondaryText.copy(alpha = 0.50f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        onSelectExercise(exercise)
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraRecommendationChip(
    recommendedView: String,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            tint = appColors.accent,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = recommendedView,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = appColors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PhaseBadge(phase: String) {
    val appColors = LocalAppColors.current
    val (bgColor, textColor) = when (phase.uppercase()) {
        "TOP", "LOCKOUT", "HOLDING", "COMPLETED_REP", "STANDING" -> appColors.success.copy(alpha = 0.20f) to appColors.success
        "ECCENTRIC", "DESCENDING", "LOWERING" -> appColors.warning.copy(alpha = 0.20f) to appColors.warning
        "BOTTOM", "INFLECTION" -> appColors.error.copy(alpha = 0.20f) to appColors.error
        "CONCENTRIC", "ASCENDING", "PULLING" -> appColors.info.copy(alpha = 0.20f) to appColors.info
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.70f) to MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, textColor.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = phase.uppercase().replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
private fun SquatTelemetryRow(
    currentState: String,
    kneeAngle: Double?,
    minKneeAngle: Double?,
    depth: String,
    isTracking: Boolean
) {
    val appColors = LocalAppColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, appColors.accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Current Squat State with indicator dot
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "STATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                val stateColor = when (currentState.uppercase()) {
                    "BOTTOM" -> appColors.error
                    "DESCENDING", "ASCENDING" -> appColors.warning
                    else -> appColors.success
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(stateColor, CircleShape)
                    )
                    Text(
                        text = currentState.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = stateColor
                    )
                }
            }

            // 2. Current Knee Angle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "KNEE ANGLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = kneeAngle?.let { "${it.roundToInt()}°" } ?: "--",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = appColors.primaryText
                )
            }

            // 3. Minimum Knee Angle this rep
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MIN ANGLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = minKneeAngle?.let { "${it.roundToInt()}°" } ?: "--",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if ((minKneeAngle ?: 180.0) <= 100.0) appColors.success else appColors.primaryText
                )
            }

            // 4. Squat Depth Status
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DEPTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                val (depthColor, depthText) = when (depth.uppercase()) {
                    "GOOD" -> appColors.success to "GOOD"
                    "MODERATE" -> appColors.warning to "MOD"
                    else -> appColors.error to "SHALLOW"
                }
                Text(
                    text = depthText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = depthColor
                )
            }
        }
    }
}

@Composable
private fun PushUpTelemetryRow(
    currentState: String,
    elbowAngle: Double?,
    minElbowAngle: Double?,
    bodyAlignment: Double?,
    depth: String,
    isTracking: Boolean
) {
    val appColors = LocalAppColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, appColors.accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Current Push-Up State
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "STATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                val stateColor = when (currentState.uppercase()) {
                    "BOTTOM" -> appColors.error
                    "DESCENDING", "ASCENDING" -> appColors.warning
                    else -> appColors.success
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(stateColor, CircleShape)
                    )
                    Text(
                        text = currentState.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = stateColor
                    )
                }
            }

            // 2. Current Elbow Angle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ELBOW",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = elbowAngle?.let { "${it.roundToInt()}°" } ?: "--",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = appColors.primaryText
                )
            }

            // 3. Minimum Elbow Angle this rep
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MIN ELBOW",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = minElbowAngle?.let { "${it.roundToInt()}°" } ?: "--",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if ((minElbowAngle ?: 180.0) <= 90.0) appColors.success else appColors.primaryText
                )
            }

            // 4. Body Alignment
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ALIGNMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                val alignmentDev = bodyAlignment ?: 0.0
                val (alignColor, alignText) = when {
                    bodyAlignment == null -> appColors.secondaryText to "--"
                    alignmentDev <= 15.0 -> appColors.success to "FLAT"
                    alignmentDev <= 25.0 -> appColors.warning to "SAG"
                    else -> appColors.error to "POOR"
                }
                Text(
                    text = alignText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = alignColor
                )
            }

            // 5. Depth Status
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DEPTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.secondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                val (depthColor, depthText) = when (depth.uppercase()) {
                    "FULL", "CHEST_TO_FLOOR", "GOOD" -> appColors.success to "FULL"
                    "MODERATE", "90_DEGREE", "PARALLEL" -> appColors.warning to "90°"
                    else -> appColors.error to "SHALLOW"
                }
                Text(
                    text = depthText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = depthColor
                )
            }
        }
    }
}

@Composable
private fun CircularScoreGauge(
    score: Double?,
    confidence: Double?,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = ((score ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350),
        label = "scoreGaugeProgress"
    )

    val scoreColor = when {
        score == null -> appColors.disabledText
        score >= 80.0 -> appColors.success
        score >= 60.0 -> appColors.warning
        else -> appColors.error
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 7.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = GeometrySize(diameter, diameter)

            // Background track
            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated progress arc
            drawArc(
                color = scoreColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score?.let { "${it.roundToInt()}" } ?: "--",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = scoreColor
            )
            if (confidence != null && confidence > 0) {
                Text(
                    text = "${(confidence * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = appColors.secondaryText
                )
            }
        }
    }
}

@Composable
private fun TechniqueMetricCard(metric: LiveScoringMetric) {
    val appColors = LocalAppColors.current
    val statusColor = when (metric.status.uppercase()) {
        "VALID" -> appColors.success
        "WARNING" -> appColors.warning
        else -> appColors.error
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)),
        modifier = Modifier.width(115.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metric.name.replace("_", " ").split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(statusColor, CircleShape)
                )
            }

            LinearProgressIndicator(
                progress = { (metric.score / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = statusColor,
                trackColor = Color.White.copy(alpha = 0.10f),
            )

            Text(
                text = "${metric.score.roundToInt()} pts",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = appColors.secondaryText
            )
        }
    }
}

@Composable
private fun FeedbackBanner(
    message: String,
    severity: String
) {
    val appColors = LocalAppColors.current
    val (bgColor, contentColor, icon) = when (severity.uppercase()) {
        "ERROR", "CRITICAL" -> Triple(appColors.error.copy(alpha = 0.15f), appColors.error, Icons.Default.Warning)
        "WARNING" -> Triple(appColors.warning.copy(alpha = 0.15f), appColors.warning, Icons.Default.Warning)
        else -> Triple(appColors.info.copy(alpha = 0.15f), appColors.info, Icons.Default.Info)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, contentColor.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WorkoutCompletionDialog(
    reps: Int,
    exerciseName: String,
    isSaving: Boolean,
    onViewDetails: () -> Unit,
    onDone: () -> Unit
) {
    val appColors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDone,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        R.string.workout_completed_message,
                        reps,
                        exerciseName
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (isSaving) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = appColors.accent
                        )
                        Text(
                            text = stringResource(R.string.saving_workout),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onViewDetails,
                colors = ButtonDefaults.buttonColors(containerColor = appColors.accent)
            ) {
                Text(
                    text = stringResource(R.string.view_details),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDone) {
                Text(
                    text = stringResource(R.string.common_done),
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun SummaryMetricRow(label: String, value: String) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = appColors.secondaryText)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = appColors.primaryText)
    }
}

@Composable
private fun PoseOverlay(
    landmarks: List<LiveLandmark>,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Canvas(modifier = modifier) {
        val canvasSize = size
        val valid = landmarks.filter { it.visible && it.confidence >= 0.38 }
        for (connection in jointConnections) {
            val a = valid.find { it.name == connection.from }
            val b = valid.find { it.name == connection.to }
            if (a != null && b != null) {
                drawLine(
                    color = appColors.success,
                    start = Offset((a.xNorm * canvasSize.width).toFloat(), (a.yNorm * canvasSize.height).toFloat()),
                    end = Offset((b.xNorm * canvasSize.width).toFloat(), (b.yNorm * canvasSize.height).toFloat()),
                    strokeWidth = 3f
                )
            }
        }
        valid.forEach { landmark ->
            drawCircle(
                color = appColors.info,
                radius = 6f,
                center = Offset((landmark.xNorm * canvasSize.width).toFloat(), (landmark.yNorm * canvasSize.height).toFloat()),
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
private fun CameraHost(
    previewView: PreviewView,
    exerciseCode: String,
    sideView: Boolean,
    isRunning: Boolean,
    useFrontCamera: Boolean,
    flashEnabled: Boolean,
    viewModel: LiveAnalysisViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: Context
) {
    val currentExerciseCode by rememberUpdatedState(exerciseCode)
    val currentSideView by rememberUpdatedState(sideView)
    val currentUseFrontCamera by rememberUpdatedState(useFrontCamera)
    val currentViewModel by rememberUpdatedState(viewModel)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val frameSequenceCounter = remember { AtomicLong(1L) }

    DisposableEffect(analysisExecutor) {
        onDispose { analysisExecutor.shutdown() }
    }

    DisposableEffect(previewView, useFrontCamera, isRunning, flashEnabled, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var camera: Camera? = null

        val bindCamera = Runnable {
            try {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                val useCases = mutableListOf<UseCase>(preview)
                if (isRunning) {
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val frameSequence = frameSequenceCounter.getAndIncrement()
                            val imageBytes = imageProxyToJpeg(imageProxy, currentUseFrontCamera)
                            if (imageBytes != null) {
                                val tempFile = File.createTempFile("live-frame-", ".jpg")
                                tempFile.writeBytes(imageBytes)
                                currentViewModel.onFrameCaptured(
                                    imageFile = tempFile,
                                    frameSequence = frameSequence,
                                    timestampMs = System.currentTimeMillis(),
                                    sideView = currentSideView,
                                    exerciseCode = currentExerciseCode
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("LiveAnalysis", "Failed to capture camera frame", e)
                        } finally {
                            imageProxy.close()
                        }
                    }
                    useCases.add(analysis)
                }
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )
                camera?.cameraControl?.enableTorch(flashEnabled && !useFrontCamera)
            } catch (e: Exception) {
                Log.e("LiveAnalysis", "Failed to bind camera", e)
            }
        }
        cameraProviderFuture.addListener(bindCamera, mainExecutor)

        onDispose {
            camera?.cameraControl?.enableTorch(false)
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            } else {
                cameraProviderFuture.addListener({
                    runCatching { cameraProviderFuture.get().unbindAll() }
                }, mainExecutor)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize())
}

private fun imageProxyToJpeg(imageProxy: ImageProxy, isFrontCamera: Boolean): ByteArray? {
    return try {
        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix().apply {
            if (rotationDegrees != 0) {
                postRotate(rotationDegrees.toFloat())
            }
            if (isFrontCamera) {
                postScale(-1f, 1f)
            }
        }
        val finalBitmap = if (rotationDegrees != 0 || isFrontCamera) {
            val transformed = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            if (transformed != bitmap) {
                bitmap.recycle()
            }
            transformed
        } else {
            bitmap
        }
        val output = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        finalBitmap.recycle()
        output.toByteArray()
    } catch (e: Exception) {
        Log.e("LiveAnalysis", "Failed to convert image proxy to jpeg", e)
        null
    }
}
