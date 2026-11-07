package com.calisthenics.ai.ui.media

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType
import kotlinx.coroutines.delay

private val StoryGradient = listOf(
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFFF97316)
)
private const val IMAGE_STORY_DURATION_MS = 5_000L
private const val MEDIA_GALLERY_TAG = "ExerciseMediaGallery"

private fun ExerciseMedia.urlHost(): String =
    Uri.parse(url).host ?: "unknown"

@Composable
fun ExerciseMediaBorder(
    hasMedia: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val brush = remember { Brush.sweepGradient(StoryGradient) }
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            if (hasMedia) {
                val borderPx = borderWidth.toPx()
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(borderPx / 2f, borderPx / 2f),
                    size = Size(size.width - borderPx, size.height - borderPx),
                    cornerRadius = CornerRadius((cornerRadius.toPx() - borderPx / 2f).coerceAtLeast(0f)),
                    style = Stroke(width = borderWidth.toPx())
                )
            }
        },
        content = content
    )
}

@Composable
fun ExerciseMediaPreview(
    mediaItems: List<ExerciseMedia>,
    modifier: Modifier = Modifier,
    onMediaClick: (Int) -> Unit
) {
    if (mediaItems.isEmpty()) return

    val context = LocalContext.current
    LaunchedEffect(mediaItems) {
        val videos = mediaItems.filter { it.type == ExerciseMediaType.VIDEO }
        videos.forEach { video ->
            ExoPlayerCacheManager.preloadVideo(context, video.url, 1024L * 1024L)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        mediaItems.forEachIndexed { index, media ->
            MediaThumbnail(
                media = media,
                modifier = Modifier.size(66.dp),
                onClick = { onMediaClick(index) }
            )
        }
    }
}

@Composable
private fun MediaThumbnail(
    media: ExerciseMedia,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = media.thumbnailUrl ?: media.url.takeIf { media.type == ExerciseMediaType.IMAGE }
    var failed by remember(media.url) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, Brush.sweepGradient(StoryGradient), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null && !failed) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                contentDescription = "Open exercise media",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                onError = { failed = true }
            )
        } else if (media.type == ExerciseMediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (media.type == ExerciseMediaType.VIDEO && (imageUrl == null || !failed)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun ExerciseStoryViewer(
    mediaItems: List<ExerciseMedia>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (mediaItems.isEmpty()) return

    var currentIndex by rememberSaveable(mediaItems.map { it.url }) {
        mutableIntStateOf(initialIndex.coerceIn(mediaItems.indices))
    }
    var progress by remember(currentIndex) { mutableFloatStateOf(0f) }
    var imageLoadFailed by remember(currentIndex) { mutableStateOf(false) }
    var reloadKey by remember(currentIndex) { mutableIntStateOf(0) }
    var videoError by remember(currentIndex) { mutableStateOf<String?>(null) }
    var isFirstFrameRendered by remember(currentIndex) { mutableStateOf(false) }
    var isBuffering by remember(currentIndex) { mutableStateOf(true) }

    val media = mediaItems.getOrNull(currentIndex) ?: return
    val currentDismiss = rememberUpdatedState(onDismiss)
    val goNext = rememberUpdatedState {
        if (currentIndex < mediaItems.lastIndex) currentIndex++ else currentDismiss.value()
    }
    val goPrevious = {
        if (currentIndex > 0) currentIndex--
    }

    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(media.url) {
        if (media.type == ExerciseMediaType.VIDEO) {
            Log.d(
                MEDIA_GALLERY_TAG,
                "initializing video player host=${media.urlHost()}"
            )
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(ExoPlayerCacheManager.createMediaSourceFactory(context))
                .setLoadControl(ExoPlayerCacheManager.createFastStartupLoadControl())
                .build().apply {
                    setMediaItem(MediaItem.fromUri(media.url))
                    prepare()
                    playWhenReady = true
                }
        } else {
            null
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        if (player == null) {
            onDispose { }
        } else {
            var resumeAfterLifecyclePause = false
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        resumeAfterLifecyclePause = player.playWhenReady
                        player.pause()
                    }
                    Lifecycle.Event.ON_START -> {
                        if (resumeAfterLifecyclePause) player.play()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                player.release()
            }
        }
    }

    val listener = remember(player) {
        player?.let {
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            isBuffering = true
                        }
                        Player.STATE_READY -> {
                            isBuffering = false
                        }
                        Player.STATE_ENDED -> {
                            isBuffering = false
                            goNext.value()
                        }
                        Player.STATE_IDLE -> {
                            isBuffering = false
                        }
                    }
                }

                override fun onRenderedFirstFrame() {
                    isFirstFrameRendered = true
                    isBuffering = false
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(
                        MEDIA_GALLERY_TAG,
                        "video playback failed host=${media.urlHost()}, error=${error.errorCodeName}",
                        error
                    )
                    isBuffering = false
                    videoError = error.localizedMessage ?: "Unable to play this video."
                    player.pause()
                }
            }
        }
    }
    DisposableEffect(player, listener) {
        if (player != null && listener != null) player.addListener(listener)
        onDispose {
            if (player != null && listener != null) player.removeListener(listener)
        }
    }

    LaunchedEffect(media.url, player, lifecycleOwner, currentIndex) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (media.type == ExerciseMediaType.IMAGE) {
                val startedAt = SystemClock.elapsedRealtime()
                while (true) {
                    val elapsed = SystemClock.elapsedRealtime() - startedAt
                    progress = (elapsed.toFloat() / IMAGE_STORY_DURATION_MS).coerceIn(0f, 1f)
                    if (elapsed >= IMAGE_STORY_DURATION_MS) {
                        goNext.value()
                        break
                    }
                    delay(50)
                }
            } else {
                while (true) {
                    val duration = player?.duration ?: 0L
                    val position = player?.currentPosition ?: 0L
                    progress = if (duration > 0L) {
                        (position.toFloat() / duration).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    delay(100)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var dragDistance by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(currentIndex) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, amount -> dragDistance += amount },
                        onDragEnd = {
                            if (dragDistance > 100.dp.toPx()) onDismiss()
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f }
                    )
                }
        ) {
            if (media.type == ExerciseMediaType.VIDEO) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (player != null) {
                        AndroidView(
                            factory = { viewContext ->
                                PlayerView(viewContext).apply {
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                    this.player = player
                                }
                            },
                            update = { it.player = player },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Render thumbnail immediately while buffering or before the first frame is rendered
                    if (!isFirstFrameRendered && videoError == null) {
                        val thumbUrl = media.thumbnailUrl
                        if (!thumbUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumbUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Video preview thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Subtle loading spinner while buffering
                    if (isBuffering && videoError == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.Center),
                            color = Color.White.copy(alpha = 0.85f),
                            strokeWidth = 3.dp
                        )
                    }
                }
            } else if (!imageLoadFailed) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.url)
                        .memoryCacheKey("${media.url}:$reloadKey")
                        .diskCacheKey(media.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Exercise story ${currentIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onError = { imageLoadFailed = true }
                )
            } else {
                UnavailableMedia(
                    onRetry = {
                        imageLoadFailed = false
                        reloadKey++
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Row(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = goPrevious)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = { goNext.value() })
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                StoryProgressIndicator(
                    count = mediaItems.size,
                    currentIndex = currentIndex,
                    currentProgress = progress
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${mediaItems.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close story", tint = Color.White)
                    }
                }
            }

            videoError?.let { message ->
                UnavailableMedia(
                    message = message,
                    onRetry = {
                        videoError = null
                        isBuffering = true
                        isFirstFrameRendered = false
                        player?.seekTo(0)
                        player?.prepare()
                        player?.play()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun StoryProgressIndicator(
    count: Int,
    currentIndex: Int,
    currentProgress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(count) { index ->
            val segmentProgress = when {
                index < currentIndex -> 1f
                index == currentIndex -> currentProgress
                else -> 0f
            }.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(segmentProgress)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun UnavailableMedia(
    message: String = "Media could not be loaded.",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.White)
        Text(message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Retry", color = Color.White)
        }
    }
}
