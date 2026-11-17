package com.calisthenics.ai.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calisthenics.ai.R
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileMediaType
import com.calisthenics.ai.ui.components.DarkCard
import com.calisthenics.ai.ui.media.ExoPlayerCacheManager
import com.calisthenics.ai.ui.media.MediaUrlUtils
import com.calisthenics.ai.ui.theme.Accent
import com.calisthenics.ai.ui.theme.Border
import com.calisthenics.ai.ui.theme.Elevated
import com.calisthenics.ai.ui.theme.Ink
import com.calisthenics.ai.ui.theme.PrimaryText
import com.calisthenics.ai.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Pre-Upload Model & Helper
// ─────────────────────────────────────────────────────────────────────────────

data class PreUploadMediaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val isVideo: Boolean,
    val fileName: String,
    val fileSizeFormatted: String,
    val durationFormatted: String? = null,
    val durationMs: Long? = null,
    val thumbnailBitmap: Bitmap? = null
)

suspend fun loadPreUploadMediaItem(context: Context, uri: Uri): PreUploadMediaItem = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: ""
    val isVideo = mimeType.startsWith("video/")

    var fileName = uri.lastPathSegment ?: "media"
    var fileSize = 0L
    try {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) cursor.getString(nameIndex)?.let { fileName = it }
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}

    val fileSizeFormatted = when {
        fileSize >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", fileSize / (1024.0 * 1024.0))
        fileSize >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", fileSize / 1024.0)
        fileSize > 0 -> "$fileSize B"
        else -> ""
    }

    var durationFormatted: String? = null
    var durationMs: Long? = null
    var thumbnailBitmap: Bitmap? = null

    if (isVideo) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (dur != null && dur > 0) {
                durationMs = dur
                val totalSecs = dur / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                durationFormatted = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
            }
            thumbnailBitmap = retriever.frameAtTime
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    PreUploadMediaItem(
        uri = uri,
        isVideo = isVideo,
        fileName = fileName,
        fileSizeFormatted = fileSizeFormatted,
        durationFormatted = durationFormatted,
        durationMs = durationMs,
        thumbnailBitmap = thumbnailBitmap
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Media Gallery Main Component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileMediaGallery(
    mediaList: List<ProfileMediaItem>,
    isOwner: Boolean,
    isLoading: Boolean = false,
    isUploading: Boolean = false,
    onUploadMedia: ((List<Uri>) -> Unit)? = null,
    onDeleteMedia: ((Long) -> Unit)? = null,
    onReorderMedia: ((List<Long>) -> Unit)? = null,
    onSetCover: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var viewerInitialIndex by remember { mutableStateOf<Int?>(null) }
    var showManageDialog by remember { mutableStateOf(false) }

    if (mediaList.isEmpty() && !isOwner) {
        return
    }

    DarkCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_media_gallery),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryText
                    )
                    if (mediaList.isNotEmpty()) {
                        Text(
                            text = "(${mediaList.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }

                if (isOwner) {
                    TextButton(
                        onClick = { showManageDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.profile_media_edit),
                            color = Accent,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Preview Layout
            if (mediaList.isEmpty()) {
                // Owner empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Elevated.copy(alpha = 0.5f))
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .clickable { showManageDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.profile_media_add_photos_videos),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryText
                        )
                        Text(
                            text = stringResource(R.string.profile_media_empty_owner),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val previewHeight = 180.dp
                when (mediaList.size) {
                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(previewHeight)
                        ) {
                            ProfileMediaPreviewItem(
                                item = mediaList[0],
                                modifier = Modifier.fillMaxSize(),
                                onClick = { viewerInitialIndex = 0 }
                            )
                        }
                    }
                    2 -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(previewHeight),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileMediaPreviewItem(
                                item = mediaList[0],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { viewerInitialIndex = 0 }
                            )
                            ProfileMediaPreviewItem(
                                item = mediaList[1],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { viewerInitialIndex = 1 }
                            )
                        }
                    }
                    else -> {
                        // 3 or more items: 50% (weight 2f), 25% (weight 1f), 25% (weight 1f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(previewHeight),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProfileMediaPreviewItem(
                                item = mediaList[0],
                                modifier = Modifier
                                    .weight(2f)
                                    .fillMaxHeight(),
                                onClick = { viewerInitialIndex = 0 }
                            )
                            ProfileMediaPreviewItem(
                                item = mediaList[1],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { viewerInitialIndex = 1 }
                            )
                            ProfileMediaPreviewItem(
                                item = mediaList[2],
                                moreCount = if (mediaList.size > 3) mediaList.size - 3 else 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { viewerInitialIndex = 2 }
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen Viewer Dialog
    viewerInitialIndex?.let { startIndex ->
        ProfileMediaViewerDialog(
            mediaList = mediaList,
            initialIndex = startIndex,
            onDismiss = { viewerInitialIndex = null }
        )
    }

    // Media Management Dialog for Owner
    if (showManageDialog) {
        ProfileMediaManageDialog(
            mediaList = mediaList,
            isUploading = isUploading,
            onDismiss = { showManageDialog = false },
            onUpload = onUploadMedia,
            onDelete = onDeleteMedia,
            onReorder = onReorderMedia,
            onSetCover = onSetCover
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Media Preview Thumbnail Item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileMediaPreviewItem(
    item: ProfileMediaItem,
    modifier: Modifier = Modifier,
    moreCount: Int = 0,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val previewUrl = item.thumbnailUrl ?: item.url

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Elevated)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(previewUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video Play Icon Overlay
        if (item.mediaType == ProfileMediaType.VIDEO && moreCount <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // More Media Overlay (+X)
        if (moreCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$moreCount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Full-Screen Media Viewer Dialog (Zoomable Image + Robust Video Player)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileMediaViewerDialog(
    mediaList: List<ProfileMediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    LaunchedEffect(mediaList.isEmpty()) {
        if (mediaList.isEmpty()) {
            onDismiss()
        }
    }

    if (mediaList.isEmpty()) {
        return
    }

    val safeInitialPage = remember(initialIndex, mediaList.size) {
        val maxIndex = (mediaList.size - 1).coerceAtLeast(0)
        initialIndex.coerceIn(0, maxIndex)
    }

    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { mediaList.size }
    )

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val mediaItem = mediaList[page]
                val isCurrentPage = pagerState.currentPage == page

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (mediaItem.mediaType == ProfileMediaType.VIDEO) {
                        ProfileVideoPlayerItem(
                            videoUrl = mediaItem.url,
                            thumbnailUrl = mediaItem.thumbnailUrl,
                            isActive = isCurrentPage
                        )
                    } else {
                        ZoomableImage(
                            imageUrl = mediaItem.url,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Top overlay bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Zoomable Image with Pinch-to-Zoom & Pan
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ZoomableImage(
    imageUrl: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    if (newScale > 1f) {
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                    scale = newScale
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile Video Player with Cache, Controls & Error Recovery
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileVideoPlayerItem(
    videoUrl: String,
    thumbnailUrl: String?,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val resolvedUrl = remember(videoUrl) {
        MediaUrlUtils.resolveMediaUrl(videoUrl) ?: videoUrl
    }

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(resolvedUrl) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(ExoPlayerCacheManager.createMediaSourceFactory(context))
            .setLoadControl(ExoPlayerCacheManager.createFastStartupLoadControl())
            .build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(resolvedUrl)))
                repeatMode = Player.REPEAT_MODE_OFF
                prepare()
            }
    }

    // Lifecycle handling: pause on background, resume if was playing
    DisposableEffect(exoPlayer, lifecycleOwner) {
        var resumeOnStart = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeOnStart = exoPlayer.playWhenReady
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_START -> {
                    if (resumeOnStart && isActive) {
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Pager active effect
    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Player events listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        areControlsVisible = true
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
                isBuffering = false
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                videoError = error.localizedMessage ?: "Failed to play video"
                exoPlayer.pause()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Position tick loop
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (!isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            }
            delay(200)
        }
    }

    // Auto-hide controls after 3.5s of playback
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && videoError == null) {
            delay(3500)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            },
        contentAlignment = Alignment.Center
    ) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        // Poster / Thumbnail before first frame is rendered
        if (!isFirstFrameRendered && videoError == null) {
            val previewPoster = thumbnailUrl ?: resolvedUrl
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(previewPoster)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering Indicator
        if (isBuffering && videoError == null) {
            CircularProgressIndicator(
                color = Accent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }

        // Error Banner with Retry
        videoError?.let { err ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = stringResource(R.string.profile_media_video_error),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Button(
                    onClick = {
                        videoError = null
                        isBuffering = true
                        isFirstFrameRendered = false
                        exoPlayer.seekTo(0)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.common_retry))
                }
            }
        }

        // Custom Interactive Controls Overlay
        AnimatedVisibility(
            visible = areControlsVisible && videoError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Center Play / Pause Button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Bottom Controls Bar (Slider + Time)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val sliderVal = if (isDraggingSlider) {
                        dragProgress
                    } else if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    Slider(
                        value = sliderVal,
                        onValueChange = { newVal ->
                            isDraggingSlider = true
                            dragProgress = newVal
                        },
                        onValueChangeFinished = {
                            val targetMs = (dragProgress * durationMs).toLong()
                            exoPlayer.seekTo(targetMs)
                            isDraggingSlider = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Accent,
                            activeTrackColor = Accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayPosMs = if (isDraggingSlider) (dragProgress * durationMs).toLong() else currentPositionMs
                        Text(
                            text = formatDuration(displayPosMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Text(
                            text = formatDuration(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Owner Management Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileMediaManageDialog(
    mediaList: List<ProfileMediaItem>,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onUpload: ((List<Uri>) -> Unit)?,
    onDelete: ((Long) -> Unit)?,
    onReorder: ((List<Long>) -> Unit)?,
    onSetCover: ((Long) -> Unit)?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mediaToDelete by remember { mutableStateOf<ProfileMediaItem?>(null) }
    var stagedItems by remember { mutableStateOf<List<PreUploadMediaItem>>(emptyList()) }
    var isAnalyzingMedia by remember { mutableStateOf(false) }
    var showPreUploadDialog by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isAnalyzingMedia = true
            coroutineScope.launch {
                val items = uris.map { uri ->
                    loadPreUploadMediaItem(context, uri)
                }
                stagedItems = items
                isAnalyzingMedia = false
                showPreUploadDialog = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            color = Ink,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_media_manage_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = stringResource(R.string.profile_media_manage_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SecondaryText)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action to Pick Media
                Button(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    enabled = !isAnalyzingMedia && !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzingMedia) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Reading selected files...")
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.profile_media_add_photos_videos),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (isUploading) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Accent, strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.profile_media_uploading),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // List of Existing Items
                Text(
                    text = "Current Media (${mediaList.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = SecondaryText,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                if (mediaList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.profile_media_empty_visitor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = mediaList,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            val isCover = index == 0
                            DarkCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Elevated)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(item.thumbnailUrl ?: item.url)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (item.mediaType == ProfileMediaType.VIDEO) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Info / Cover Chip
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (item.mediaType == ProfileMediaType.VIDEO) "Video" else "Image",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = PrimaryText
                                            )
                                            if (isCover) {
                                                Surface(
                                                    color = Accent.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Accent)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.profile_media_cover_badge),
                                                        color = Accent,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (!isCover) {
                                            TextButton(
                                                onClick = { onSetCover?.invoke(item.id) },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Accent, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = stringResource(R.string.profile_media_set_cover),
                                                    color = Accent,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }

                                    // Reordering Arrows
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutableIds = mediaList.map { it.id }.toMutableList()
                                                val temp = mutableIds[index]
                                                mutableIds[index] = mutableIds[index - 1]
                                                mutableIds[index - 1] = temp
                                                onReorder?.invoke(mutableIds)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) PrimaryText else SecondaryText.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < mediaList.size - 1) {
                                                val mutableIds = mediaList.map { it.id }.toMutableList()
                                                val temp = mutableIds[index]
                                                mutableIds[index] = mutableIds[index + 1]
                                                mutableIds[index + 1] = temp
                                                onReorder?.invoke(mutableIds)
                                            }
                                        },
                                        enabled = index < mediaList.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = if (index < mediaList.size - 1) PrimaryText else SecondaryText.copy(alpha = 0.3f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { mediaToDelete = item },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pre-Upload Preview Dialog (allows inspecting, reordering, removing, full preview before upload)
    if (showPreUploadDialog && stagedItems.isNotEmpty()) {
        ProfileMediaPreUploadDialog(
            stagedItems = stagedItems,
            isUploading = isUploading,
            onDismiss = {
                showPreUploadDialog = false
                stagedItems = emptyList()
            },
            onUpload = { orderedUris ->
                showPreUploadDialog = false
                stagedItems = emptyList()
                onUpload?.invoke(orderedUris)
            }
        )
    }

    // Delete confirmation dialog
    mediaToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text(stringResource(R.string.profile_media_delete_confirm), color = PrimaryText) },
            text = { Text("Are you sure you want to delete this media item?", color = SecondaryText) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete?.invoke(item.id)
                        mediaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.profile_media_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { mediaToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = SecondaryText)
                }
            },
            containerColor = Elevated
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pre-Upload Media Preview Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileMediaPreUploadDialog(
    stagedItems: List<PreUploadMediaItem>,
    isUploading: Boolean,
    onDismiss: () -> Unit,
    onUpload: (List<Uri>) -> Unit
) {
    val items = remember { mutableStateListOf<PreUploadMediaItem>().apply { addAll(stagedItems) } }
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewVideoUri by remember { mutableStateOf<Uri?>(null) }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            color = Ink,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_media_preview_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = stringResource(R.string.profile_media_selected_count, items.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = Accent
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SecondaryText)
                    }
                }

                Text(
                    text = stringResource(R.string.profile_media_preview_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // List of staged items
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No files selected",
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { _, itm -> itm.id }
                        ) { index, item ->
                            DarkCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (item.isVideo) {
                                            previewVideoUri = item.uri
                                        } else {
                                            previewImageUri = item.uri
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Thumbnail Box
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Elevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.isVideo) {
                                            if (item.thumbnailBitmap != null) {
                                                Image(
                                                    bitmap = item.thumbnailBitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0xFF1E293B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }

                                            // Play Overlay
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.7f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = "Preview",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // Duration Badge
                                            item.durationFormatted?.let { dur ->
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.75f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(3.dp)
                                                ) {
                                                    Text(
                                                        text = dur,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(item.uri)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    // Details Column
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = buildString {
                                                append(if (item.isVideo) "Video" else "Image")
                                                if (item.fileSizeFormatted.isNotBlank()) {
                                                    append(" • ")
                                                    append(item.fileSizeFormatted)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SecondaryText
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = Accent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.profile_media_tap_to_preview),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Accent
                                            )
                                        }
                                    }

                                    // Action Buttons (Reorder & Delete)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val temp = items[index]
                                                    items[index] = items[index - 1]
                                                    items[index - 1] = temp
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) PrimaryText else SecondaryText.copy(alpha = 0.3f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < items.size - 1) {
                                                    val temp = items[index]
                                                    items[index] = items[index + 1]
                                                    items[index + 1] = temp
                                                }
                                            },
                                            enabled = index < items.size - 1,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = "Move Down",
                                                tint = if (index < items.size - 1) PrimaryText else SecondaryText.copy(alpha = 0.3f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                items.removeAt(index)
                                                if (items.isEmpty()) {
                                                    onDismiss()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Button(
                        onClick = {
                            if (items.isNotEmpty()) {
                                onUpload(items.map { it.uri })
                            }
                        },
                        enabled = items.isNotEmpty() && !isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Ink, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.profile_media_uploading))
                        } else {
                            Text(stringResource(R.string.profile_media_upload_count, items.size))
                        }
                    }
                }
            }
        }
    }

    // Full screen image preview dialog
    previewImageUri?.let { uri ->
        LocalImagePreviewDialog(
            uri = uri,
            onDismiss = { previewImageUri = null }
        )
    }

    // Full screen video preview dialog
    previewVideoUri?.let { uri ->
        LocalVideoPreviewDialog(
            uri = uri,
            onDismiss = { previewVideoUri = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Local Pre-Upload Image Preview Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LocalImagePreviewDialog(
    uri: Uri,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ZoomableImage(
                imageUrl = uri.toString(),
                modifier = Modifier.fillMaxSize()
            )

            // Close button overlay
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Local Pre-Upload Video Preview Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LocalVideoPreviewDialog(
    uri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            play()
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                Lifecycle.Event.ON_START -> exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        areControlsVisible = true
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                videoError = error.localizedMessage ?: "Failed to play local video"
                exoPlayer.pause()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (!isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
            }
            delay(200)
        }
    }

    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying && videoError == null) {
            delay(3500)
            areControlsVisible = false
        }
    }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    areControlsVisible = !areControlsVisible
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                update = { it.player = exoPlayer },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering && videoError == null) {
                CircularProgressIndicator(
                    color = Accent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }

            videoError?.let { err ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Cannot preview video: $err", color = PrimaryText, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Controls Overlay
            AnimatedVisibility(
                visible = areControlsVisible && videoError == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .align(Alignment.TopStart)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    // Center Play / Pause
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0)
                                }
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Bottom Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val sliderVal = if (isDraggingSlider) {
                            dragProgress
                        } else if (durationMs > 0) {
                            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        Slider(
                            value = sliderVal,
                            onValueChange = { newVal ->
                                isDraggingSlider = true
                                dragProgress = newVal
                            },
                            onValueChangeFinished = {
                                val targetMs = (dragProgress * durationMs).toLong()
                                exoPlayer.seekTo(targetMs)
                                isDraggingSlider = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val displayPosMs = if (isDraggingSlider) (dragProgress * durationMs).toLong() else currentPositionMs
                            Text(
                                text = formatDuration(displayPosMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Text(
                                text = formatDuration(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
