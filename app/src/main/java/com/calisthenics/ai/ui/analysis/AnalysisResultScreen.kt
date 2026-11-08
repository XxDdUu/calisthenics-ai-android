package com.calisthenics.ai.ui.analysis

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.calisthenics.ai.BuildConfig
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.ComponentScores
import com.calisthenics.ai.domain.model.FrameMetric
import com.calisthenics.ai.domain.model.KeypointFrameModel
import com.calisthenics.ai.domain.model.KeypointItemModel
import com.calisthenics.ai.domain.model.StabilityMetrics
import com.calisthenics.ai.domain.model.VideoKeypointsDataModel
import com.calisthenics.ai.domain.model.VideoMetadata
import com.calisthenics.ai.ui.components.ErrorView
import com.calisthenics.ai.ui.components.LoadingView
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// COCO Topology for 17 Keypoints
// ─────────────────────────────────────────────────────────────────────────────

private val COCO_CONNECTIONS = listOf(
    Pair(0, 1), Pair(0, 2), Pair(1, 3), Pair(2, 4), // Facial features
    Pair(5, 6), // Shoulders
    Pair(5, 7), Pair(7, 9), // Left Arm
    Pair(6, 8), Pair(8, 10), // Right Arm
    Pair(5, 11), Pair(6, 12), // Torso
    Pair(11, 12), // Hips
    Pair(11, 13), Pair(13, 15), // Left Leg
    Pair(12, 14), Pair(14, 16)  // Right Leg
)

