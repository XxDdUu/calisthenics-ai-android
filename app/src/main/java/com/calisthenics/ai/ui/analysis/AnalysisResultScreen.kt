package com.calisthenics.ai.ui.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.ComponentScores
import com.calisthenics.ai.domain.model.FrameMetric
import com.calisthenics.ai.domain.model.StabilityMetrics
import com.calisthenics.ai.domain.model.VideoMetadata
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.CardBackground
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Error
import com.calisthenics.ai.ui.theme.Info
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.MutedText
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import com.calisthenics.ai.ui.theme.SubtleBorder
import com.calisthenics.ai.ui.theme.Success
import com.calisthenics.ai.ui.theme.Warning
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    onAnalyzeAnother: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: AnalysisResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analysis Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        },
        containerColor = Ink
    ) { padding ->
        when {
            uiState.isLoading -> LoadingView(
                message = "Retrieving analysis report…",
                modifier = Modifier.padding(padding)
            )

            uiState.error != null -> ErrorView(
                message = uiState.error ?: "Could not load report",
                onRetry = { viewModel.loadAnalysisResult() },
                modifier = Modifier.padding(padding)
            )

            uiState.result != null -> AnalysisReportContent(
                result = uiState.result!!,
                onAnalyzeAnother = onAnalyzeAnother,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main content — scrollable report
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisReportContent(
    result: AnalysisResult,
    onAnalyzeAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Section 1 – Video Overview
        if (result.videoMetadata != null || result.mediaType == "VIDEO") {
            SectionHeader(title = "Video Overview")
            VideoOverviewSection(result = result, metadata = result.videoMetadata)
            SectionDivider()
        }

        // Section 2 – Performance Summary
        SectionHeader(title = "Performance Summary")
        PerformanceSummarySection(result = result)
        SectionDivider()

        // Section 3 – Biomechanical Analysis
        SectionHeader(title = "Biomechanical Analysis")
        BiomechanicsSection(result = result)
        SectionDivider()

        // Section 4 – Stability Analysis (only if timeline data present)
        if (result.timeline.isNotEmpty()) {
            SectionHeader(title = "Stability Analysis")
            StabilitySection(timeline = result.timeline, stability = result.stabilityMetrics)
            SectionDivider()
        }

        // Section 5 – AI Feedback
        if (result.formFeedbackList.isNotEmpty()) {
            SectionHeader(title = "AI Form Feedback")
            FeedbackSection(feedbackList = result.formFeedbackList)
            SectionDivider()
        }

        // Section 6 – Frame-by-Frame
        if (result.timeline.isNotEmpty()) {
            SectionHeader(title = "Frame-by-Frame Analysis")
            FrameTimelineSection(timeline = result.timeline)
        }

        // Analyze Another CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Elevated)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) { detectTapGestures { onAnalyzeAnother() } }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Analyze Another Video",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Accent
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 1: Video Overview
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoOverviewSection(result: AnalysisResult, metadata: VideoMetadata?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Exercise name + timestamp row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = result.exerciseName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                if (result.createdAt.isNotEmpty()) {
                    Text(
                        text = formatTimestamp(result.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }
            MetricTypeBadge(mediaType = result.mediaType)
        }

        if (metadata != null) {
            // Metadata grid
            VideoMetadataGrid(metadata = metadata, processingTimeMs = result.videoMetadata?.let { null })
        }
    }
}

@Composable
private fun VideoMetadataGrid(metadata: VideoMetadata, processingTimeMs: Double?) {
    val items = buildList {
        if (metadata.durationMs > 0) add(Pair("Duration", formatDurationMs(metadata.durationMs)))
        if (metadata.width > 0 && metadata.height > 0) add(Pair("Resolution", "${metadata.width}×${metadata.height}"))
        if (metadata.originalFps > 0) add(Pair("Original FPS", "${metadata.originalFps.roundToInt()} fps"))
        if (metadata.totalFrames > 0) add(Pair("Total Frames", "${metadata.totalFrames}"))
        if (metadata.analyzedFrames > 0) add(Pair("Analyzed Frames", "${metadata.analyzedFrames}"))
        if (metadata.samplingFps > 0) add(Pair("Sampling FPS", "%.1f fps".format(metadata.samplingFps)))
        if (metadata.fileSizeBytes > 0) add(Pair("File Size", formatFileSize(metadata.fileSizeBytes)))
        if (processingTimeMs != null) add(Pair("Processing", "%.0f ms".format(processingTimeMs)))
    }

    if (items.isEmpty()) return

    // 2-column grid
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value) ->
                    MetaInfoChip(label = label, value = value, modifier = Modifier.weight(1f))
                }
                // Fill empty slot if odd
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetaInfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedText)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryText
        )
    }
}

