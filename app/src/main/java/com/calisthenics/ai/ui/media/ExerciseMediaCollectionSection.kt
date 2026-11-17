package com.calisthenics.ai.ui.media

import com.calisthenics.ai.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType
import kotlinx.coroutines.launch

@Composable
fun ExerciseMediaCollectionSection(
    mediaItems: List<ExerciseMedia>,
    uploadManager: MediaUploadManager,
    exerciseId: Long? = null,
    onMediaItemsChange: (List<ExerciseMedia>) -> Unit,
    onUploadStateChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia((100 - mediaItems.size).coerceAtLeast(1))
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            coroutineScope.launch {
                isUploading = true
                onUploadStateChange(true)
                uploadError = null
                val updatedItems = mediaItems.toMutableList()
                val failures = mutableListOf<String>()
                try {
                    selectedUris.forEach { uri ->
                        val type = when {
                            context.contentResolver.getType(uri)?.startsWith("image/") == true ->
                                ExerciseMediaType.IMAGE
                            context.contentResolver.getType(uri)?.startsWith("video/") == true ->
                                ExerciseMediaType.VIDEO
                            else -> null
                        }
                        if (type == null) {
                            failures += uploadManager.getFileName(uri) ?: "Selected file"
                            return@forEach
                        }

                        val displayOrder = (updatedItems.maxOfOrNull { it.displayOrder } ?: -1) + 1
                        if (exerciseId != null) {
                            uploadManager.uploadMediaForExercise(uri, exerciseId, displayOrder)
                                .onSuccess { updatedItems += it }
                                .onFailure { error ->
                                    val name = uploadManager.getFileName(uri) ?: "Selected file"
                                    failures += "$name: ${error.localizedMessage ?: "Upload failed"}"
                                }
                        } else {
                            uploadManager.uploadMedia(uri)
                                .onSuccess { url ->
                                    updatedItems += ExerciseMedia(
                                        type = type,
                                        url = url,
                                        displayOrder = displayOrder
                                    )
                                }
                                .onFailure { error ->
                                    val name = uploadManager.getFileName(uri) ?: "Selected file"
                                    failures += "$name: ${error.localizedMessage ?: "Upload failed"}"
                                }
                        }
                    }
                    if (updatedItems.size != mediaItems.size) onMediaItemsChange(updatedItems)
                    if (failures.isNotEmpty()) {
                        uploadError = "Could not upload: ${failures.take(3).joinToString()}"
                    }
                } finally {
                    isUploading = false
                    onUploadStateChange(false)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.additional_exercise_media), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.loc_add_media_story_order),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    enabled = !isUploading && mediaItems.size < 100,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                ) {
                    Text(stringResource(R.string.add_media))
                }
            }

            if (isUploading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.uploading_media), style = MaterialTheme.typography.bodySmall)
            }
            uploadError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (mediaItems.size >= 100) {
                Text(
                    stringResource(R.string.loc_media_limit_reached),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (mediaItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    mediaItems.forEachIndexed { index, media ->
                        MediaCollectionItem(
                            media = media,
                            index = index,
                            onRemove = {
                                onMediaItemsChange(mediaItems.filterIndexed { itemIndex, _ -> itemIndex != index })
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCollectionItem(
    media: ExerciseMedia,
    index: Int,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailFailed by remember(media.url) { mutableStateOf(false) }
    Column(
        modifier = Modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (media.type == ExerciseMediaType.IMAGE && !thumbnailFailed) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(media.url).crossfade(true).build(),
                    contentDescription = stringResource(R.string.loc_exercise_image_index, index + 1),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { thumbnailFailed = true }
                )
            } else {
                Icon(
                    imageVector = if (media.type == ExerciseMediaType.IMAGE) Icons.Default.Image else Icons.Default.Movie,
                    contentDescription = media.type.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.loc_remove_media_index, index + 1),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = "${index + 1}. ${media.type.name.lowercase().replaceFirstChar(Char::uppercase)}",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