private val COCO_NAMES = listOf(
    "nose", "left_eye", "right_eye", "left_ear", "right_ear",
    "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
    "left_wrist", "right_wrist", "left_hip", "right_hip",
    "left_knee", "right_knee", "left_ankle", "right_ankle"
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen Entry Point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    onAnalyzeAnother: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: AnalysisResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val seekRequestMs by viewModel.seekRequestMs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Video Analysis",
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
                message = "Retrieving AI analysis report…",
                modifier = Modifier.padding(padding)
            )

            uiState.error != null -> ErrorView(
                message = uiState.error ?: "Could not load report",
                onRetry = { viewModel.loadAnalysisResult() },
                modifier = Modifier.padding(padding)
            )

            uiState.result != null -> AnalysisReportContent(
                result = uiState.result!!,
                authToken = uiState.authToken,
                selectedMode = uiState.selectedVideoMode,
                onModeChange = viewModel::setVideoMode,
                showSkeletonOnOriginal = uiState.showSkeletonOnOriginal,
                onToggleSkeletonOnOriginal = viewModel::toggleSkeletonOnOriginal,
                currentPlaybackPositionMs = uiState.currentPlaybackPositionMs,
                currentKeypointFrame = uiState.currentKeypointFrame,
                seekRequestMs = seekRequestMs,
                onClearSeekRequest = viewModel::clearSeekRequest,
                onSeekToTimestamp = viewModel::requestSeek,
                onPositionChanged = viewModel::updatePlaybackPosition,
                onAnalyzeAnother = onAnalyzeAnother,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Content — Scrollable Report
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisReportContent(
    result: AnalysisResult,
    authToken: String?,
    selectedMode: VideoDisplayMode,
    onModeChange: (VideoDisplayMode) -> Unit,
    showSkeletonOnOriginal: Boolean,
    onToggleSkeletonOnOriginal: () -> Unit,
    currentPlaybackPositionMs: Long,
    currentKeypointFrame: KeypointFrameModel?,
    seekRequestMs: Long?,
    onClearSeekRequest: () -> Unit,
    onSeekToTimestamp: (Long) -> Unit,
    onPositionChanged: (Long) -> Unit,
    onAnalyzeAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // SECTION A – Dual Video Comparison Player (Original vs AI Skeleton)
        if (result.mediaType == "VIDEO" || !result.originalVideoUrl.isNullOrBlank() || !result.annotatedVideoUrl.isNullOrBlank()) {
            SectionHeader(title = "Section A: Video Comparison")
            VideoComparisonPlayerSection(
                result = result,
                authToken = authToken,
                selectedMode = selectedMode,
                onModeChange = onModeChange,
                showSkeletonOnOriginal = showSkeletonOnOriginal,
                onToggleSkeletonOnOriginal = onToggleSkeletonOnOriginal,
                currentFrame = currentKeypointFrame,
                seekRequestMs = seekRequestMs,
                onClearSeekRequest = onClearSeekRequest,
                onPositionChanged = onPositionChanged
            )
            SectionDivider()
        }

        // SECTION B – Performance & Form Score
        SectionHeader(title = "Section B: Performance Score")
        PerformanceSummarySection(result = result)
        SectionDivider()

        // SECTION C – Synchronized Keypoint Timeline
        if (result.keypointsData != null || currentKeypointFrame != null) {
            SectionHeader(title = "Section C: Keypoint Timeline & Pose")
            KeypointTimelineSyncSection(
                currentPosMs = currentPlaybackPositionMs,
                currentFrame = currentKeypointFrame,
                keypointsData = result.keypointsData,
                totalDurationMs = result.videoMetadata?.durationMs ?: 0L,
                onSeekToTimestamp = onSeekToTimestamp
            )
            SectionDivider()
        }

        // SECTION D – AI Form Feedback
        SectionHeader(title = "Section D: AI Form Feedback")
        FeedbackSection(result = result)
        SectionDivider()

        // SECTION E – Biomechanical Analysis & Stability
        SectionHeader(title = "Biomechanical Analysis")
        BiomechanicsSection(result = result)
        SectionDivider()

        if (result.timeline.isNotEmpty()) {
            SectionHeader(title = "Stability Analysis")
            StabilitySection(timeline = result.timeline, stability = result.stabilityMetrics)
            SectionDivider()

            SectionHeader(title = "Frame-by-Frame Timeline")
            FrameTimelineSection(timeline = result.timeline)
            SectionDivider()
        }

        // Video Overview & Metadata
        if (result.videoMetadata != null) {
            SectionHeader(title = "Video Metadata")
            VideoOverviewSection(result = result, metadata = result.videoMetadata)
            SectionDivider()
        }

        // CTA: Analyze Another Video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
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
// URL Resolver Helper
// ─────────────────────────────────────────────────────────────────────────────

private fun resolveMediaUrl(rawUrl: String?, token: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
    var absoluteUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
        rawUrl
    } else {
        "$baseUrl/${rawUrl.trimStart('/')}"
    }

    // Security & Playback Fix: Upgrade http to https if base URL is HTTPS or domain matches backend host.
    // This prevents Android's NetworkSecurityConfig from throwing CleartextNotPermittedException.
    if ((baseUrl.startsWith("https://") || absoluteUrl.contains("sky-enterprise.id.vn")) && absoluteUrl.startsWith("http://")) {
        absoluteUrl = "https://" + absoluteUrl.substring("http://".length)
    }

    return if (!token.isNullOrBlank() && !absoluteUrl.contains("token=")) {
        if (absoluteUrl.contains("?")) "$absoluteUrl&token=$token" else "$absoluteUrl?token=$token"
    } else {
        absoluteUrl
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section A: Dual Video Comparison Player
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun VideoComparisonPlayerSection(
    result: AnalysisResult,
    authToken: String?,
    selectedMode: VideoDisplayMode,
    onModeChange: (VideoDisplayMode) -> Unit,
    showSkeletonOnOriginal: Boolean,
    onToggleSkeletonOnOriginal: () -> Unit,
    currentFrame: KeypointFrameModel?,
    seekRequestMs: Long?,
    onClearSeekRequest: () -> Unit,
    onPositionChanged: (Long) -> Unit
) {
    val context = LocalContext.current
    val originalUrl = remember(result.originalVideoUrl, authToken) {
        resolveMediaUrl(result.originalVideoUrl, authToken)
    }
    val annotatedUrl = remember(result.annotatedVideoUrl, result.mediaUrl, authToken) {
        resolveMediaUrl(result.annotatedVideoUrl ?: result.mediaUrl, authToken)
    }

    val activeUrl = if (selectedMode == VideoDisplayMode.ANNOTATED) {
        annotatedUrl ?: originalUrl
    } else {
        originalUrl ?: annotatedUrl
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(result.videoMetadata?.durationMs ?: 0L) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosMs by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(context, authToken) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("CalisthenicsAI-Android")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        if (!authToken.isNullOrBlank()) {
            httpDataSourceFactory.setDefaultRequestProperties(mapOf("Authorization" to "Bearer $authToken"))
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = false
            }
    }

    // Handle seek requests from timeline
    LaunchedEffect(seekRequestMs) {
        seekRequestMs?.let { targetMs ->
            exoPlayer.seekTo(targetMs)
            currentPosMs = targetMs
            onPositionChanged(targetMs)
            onClearSeekRequest()
        }
    }

    // Set or switch media item when activeUrl changes, preserving position
    LaunchedEffect(activeUrl) {
        if (!activeUrl.isNullOrBlank()) {
            playbackError = null
            val savedPos = exoPlayer.currentPosition
            val wasPlaying = exoPlayer.playWhenReady
            val mediaItem = MediaItem.fromUri(Uri.parse(activeUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (savedPos > 0) {
                exoPlayer.seekTo(savedPos)
            }
            exoPlayer.playWhenReady = wasPlaying
        }
    }

    // Lifecycle & playback listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    if (totalDurationMs <= 0L && exoPlayer.duration > 0L) {
                        totalDurationMs = exoPlayer.duration
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("VideoComparisonPlayer", "ExoPlayer playback error: ${error.errorCodeName}", error)
                val message = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Network error connecting to video stream (${error.errorCodeName})"
                    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "Security restriction: Cleartext HTTP not permitted"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "Unsupported or corrupted video format"
                    else -> error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
                }
                playbackError = message
                isPlaying = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Real-time position tracking
    LaunchedEffect(exoPlayer, isUserScrubbing) {
        while (isActive) {
            if (!isUserScrubbing && exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                currentPosMs = pos
                onPositionChanged(pos)
                if (exoPlayer.duration > 0 && totalDurationMs <= 0) {
                    totalDurationMs = exoPlayer.duration
                }
            }
            delay(100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Toggle Switch: [ Original ] [ AI Skeleton ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Elevated)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isOriginal = selectedMode == VideoDisplayMode.ORIGINAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isOriginal) Accent else Color.Transparent)
                    .pointerInput(Unit) { detectTapGestures { onModeChange(VideoDisplayMode.ORIGINAL) } }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Original Video",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Medium,
                    color = if (isOriginal) Color.Black else SecondaryText
                )
            }

            val isAnnotated = selectedMode == VideoDisplayMode.ANNOTATED
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAnnotated) Accent else Color.Transparent)
                    .pointerInput(Unit) { detectTapGestures { onModeChange(VideoDisplayMode.ANNOTATED) } }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI Skeleton Video",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isAnnotated) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAnnotated) Color.Black else SecondaryText
                )
            }
        }

        // Video Player Box (16:9 ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // When in ORIGINAL mode and overlay enabled, draw skeleton
            // Note: when in ANNOTATED mode, the skeleton is already burned into the video frames, so do not draw twice
            if (selectedMode == VideoDisplayMode.ORIGINAL && showSkeletonOnOriginal && currentFrame != null) {
                SkeletonOverlayCanvas(
                    keypoints = currentFrame.keypoints,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Central Play/Pause Tap Target
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.5.dp, Accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Error overlay with Retry
            if (playbackError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Playback Error",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = playbackError ?: "Playback failed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                playbackError = null
                                activeUrl?.let { url ->
                                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Playback", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Badge overlay indicating active video source
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (selectedMode == VideoDisplayMode.ANNOTATED) "AI Skeleton Overlay" else "Original Stream",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Custom Player Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Elevated)
                .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Accent
                )
            }

            val displayPos = if (isUserScrubbing) scrubPosMs.toLong() else currentPosMs
            val maxRange = if (totalDurationMs > 0) totalDurationMs.toFloat() else 1f

            Slider(
                value = displayPos.coerceIn(0L, totalDurationMs).toFloat(),
                onValueChange = { newVal ->
                    isUserScrubbing = true
                    scrubPosMs = newVal
                },
                onValueChangeFinished = {
                    isUserScrubbing = false
                    val targetMs = scrubPosMs.toLong()
                    exoPlayer.seekTo(targetMs)
                    currentPosMs = targetMs
                    onPositionChanged(targetMs)
                },
                valueRange = 0f..maxRange,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Accent,
                    activeTrackColor = Accent,
                    inactiveTrackColor = Border
                )
            )

            Text(
                text = "${formatDurationMs(displayPos)} / ${formatDurationMs(totalDurationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText
            )

            // Overlay toggle button for ORIGINAL mode
            if (selectedMode == VideoDisplayMode.ORIGINAL) {
                IconButton(
                    onClick = onToggleSkeletonOnOriginal,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (showSkeletonOnOriginal) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Skeleton Overlay",
                        tint = if (showSkeletonOnOriginal) Accent else MutedText
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Skeleton Canvas Component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkeletonOverlayCanvas(
    keypoints: List<KeypointItemModel>?,
    modifier: Modifier = Modifier,
    confidenceThreshold: Float = 0.35f,
    jointColor: Color = Color(0xFF00E676),
    boneColor: Color = Color(0xFF00E5FF)
) {
    if (keypoints.isNullOrEmpty()) return

    val kpMap = remember(keypoints) {
        val map = mutableMapOf<Int, KeypointItemModel>()
        keypoints.forEachIndexed { idx, kp ->
            val mappedIdx = COCO_NAMES.indexOf(kp.name.lowercase()).takeIf { it >= 0 } ?: idx
            map[mappedIdx] = kp
        }
        map
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Draw bones (connected lines)
        for ((idx1, idx2) in COCO_CONNECTIONS) {
            val p1 = kpMap[idx1]
            val p2 = kpMap[idx2]
            if (p1 != null && p2 != null &&
                p1.confidence >= confidenceThreshold &&
                p2.confidence >= confidenceThreshold
            ) {
                drawLine(
                    color = boneColor,
                    start = Offset(p1.x * canvasWidth, p1.y * canvasHeight),
                    end = Offset(p2.x * canvasWidth, p2.y * canvasHeight),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 2. Draw joints (circles)
        for ((_, p) in kpMap) {
            if (p.confidence >= confidenceThreshold) {
                val center = Offset(p.x * canvasWidth, p.y * canvasHeight)
                drawCircle(
                    color = Color.Black,
                    radius = 5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = jointColor,
                    radius = 3.5.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section B: Performance & Scores
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PerformanceSummarySection(result: AnalysisResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Score Card
        result.overallScore?.let { score ->
            ScoreHeroCard(score = score)
        }

        // Metric Badges Row
        val secondaryItems = buildList {
            result.componentScores?.bodyAlignment?.let {
                add(Triple("Form Accuracy", "${it.roundToInt()}%", Icons.Default.Speed))
            }
            result.stabilityMetrics?.stabilityScore?.let {
                add(Triple("Stability", "${it.roundToInt()}%", Icons.Default.TrendingUp))
            }
            result.componentScores?.elbowExtension?.let {
                add(Triple("Elbow Extension", "${it.roundToInt()}%", Icons.Default.FitnessCenter))
            }
            result.holdDurationSeconds?.let {
                add(Triple("Duration", formatSeconds(it), Icons.Default.Timer))
            }
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

        // Component breakdown card
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
        score >= 80 -> "Excellent Form"
        score >= 70 -> "Good Form"
        score >= 60 -> "Needs Improvement"
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
                    text = "AI Video Assessment",
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
                    text = "Movement assessed via YOLOv8 Pose biomechanical scoring.",
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
        scores.overall?.let { Triple("Overall Assessment", it, null) }
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
                text = "Form Accuracy Breakdown",
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
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat() / 100f,
        animationSpec = tween(600),
        label = "score_bar"
    )
    val color = when {
        score >= 80 -> Success
        score >= 60 -> Warning
        else -> Error
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = PrimaryText)
                if (weight != null) {
                    Text(text = "($weight%)", style = MaterialTheme.typography.labelSmall, color = MutedText)
                }
            }
            Text(
                text = "${score.roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SubtleBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedScore.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section C: Keypoint Timeline & Synchronized Pose Inspector
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KeypointTimelineSyncSection(
    currentPosMs: Long,
    currentFrame: KeypointFrameModel?,
    keypointsData: VideoKeypointsDataModel?,
    totalDurationMs: Long,
    onSeekToTimestamp: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (currentFrame != null) Success else Warning)
                )
                Text(
                    text = if (currentFrame != null) "Pose Synchronized" else "Seeking Pose…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
            }
            Text(
                text = "Frame #${currentFrame?.frameIndex ?: 0} • ${currentPosMs} ms",
                style = MaterialTheme.typography.labelMedium,
                color = Accent
            )
        }

        // 2D Skeleton Wireframe Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated),
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "17 COCO Keypoints Wireframe",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryText
                    )
                    val validCount = currentFrame?.keypoints?.count { it.confidence >= 0.4f } ?: 0
                    Text(
                        text = "$validCount/17 joints active",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (validCount >= 10) Success else Warning
                    )
                }

                // Mini wireframe canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentFrame != null && currentFrame.keypoints.isNotEmpty()) {
                        SkeletonOverlayCanvas(
                            keypoints = currentFrame.keypoints,
                            modifier = Modifier.fillMaxSize(),
                            confidenceThreshold = 0.35f
                        )
                    } else {
                        Text(
                            text = "No keypoints detected for this timestamp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }

                // Key joints table
                val jointsToDisplay = listOf(
                    "left_shoulder", "right_shoulder",
                    "left_elbow", "right_elbow",
                    "left_wrist", "right_wrist",
                    "left_hip", "right_hip",
                    "left_knee", "right_knee",
                    "left_ankle", "right_ankle"
                )
                val displayedKeypoints = currentFrame?.keypoints?.filter {
                    it.name.lowercase() in jointsToDisplay
                } ?: emptyList()

                if (displayedKeypoints.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(displayedKeypoints) { kp ->
                            KeypointJointChip(kp = kp)
                        }
                    }
                }
            }
        }

        // Frame timeline scrubber buttons
        val frames = keypointsData?.frames ?: emptyList()
        if (frames.isNotEmpty()) {
            Text(
                text = "Keypoint Sampled Frames (${frames.size} frames)",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(frames) { f ->
                    val isSelected = currentFrame?.frameIndex == f.frameIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Accent else Elevated)
                            .border(1.dp, if (isSelected) Accent else SubtleBorder, RoundedCornerShape(8.dp))
                            .pointerInput(Unit) { detectTapGestures { onSeekToTimestamp(f.timestampMs) } }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#${f.frameIndex} (${f.timestampMs}ms)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else SecondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypointJointChip(kp: KeypointItemModel) {
    val confPercent = (kp.confidence * 100).roundToInt()
    val confColor = when {
        confPercent >= 80 -> Success
        confPercent >= 50 -> Warning
        else -> Error
    }
    val friendlyName = kp.name.replace("_", " ").replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, SubtleBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = friendlyName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(confColor.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$confPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = confColor,
                    fontSize = 9.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "X: %.2f  Y: %.2f".format(kp.x, kp.y),
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
            fontSize = 10.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section D: AI Form Feedback
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeedbackSection(result: AnalysisResult) {
    val feedbackList = result.formFeedbackList
    val compScores = result.componentScores

    val shoulderStatus = when {
        compScores?.shoulderPosition == null -> "Not Assessed"
        compScores.shoulderPosition >= 80 -> "Good"
        compScores.shoulderPosition >= 60 -> "Moderate"
        else -> "Needs Improvement"
    }
    val hipStatus = when {
        compScores?.hipExtension == null -> "Not Assessed"
        compScores.hipExtension >= 80 -> "Good"
        compScores.hipExtension >= 60 -> "Moderate"
        else -> "Needs Improvement"
    }
    val stabilityStatus = when {
        compScores?.stability == null -> "Not Assessed"
        compScores.stability >= 80 -> "High"
        compScores.stability >= 60 -> "Moderate"
        else -> "Needs Improvement"
    }
    val elbowStatus = when {
        compScores?.elbowExtension == null -> "Not Assessed"
        compScores.elbowExtension >= 80 -> "Good"
        compScores.elbowExtension >= 60 -> "Moderate"
        else -> "Needs Improvement"
    }

    val statusItems = listOf(
        Pair("Shoulder alignment", shoulderStatus),
        Pair("Hip alignment", hipStatus),
        Pair("Body stability", stabilityStatus),
        Pair("Elbow positioning", elbowStatus)
    )

    val positives = feedbackList.filter { fb ->
        val l = fb.lowercase()
        l.startsWith("solid") || l.startsWith("great") || l.startsWith("good") ||
                l.startsWith("excellent") || l.startsWith("maintained") || l.startsWith("well")
    }
    val recommendations = feedbackList.filter { fb ->
        val l = fb.lowercase()
        l.startsWith("work on") || l.startsWith("practice") || l.startsWith("increase") ||
                l.startsWith("focus") || l.startsWith("improve") || l.contains("deviat") ||
                l.contains("bend") || l.contains("drop") || l.contains("pike")
    }
    val general = feedbackList - positives.toSet() - recommendations.toSet()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Form alignment status matrix
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Elevated),
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Alignment & Stability Checklist",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryText
                )
                HorizontalDivider(color = SubtleBorder)
                statusItems.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (label, status) ->
                            val color = when (status) {
                                "Good", "High" -> Success
                                "Moderate" -> Warning
                                "Needs Improvement" -> Error
                                else -> SecondaryText
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryText,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                StatusPill(label = status, color = color)
                            }
                        }
                    }
                }
            }
        }

        if (recommendations.isNotEmpty()) {
            FeedbackGroup(title = "Identified Mistakes & Form Corrections", items = recommendations, icon = Icons.Default.Warning, iconColor = Warning)
        }
        if (positives.isNotEmpty()) {
            FeedbackGroup(title = "Form Strengths", items = positives, icon = Icons.Default.CheckCircle, iconColor = Success)
        }
        if (general.isNotEmpty()) {
            FeedbackGroup(title = "AI Suggestions & Coaching Cues", items = general, icon = Icons.Default.Info, iconColor = Info)
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
// Biomechanical Details & Frame Inspector
// ─────────────────────────────────────────────────────────────────────────────

private data class BiomechMetric(
    val name: String,
    val valueStr: String,
    val targetRange: String,
    val description: String,
    val status: String,
    val icon: ImageVector
)

@Composable
private fun BiomechanicsSection(result: AnalysisResult) {
    val metrics = buildList {
        result.bodyLineAngle?.let {
            add(
                BiomechMetric(
                    name = "Body Line Angle",
                    valueStr = "%.1f°".format(it),
                    targetRange = "170° – 180°",
                    description = "Deviation from straight body line (shoulder to ankle).",
                    status = if (it >= 170.0) "Optimal" else if (it >= 160.0) "Acceptable" else "Needs Work",
                    icon = Icons.Default.TrendingUp
                )
            )
        }
        result.elbowAngle?.let {
            add(
                BiomechMetric(
                    name = "Elbow Extension",
                    valueStr = "%.1f°".format(it),
                    targetRange = "175° – 180°",
                    description = "Arm straightness during hold.",
                    status = if (it >= 175.0) "Locked Out" else if (it >= 165.0) "Slight Bend" else "Bent Arm",
                    icon = Icons.Default.FitnessCenter
                )
            )
        }
        result.shoulderLeanAngle?.let {
            add(
                BiomechMetric(
                    name = "Shoulder Lean",
                    valueStr = "%.1f°".format(it),
                    targetRange = "Exercise-specific",
                    description = "Shoulder position forward of wrists.",
                    status = "Detected",
                    icon = Icons.Default.Speed
                )
            )
        }
    }

    if (metrics.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.forEach { metric ->
            BiomechMetricCard(metric = metric)
        }
    }
}

@Composable
private fun BiomechMetricCard(metric: BiomechMetric) {
    val statusColor = when (metric.status) {
        "Optimal", "Locked Out" -> Success
        "Acceptable", "Slight Bend" -> Warning
        else -> SecondaryText
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = metric.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PrimaryText)
                    StatusPill(label = metric.status, color = statusColor)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = metric.description, style = MaterialTheme.typography.labelSmall, color = MutedText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Target: ${metric.targetRange}", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
            }
            Text(
                text = metric.valueStr,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Accent
            )
        }
    }
}

