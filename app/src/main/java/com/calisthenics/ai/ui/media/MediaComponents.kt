package com.calisthenics.ai.ui.media

import com.calisthenics.ai.R

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImagePickerLauncher(
    onImageSelected: (Uri) -> Unit
): () -> Unit {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }
    return {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
fun VideoPickerLauncher(
    onVideoSelected: (Uri) -> Unit
): () -> Unit {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onVideoSelected(it) }
    }
    return {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }
}

@Composable
fun ImageSelectorSection(
    currentImageUrl: String?,
    selectedUri: Uri?,
    uploadState: MediaUploadState,
    onSelectImage: (Uri) -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val launchPicker = ImagePickerLauncher(onImageSelected = onSelectImage)
    val hasImage = !currentImageUrl.isNullOrBlank() || selectedUri != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.loc_illustration_local),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = launchPicker,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (hasImage) stringResource(R.string.loc_change_photo) else stringResource(R.string.loc_choose_photo), fontSize = 12.sp)
                }
                if (hasImage) {
                    TextButton(
                        onClick = onRemoveImage,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.loc_delete_photo), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }

        if (uploadState is MediaUploadState.Uploading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = uploadState.stage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uploadState is MediaUploadState.Error) {
            Text(
                text = uploadState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (hasImage) {
            val previewModel = selectedUri ?: currentImageUrl
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = stringResource(R.string.image_preview),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun VideoSelectorSection(
    currentVideoUrl: String?,
    selectedUri: Uri?,
    uploadState: MediaUploadState,
    uploadManager: MediaUploadManager,
    onSelectVideo: (Uri) -> Unit,
    onRemoveVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val launchPicker = VideoPickerLauncher(onVideoSelected = onSelectVideo)
    val hasVideo = !currentVideoUrl.isNullOrBlank() || selectedUri != null

    var videoFileName by remember(selectedUri, currentVideoUrl) {
        mutableStateOf<String?>(null)
    }
    var videoFileSize by remember(selectedUri) {
        mutableStateOf<String?>(null)
    }
    var thumbnailBitmap by remember(selectedUri) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(selectedUri, currentVideoUrl) {
        if (selectedUri != null) {
            videoFileName = uploadManager.getFileName(selectedUri) ?: "video.mp4"
            val size = uploadManager.getFileSize(selectedUri)
            videoFileSize = uploadManager.formatFileSize(size)
            thumbnailBitmap = uploadManager.getVideoThumbnail(selectedUri)
        } else if (!currentVideoUrl.isNullOrBlank()) {
            val cut = currentVideoUrl.lastIndexOf('/')
            videoFileName = if (cut != -1) currentVideoUrl.substring(cut + 1) else currentVideoUrl
            videoFileSize = null
            thumbnailBitmap = null
        } else {
            videoFileName = null
            videoFileSize = null
            thumbnailBitmap = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.loc_video_instructional),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = launchPicker,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (hasVideo) stringResource(R.string.loc_change_video) else stringResource(R.string.loc_choose_video), fontSize = 12.sp)
                }
                if (hasVideo) {
                    TextButton(
                        onClick = onRemoveVideo,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.loc_delete_video), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }

        if (uploadState is MediaUploadState.Uploading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = uploadState.stage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uploadState is MediaUploadState.Error) {
            Text(
                text = uploadState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (hasVideo) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap!!.asImageBitmap(),
                                contentDescription = stringResource(R.string.video_thumbnail),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = videoFileName ?: stringResource(R.string.loc_video_file),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        if (videoFileSize != null) {
                            Text(
                                text = stringResource(R.string.loc_file_size, videoFileSize.toString()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