@Composable
private fun MetricTypeBadge(mediaType: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Accent.copy(alpha = 0.12f))
            .border(1.dp, Accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = mediaType,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Accent
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 2: Performance Summary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PerformanceSummarySection(result: AnalysisResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary hero metric: Overall Score
        result.overallScore?.let { score ->
            ScoreHeroCard(score = score)
        }

        // Secondary metrics row
        val secondaryItems = buildList {
            result.holdDurationSeconds?.let { add(Triple("Hold Duration", formatSeconds(it), Icons.Default.Timer)) }
            result.componentScores?.elbowExtension?.let { add(Triple("Elbow Extension", "${it.roundToInt()}%", Icons.Default.FitnessCenter)) }
            result.stabilityMetrics?.stabilityScore?.let { add(Triple("Stability", "${it.roundToInt()}%", Icons.Default.TrendingUp)) }
        }

        if (secondaryItems.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondaryItems.forEach { (label, value, icon) ->
                    SmallMetricCard(label = label, value = value, icon = icon, modifier = Modifier.weight(1f))
                }
            }
        }

        // Component scores breakdown (if available)
        result.componentScores?.let { scores ->
            ComponentScoresCard(scores = scores)
        }
    }
}

@Composable
private fun ScoreHeroCard(score: Double) {
    val color = when {
        score >= 80 -> Success
        score >= 60 -> Warning
        else -> Error
    }
    val label = when {
        score >= 80 -> "Excellent"
        score >= 70 -> "Good"
        score >= 60 -> "Needs Work"
        else -> "Poor Form"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0.08f), Elevated)
                )
            )
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Assessment Score",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Internal AI biomechanical assessment — not an official gymnastics score.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
            Text(
                text = "${score.roundToInt()}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun SmallMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SecondaryText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ComponentScoresCard(scores: ComponentScores) {
    val items = listOfNotNull(
        scores.bodyAlignment?.let { Triple("Body Alignment", it, 30) },
        scores.elbowExtension?.let { Triple("Elbow Extension", it, 20) },
        scores.shoulderPosition?.let { Triple("Shoulder Position", it, 20) },
        scores.hipExtension?.let { Triple("Hip Alignment", it, 15) },
        scores.stability?.let { Triple("Stability", it, 15) },
        scores.overall?.let { Triple("Overall (avg)", it, null) }
    )
    if (items.isEmpty()) return

    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { expanded = !expanded } }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = SubtleBorder)
                Spacer(modifier = Modifier.height(2.dp))
                items.forEach { (label, score, weight) ->
                    ComponentScoreRow(label = label, score = score, weight = weight)
                }
            }
        }
    }
}