@Composable
private fun StabilitySection(timeline: List<FrameMetric>, stability: StabilityMetrics?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (stability != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StabilityChip(
                    label = "Stability Score",
                    value = "${stability.stabilityScore.roundToInt()}%",
                    modifier = Modifier.weight(1f)
                )
                StabilityChip(
                    label = "Pose Confidence",
                    value = "%.0f%%".format(stability.averagePoseConfidence * 100),
                    modifier = Modifier.weight(1f)
                )
            }
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MutedText)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
    }
}

@Composable
private fun FrameTimelineSection(timeline: List<FrameMetric>) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(timeline) { index, frame ->
                val isSelected = index == selectedIndex
                val color = when {
                    !frame.poseDetected -> MutedText
                    frame.formScore >= 80 -> Success
                    frame.formScore >= 60 -> Warning
                    else -> Error
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) color.copy(alpha = 0.2f) else Elevated)
                        .border(1.dp, if (isSelected) color else SubtleBorder, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { selectedIndex = index } }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "%.1fs".format(frame.timestampSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) color else SecondaryText
                    )
                }
            }
        }

        if (selectedIndex in timeline.indices) {
            FrameDetailCard(frame = timeline[selectedIndex], index = selectedIndex, total = timeline.size)
        }
    }
}

