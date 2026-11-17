package com.calisthenics.ai.ui.video

import android.net.Uri
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.QuotaEntitlement
import com.calisthenics.ai.ui.theme.LocalAppColors

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSelectionScreen(
    onAnalysisSuccess: (Long) -> Unit,
    onBackClick: () -> Unit,
    viewModel: VideoSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasResumedOnce by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasResumedOnce) {
                    viewModel.loadQuota()
                } else {
                    hasResumedOnce = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onMediaSelected(context, it, "VIDEO") }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onMediaSelected(context, it, "IMAGE") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.choose_media)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Quota Status Pill
            QuotaStatusPill(
                quota = state.quota,
                isLoading = state.isLoadingQuota,
                isUpdatingMembership = state.isUpdatingMembership,
                onUpgradeToPro = { viewModel.upgradeToPro() }
            )

            // 2. Selected Exercise & Camera Angle Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.selected_skill),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.progressionName.ifBlank { state.exerciseCode },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (state.exerciseCode.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = state.exerciseCode,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Side-View Recording",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (state.sideView) {
                                    "Camera positioned to your side (recommended for depth & alignment)"
                                } else {
                                    "Front or 45° diagonal camera angle"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.sideView,
                            onCheckedChange = { if (!state.isUploading) viewModel.setSideView(it) },
                            enabled = !state.isUploading
                        )
                    }
                }
            }

            // 3. Exercise-Specific Recording Guidance Card
            RecordingGuidanceCard(guidance = state.recordingGuidance)

            // 4. Inline Video / Media Preview or Empty Picker Box
            if (state.selectedVideoUri == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clickable(enabled = !state.isUploading) { videoPicker.launch("video/*") },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.no_media_selected),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.choose_media_for_analysis),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.mediaType == "VIDEO") {
                VideoUriPreviewPlayerCard(
                    uri = state.selectedVideoUri!!,
                    fileName = state.videoFileName ?: stringResource(R.string.loc_selected_media),
                    durationFormatted = state.videoDurationFormatted,
                    fileSizeFormatted = state.videoFileSizeFormatted
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = state.videoFileName ?: stringResource(R.string.loc_selected_media),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    R.string.loc_media_type_size,
                                    state.mediaType.lowercase().replaceFirstChar { it.uppercase() },
                                    state.videoFileSizeFormatted.orEmpty()
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Media Picker Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { videoPicker.launch("video/*") },
                    enabled = !state.isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VideoLibrary, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.selectedVideoUri != null && state.mediaType == "VIDEO") "Change Video" else stringResource(R.string.video))
                }
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    enabled = !state.isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.image))
                }
            }

            // 6. Multi-Stage Upload & Processing Progress Card
            if (state.isUploading) {
                UploadProgressCard(
                    activeStep = state.uploadStep,
                    stageText = state.uploadStage
                )
            }

            // 7. Error Handling Cards (HTTP 429, HTTP 403, Network / Retry)
            if (state.error != null && !state.isUploading) {
                when {
                    state.isQuotaExhaustedError -> {
                        QuotaExhaustedErrorCard(
                            message = state.error!!,
                            resetHint = state.quotaResetHint ?: "Resets at 00:00 (Asia/Ho_Chi_Minh)",
                            isUpdatingMembership = state.isUpdatingMembership,
                            membershipError = state.membershipError,
                            onUpgradeToPro = { viewModel.upgradeToPro() }
                        )
                    }
                    state.isForbiddenError -> {
                        ForbiddenErrorCard(
                            message = state.error!!,
                            isUpdatingMembership = state.isUpdatingMembership,
                            onUpgradeToPro = { viewModel.upgradeToPro() },
                            onBackClick = onBackClick
                        )
                    }
                    else -> {
                        NetworkRetryErrorCard(
                            message = state.error!!,
                            onRetry = { viewModel.analyzeVideo(context, onAnalysisSuccess) }
                        )
                    }
                }
            }

            // 8. Analyze Form Primary Action Button
            if (state.selectedVideoUri != null && !state.isUploading) {
                Button(
                    onClick = { viewModel.analyzeVideo(context, onAnalysisSuccess) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.analyze_form), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuotaStatusPill(
    quota: QuotaEntitlement?,
    isLoading: Boolean,
    isUpdatingMembership: Boolean,
    onUpgradeToPro: () -> Unit
) {
    val appColors = LocalAppColors.current
    val isAdmin = quota?.isAdmin == true
    val isExhausted = quota?.isExhausted == true
    val isPro = quota?.isPro == true
    val pillColor = when {
        isAdmin -> appColors.accent
        isExhausted -> appColors.error
        isPro -> appColors.accent
        else -> appColors.warning
    }

    val quotaText = when {
        isAdmin || quota?.isUnlimited == true -> "Quota: Unlimited uploads (${quota?.usedToday ?: 0} used today)"
        quota == null && isLoading -> "Quota: Checking daily limit..."
        quota == null -> "Quota: Daily upload allowance"
        quota.isExhausted -> "Quota: ${quota.remainingToday ?: 0}/${quota.dailyLimit ?: 1} remaining — Daily limit reached"
        else -> "Quota: ${quota.remainingToday}/${quota.dailyLimit} remaining today"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = pillColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, pillColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when {
                        isAdmin -> Icons.Default.AdminPanelSettings
                        isExhausted -> Icons.Default.Warning
                        else -> Icons.Default.WorkspacePremium
                    },
                    contentDescription = null,
                    tint = pillColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = quotaText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isExhausted) appColors.error else MaterialTheme.colorScheme.onSurface
                    )
                    quota?.let {
                        val subtitle = if (isAdmin || it.isUnlimited) {
                            "${if (isAdmin) "ADMIN" else it.membershipTier} tier • Unlimited daily quota • Resets 00:00 (${it.timezone})"
                        } else {
                            "${it.membershipTier} tier • Resets 00:00 (${it.timezone})"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isExhausted && !isPro && !isAdmin) {
                Button(
                    onClick = onUpgradeToPro,
                    enabled = !isUpdatingMembership,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUpdatingMembership) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Upgrade", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingGuidanceCard(guidance: RecordingGuidance) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, appColors.accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = appColors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = guidance.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            GuidanceBullet(label = "Camera Angle", value = guidance.cameraAngle)
            GuidanceBullet(label = "Distance", value = guidance.distance)
            GuidanceBullet(label = "Full-Body Framing", value = guidance.framing)
            GuidanceBullet(label = "Lighting", value = guidance.lighting)
        }
    }
}

@Composable
private fun GuidanceBullet(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoUriPreviewPlayerCard(
    uri: Uri,
    fileName: String,
    durationFormatted: String?,
    fileSizeFormatted: String?
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = false
        isPlaying = false
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .clickable {
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.5.dp, appColors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            tint = appColors.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val metaLine = listOfNotNull(
                            durationFormatted?.let { "Duration: $it" },
                            fileSizeFormatted?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")
                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadProgressCard(
    activeStep: UploadProgressStep,
    stageText: String
) {
    val appColors = LocalAppColors.current
    val orderedSteps = listOf(
        UploadProgressStep.UPLOADING,
        UploadProgressStep.POSE_DETECTION,
        UploadProgressStep.BIOMECHANICAL_ANALYSIS,
        UploadProgressStep.COMPLETED
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stageText.ifBlank { activeStep.label },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { activeStep.progress.coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                orderedSteps.forEach { step ->
                    val isDone = activeStep.stepIndex > step.stepIndex ||
                        (activeStep == UploadProgressStep.COMPLETED && step == UploadProgressStep.COMPLETED)
                    val isCurrent = activeStep == step && activeStep != UploadProgressStep.COMPLETED
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when {
                            isDone -> Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = appColors.success,
                                modifier = Modifier.size(18.dp)
                            )
                            isCurrent -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            else -> Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = step.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isCurrent || isDone) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCurrent || isDone) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaExhaustedErrorCard(
    message: String,
    resetHint: String,
    isUpdatingMembership: Boolean,
    membershipError: String?,
    onUpgradeToPro: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.error.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, appColors.error.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = appColors.error
                )
                Text(
                    text = "Daily Quota Exhausted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.error
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = resetHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!membershipError.isNullOrBlank()) {
                Text(
                    text = membershipError,
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.error
                )
            }
            Button(
                onClick = onUpgradeToPro,
                enabled = !isUpdatingMembership,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUpdatingMembership) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.WorkspacePremium, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Upgrade to PRO (10/day)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ForbiddenErrorCard(
    message: String,
    isUpdatingMembership: Boolean,
    onUpgradeToPro: () -> Unit,
    onBackClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.warning.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, appColors.warning.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = appColors.warning)
                Text(
                    text = "Access Forbidden",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.warning
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_back))
                }
                Button(
                    onClick = onUpgradeToPro,
                    enabled = !isUpdatingMembership,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Upgrade to PRO")
                }
            }
        }
    }
}

@Composable
private fun NetworkRetryErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.error.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, appColors.error.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, null, tint = appColors.error)
                Text(
                    text = stringResource(R.string.analysis_failed),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = appColors.error
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.common_retry), fontWeight = FontWeight.Bold)
            }
        }
    }
}