@Composable
private fun ComponentScoreRow(label: String, score: Double, weight: Int?) {
    val color = when {
        score >= 80 -> Success
        score >= 60 -> Warning
        else -> Error
    }
    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "score_$label"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                weight?.let {
                    Text(
                        text = "${it}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }
            Text(
                text = "${score.roundToInt()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 3: Biomechanical Analysis
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BiomechanicsSection(result: AnalysisResult) {
    val angles = buildList {
        result.elbowAngle?.let {
            add(BiomechMetric(
                name = "Elbow Extension",
                value = "${it.roundToInt()}°",
                unit = "degrees",
                idealRange = ">= 170°",
                description = "Elbow lockout angle. Full extension (180°) is ideal; > 170° indicates good straight-arm strength.",
                score = when { it >= 170 -> 100.0; it >= 160 -> 75.0; it >= 150 -> 50.0; else -> 25.0 }
            ))
        }
        result.bodyLineAngle?.let {
            add(BiomechMetric(
                name = "Body Line / Hip Angle",
                value = "${it.roundToInt()}°",
                unit = "degrees",
                idealRange = ">= 140°",
                description = "Hip-shoulder-ankle alignment angle. Higher values indicate a more horizontal body line.",
                score = when { it >= 140 -> 100.0; it >= 120 -> 70.0; it >= 100 -> 45.0; else -> 20.0 }
            ))
        }
        result.shoulderLeanAngle?.let {
            add(BiomechMetric(
                name = "Shoulder Forward Lean",
                value = "${it.roundToInt()}°",
                unit = "degrees",
                idealRange = ">= 15°",
                description = "Forward shoulder displacement relative to wrist base. Proper lean counterbalances body weight.",
                score = when { it >= 20 -> 100.0; it >= 15 -> 85.0; it >= 10 -> 60.0; else -> 30.0 }
            ))
        }
    }

    // Also get averages from timeline if individual angles are absent
    val timelineAngles = if (result.timeline.isNotEmpty()) {
        val withPose = result.timeline.filter { it.poseDetected }
        buildList {
            withPose.mapNotNull { it.elbowAngle }.average().takeIf { !it.isNaN() }?.let {
                if (result.elbowAngle == null) add(BiomechMetric(
                    name = "Avg. Elbow Extension",
                    value = "${it.roundToInt()}°",
                    unit = "degrees (avg across video)",
                    idealRange = ">= 170°",
                    description = "Average elbow lockout angle measured across all analyzed frames.",
                    score = when { it >= 170 -> 100.0; it >= 160 -> 75.0; it >= 150 -> 50.0; else -> 25.0 }
                ))
            }
            withPose.mapNotNull { it.hipAlignment }.average().takeIf { !it.isNaN() }?.let {
                if (result.bodyLineAngle == null) add(BiomechMetric(
                    name = "Avg. Hip Alignment",
                    value = "${it.roundToInt()}°",
                    unit = "degrees (avg across video)",
                    idealRange = ">= 140°",
                    description = "Average hip alignment measured across all analyzed frames.",
                    score = when { it >= 140 -> 100.0; it >= 120 -> 70.0; it >= 100 -> 45.0; else -> 20.0 }
                ))
            }
            withPose.mapNotNull { it.kneeAngle }.average().takeIf { !it.isNaN() }?.let {
                add(BiomechMetric(
                    name = "Avg. Knee Extension",
                    value = "${it.roundToInt()}°",
                    unit = "degrees (avg across video)",
                    idealRange = ">= 140°",
                    description = "Average knee extension angle. Near 180° = fully straight legs.",
                    score = when { it >= 170 -> 100.0; it >= 150 -> 70.0; it >= 130 -> 45.0; else -> 20.0 }
                ))
            }
            withPose.mapNotNull { it.bodyLineDeviation }.average().takeIf { !it.isNaN() }?.let { avg ->
                add(BiomechMetric(
                    name = "Avg. Body Line Deviation",
                    value = "%.1f°".format(avg),
                    unit = "degrees from horizontal",
                    idealRange = "< 15°",
                    description = "Average body tilt deviation from the horizontal reference. Lower = more horizontal body.",
                    score = when { avg < 10 -> 100.0; avg < 15 -> 80.0; avg < 25 -> 55.0; else -> 25.0 }
                ))
            }
        }
    } else emptyList()

    val allMetrics = angles + timelineAngles

    if (allMetrics.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Elevated)
                .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No biomechanical measurements available for this analysis.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        allMetrics.forEach { metric ->
            BiomechMetricCard(metric = metric)
        }
    }
}

private data class BiomechMetric(
    val name: String,
    val value: String,
    val unit: String,
    val idealRange: String,
    val description: String,
    val score: Double
)

@Composable
private fun BiomechMetricCard(metric: BiomechMetric) {
    val statusColor = when {
        metric.score >= 80 -> Success
        metric.score >= 60 -> Warning
        else -> Error
    }
    val statusLabel = when {
        metric.score >= 80 -> "Good"
        metric.score >= 60 -> "Acceptable"
        else -> "Needs Work"
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
            .pointerInput(Unit) { detectTapGestures { expanded = !expanded } }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryText
                )
                Text(text = "Target: ${metric.idealRange}", style = MaterialTheme.typography.labelSmall, color = MutedText)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp)) {
                HorizontalDivider(color = SubtleBorder)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = metric.description, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unit: ${metric.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 4: Stability Analysis with time-series chart
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StabilitySection(timeline: List<FrameMetric>, stability: StabilityMetrics?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stability summary chips
        stability?.let { s ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StabilityChip("Stability Score", "${s.stabilityScore.roundToInt()}%", modifier = Modifier.weight(1f))
                StabilityChip("Avg Confidence", "%.0f%%".format(s.averagePoseConfidence * 100), modifier = Modifier.weight(1f))
                StabilityChip("Max Deviation", "%.1f°".format(s.maxBodyLineDeviation), modifier = Modifier.weight(1f))
            }
        }

        // Body-line deviation chart
        val deviationPoints = timeline.filter { it.poseDetected && it.bodyLineDeviation != null }
        if (deviationPoints.isNotEmpty()) {
            TimeSeriesChart(
                title = "Body-Line Deviation over Time",
                yLabel = "Deviation (°)",
                points = deviationPoints.map { Pair(it.timestampSec.toFloat(), it.bodyLineDeviation!!.toFloat()) },
                warningThreshold = 15f,
                color = Info
            )
        }

        // Form score over time chart
        val formPoints = timeline.filter { it.poseDetected && it.formScore > 0 }
        if (formPoints.isNotEmpty()) {
            TimeSeriesChart(
                title = "Form Score over Time",
                yLabel = "Score (0–100)",
                points = formPoints.map { Pair(it.timestampSec.toFloat(), it.formScore.toFloat()) },
                warningThreshold = 70f,
                warningInvert = true, // below threshold = bad
                color = Accent
            )
        }
    }
}

@Composable
private fun StabilityChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryText)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedText, textAlign = TextAlign.Center)
    }
}