@Composable
private fun FrameDetailCard(frame: FrameMetric, index: Int, total: Int) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frame ${index + 1} of $total (${"%.2f".format(frame.timestampSec)}s)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText
                )
                Text(
                    text = "Score: ${frame.formScore.roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (frame.formScore >= 80) Success else if (frame.formScore >= 60) Warning else Error
                )
            }
            if (frame.keyIssues.isNotEmpty()) {
                Text(
                    text = "Issues: ${frame.keyIssues.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Error
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video Overview Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VideoOverviewSection(result: AnalysisResult, metadata: VideoMetadata?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
            VideoMetadataGrid(metadata = metadata)
        }
    }
}

@Composable
private fun VideoMetadataGrid(metadata: VideoMetadata) {
    val items = buildList {
        if (metadata.durationMs > 0) add(Pair("Duration", formatDurationMs(metadata.durationMs)))
        if (metadata.width > 0 && metadata.height > 0) add(Pair("Resolution", "${metadata.width}×${metadata.height}"))
        if (metadata.originalFps > 0) add(Pair("Original FPS", "${metadata.originalFps.roundToInt()} fps"))
        if (metadata.totalFrames > 0) add(Pair("Total Frames", "${metadata.totalFrames}"))
        if (metadata.analyzedFrames > 0) add(Pair("Analyzed Frames", "${metadata.analyzedFrames}"))
        if (metadata.samplingFps > 0) add(Pair("Sampling FPS", "%.1f fps".format(metadata.samplingFps)))
    }

    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, value) ->
                    MetaInfoChip(label = label, value = value, modifier = Modifier.weight(1f))
                }
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
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PrimaryText)
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

@Composable
private fun StatusPill(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = SecondaryText,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = SubtleBorder,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatSeconds(sec: Double): String {
    val totalSeconds = sec.roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "%d:%02ds".format(minutes, seconds) else "${seconds}s"
}

private fun formatTimestamp(raw: String): String {
    return raw.substringBefore("T").ifEmpty { raw }
}