/**
 * Reusable time-series chart for analysis data.
 * X-axis = timestamp in seconds, Y-axis = metric value.
 * warningThreshold marks a reference line. Points above/below threshold shown differently.
 */
@Composable
private fun TimeSeriesChart(
    title: String,
    yLabel: String,
    points: List<Pair<Float, Float>>,
    warningThreshold: Float? = null,
    warningInvert: Boolean = false,
    color: Color = Accent
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points) { mutableIntStateOf(points.size - 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = PrimaryText)
            Text(text = yLabel, style = MaterialTheme.typography.labelSmall, color = MutedText)
        }

        // Selected point tooltip
        val selected = points.getOrNull(selectedIndex)
        if (selected != null) {
            val (ts, value) = selected
            val isWarning = if (warningThreshold != null) {
                if (warningInvert) value < warningThreshold else value > warningThreshold
            } else false
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Ink)
                    .border(1.dp, if (isWarning) Warning else SubtleBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "t = %.2fs".format(ts), style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) Warning else color
                )
            }
        }

        // Canvas chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val chartBorder = Border
            val chartColor = color
            val warnColor = Warning
            val chartSecondary = SecondaryText
            val chartPrimary = PrimaryText

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { tap ->
                            val w = size.width.toFloat()
                            val n = points.size
                            if (n > 1) {
                                val step = w / (n - 1)
                                selectedIndex = (tap.x / step).toInt().coerceIn(0, n - 1)
                            }
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            val w = size.width.toFloat()
                            val n = points.size
                            if (n > 1) {
                                val step = w / (n - 1)
                                val delta = (dragAmount / step).toInt()
                                selectedIndex = (selectedIndex + delta).coerceIn(0, n - 1)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val padTop = 12.dp.toPx()
                val padBottom = 20.dp.toPx()
                val chartH = h - padTop - padBottom

                val minV = points.minOf { it.second }
                val maxV = points.maxOf { it.second }
                val range = (maxV - minV).coerceAtLeast(1f)

                fun yFor(v: Float) = padTop + (1f - (v - minV) / range) * chartH
                fun xFor(i: Int) = if (points.size == 1) w / 2f else i * w / (points.size - 1)

                // Grid lines
                repeat(4) { i ->
                    val y = padTop + chartH / 4f * i
                    drawLine(chartBorder.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 1.dp.toPx())
                }

                // Warning threshold line
                if (warningThreshold != null && warningThreshold in minV..maxV) {
                    val wy = yFor(warningThreshold)
                    drawLine(warnColor.copy(alpha = 0.5f), Offset(0f, wy), Offset(w, wy), 1.dp.toPx())
                }

                // X-axis labels (first + last timestamp)
                // (Skipped for simplicity; labels would need drawContext.canvas.nativeCanvas)

                // Fill
                if (points.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(xFor(0), h - padBottom)
                        points.forEachIndexed { i, (_, v) -> lineTo(xFor(i), yFor(v)) }
                        lineTo(xFor(points.size - 1), h - padBottom)
                        close()
                    }
                    drawPath(
                        fillPath,
                        Brush.verticalGradient(listOf(chartColor.copy(alpha = 0.3f), Color.Transparent), padTop, h - padBottom)
                    )

                    // Line
                    val linePath = Path().apply {
                        moveTo(xFor(0), yFor(points[0].second))
                        points.forEachIndexed { i, (_, v) -> if (i > 0) lineTo(xFor(i), yFor(v)) }
                    }
                    drawPath(linePath, chartColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                }

                // Data points
                points.forEachIndexed { i, (_, v) ->
                    val cx = xFor(i)
                    val cy = yFor(v)
                    val isSelected = i == selectedIndex
                    if (isSelected) {
                        drawLine(chartSecondary.copy(alpha = 0.4f), Offset(cx, padTop), Offset(cx, h - padBottom), 1.dp.toPx())
                    }
                    drawCircle(
                        if (isSelected) chartPrimary else chartColor,
                        radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 5: AI Feedback
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedbackSection(feedbackList: List<String>) {
    // Classify feedback heuristically by prefix keywords
    val positives = feedbackList.filter { fb ->
        val l = fb.lowercase()
        l.startsWith("solid") || l.startsWith("great") || l.startsWith("good") ||
                l.startsWith("excellent") || l.startsWith("maintained") || l.startsWith("well")
    }
    val recommendations = feedbackList.filter { fb ->
        val l = fb.lowercase()
        l.startsWith("work on") || l.startsWith("practice") || l.startsWith("increase") ||
                l.startsWith("focus") || l.startsWith("improve")
    }
    val general = feedbackList - positives.toSet() - recommendations.toSet()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (positives.isNotEmpty()) {
            FeedbackGroup(title = "Strengths", items = positives, icon = Icons.Default.CheckCircle, iconColor = Success)
        }
        if (general.isNotEmpty()) {
            FeedbackGroup(title = "Overall Assessment", items = general, icon = Icons.Default.Info, iconColor = Info)
        }
        if (recommendations.isNotEmpty()) {
            FeedbackGroup(title = "Recommended Corrections", items = recommendations, icon = Icons.Default.Warning, iconColor = Warning)
        }
    }
}

@Composable
private fun FeedbackGroup(title: String, items: List<String>, icon: ImageVector, iconColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = PrimaryText)
        }
        HorizontalDivider(color = SubtleBorder)
        items.forEach { fb ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(iconColor)
                )
                Text(text = fb, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 6: Frame-by-Frame Inspection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FrameTimelineSection(timeline: List<FrameMetric>) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Scrub through ${timeline.size} analyzed frames.",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText
        )

        // Horizontal scrubber
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(timeline) { idx, frame ->
                val isSelected = idx == selectedIndex
                val dotColor = when {
                    !frame.poseDetected -> MutedText
                    frame.isValidHold -> Success
                    frame.formScore > 0 -> Warning
                    else -> Error
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Accent.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) Accent else SubtleBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .pointerInput(idx) { detectTapGestures { selectedIndex = idx } }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1fs".format(frame.timestampSec),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (isSelected) Accent else MutedText,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        // Frame detail card
        timeline.getOrNull(selectedIndex)?.let { frame ->
            FrameDetailCard(frame = frame, index = selectedIndex, total = timeline.size)
        }
    }
}

@Composable
private fun FrameDetailCard(frame: FrameMetric, index: Int, total: Int) {
    val holdColor = when {
        frame.isValidHold -> Success
        !frame.poseDetected -> MutedText
        frame.formScore > 60 -> Warning
        else -> Error
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Elevated)
            .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Frame ${index + 1} of $total",
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText
            )
            Text(
                text = "t = %.2fs".format(frame.timestampSec),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
        }

        // Status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(
                label = if (frame.poseDetected) "Pose Detected" else "No Pose",
                color = if (frame.poseDetected) Success else Error,
                modifier = Modifier.weight(1f)
            )
            StatusPill(
                label = if (frame.isValidHold) "Valid Hold" else frame.holdState?.replace("_", " ") ?: "No Hold",
                color = holdColor,
                modifier = Modifier.weight(1f)
            )
        }

        if (frame.poseDetected) {
            HorizontalDivider(color = SubtleBorder)

            // Angles grid
            val angleItems = listOfNotNull(
                frame.elbowAngle?.let { Pair("Elbow", "${it.roundToInt()}°") },
                frame.shoulderAngle?.let { Pair("Shoulder", "${it.roundToInt()}°") },
                frame.hipAlignment?.let { Pair("Hip", "${it.roundToInt()}°") },
                frame.bodyLineDeviation?.let { Pair("Body Tilt", "%.1f°".format(it)) },
                frame.kneeAngle?.let { Pair("Knee", "${it.roundToInt()}°") },
                frame.poseConfidence?.let { Pair("Confidence", "%.0f%%".format(it * 100)) }
            )

            if (angleItems.isNotEmpty()) {
                angleItems.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (label, value) ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PrimaryText)
                                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            // Form score bar
            if (frame.formScore > 0) {
                val scoreColor = when {
                    frame.formScore >= 80 -> Success
                    frame.formScore >= 60 -> Warning
                    else -> Error
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Form Score", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    Text(
                        text = "${frame.formScore.roundToInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((frame.formScore / 100.0).toFloat().coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(scoreColor)
                    )
                }
            }

            // Key issues
            if (frame.keyIssues.isNotEmpty()) {
                HorizontalDivider(color = SubtleBorder)
                frame.keyIssues.take(3).forEach { issue ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Warning, modifier = Modifier.size(12.dp).padding(top = 2.dp))
                        Text(text = issue, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color, textAlign = TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MutedText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = SubtleBorder
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Format utilities
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDurationMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min > 0) "%d:%02ds".format(min, sec) else "${sec}s"
}

private fun formatSeconds(sec: Double): String {
    val min = (sec / 60).toInt()
    val s = (sec % 60).toInt()
    return if (min > 0) "%d:%02ds".format(min, s) else "%.1fs".format(sec)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}

private fun formatTimestamp(raw: String): String {
    // Best-effort formatting; raw may be ISO-8601 or empty
    return try {
        raw.take(10) // "YYYY-MM-DD"
    } catch (e: Exception) {
        raw
    }
}
