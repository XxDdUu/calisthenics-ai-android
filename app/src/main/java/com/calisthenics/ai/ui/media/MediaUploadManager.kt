package com.calisthenics.ai.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: CalisthenicsApi
) {

    suspend fun uploadMedia(
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = upload(uri, null, null, onProgress).map { it.fileDownloadUri }

    suspend fun uploadProfileMedia(
        uri: Uri,
        profileType: ProfileType
    ): Result<ProfileMediaItem> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(uri) ?: ("media_" + UUID.randomUUID().toString())

            val tempFile = File(context.cacheDir, "upload_prof_${UUID.randomUUID()}_$fileName")
            var tempThumbnail: File? = null
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Could not open input stream for URI: $uri")

                if (mimeType.startsWith("video/")) {
                    val thumbBitmap = getVideoThumbnail(uri)
                    if (thumbBitmap != null) {
                        tempThumbnail = File(context.cacheDir, "thumb_${UUID.randomUUID()}.jpg")
                        FileOutputStream(tempThumbnail).use { out ->
                            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                    }
                }

                val fileBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)
                val thumbPart = tempThumbnail?.let {
                    val thumbBody = it.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("thumbnail", it.name, thumbBody)
                }

                val dto = api.uploadProfileMedia(profileType.name, filePart, thumbPart)
                dto.toDomain()
            } finally {
                if (tempFile.exists()) tempFile.delete()
                tempThumbnail?.let { if (it.exists()) it.delete() }
            }
        }
    }

    suspend fun uploadMediaForExercise(
        uri: Uri,
        exerciseId: Long,
        displayOrder: Int,
        onProgress: (Float) -> Unit = {}
    ): Result<ExerciseMedia> = upload(uri, exerciseId, displayOrder, onProgress).map { response ->
        val persisted = response.mediaItem
            ?: throw IllegalStateException("The server did not return persisted exercise media.")
        val mediaType = ExerciseMediaType.entries.firstOrNull {
            it.name.equals(persisted.type, ignoreCase = true)
        } ?: throw IllegalStateException("The server returned an invalid exercise media type.")
        ExerciseMedia(
            type = mediaType,
            url = MediaUrlUtils.resolveMediaUrl(persisted.url)?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("The server returned an empty exercise media URL."),
            thumbnailUrl = MediaUrlUtils.resolveMediaUrl(persisted.thumbnailUrl),
            displayOrder = persisted.displayOrder ?: displayOrder
        )
    }

    private suspend fun upload(
        uri: Uri,
        exerciseId: Long?,
        displayOrder: Int?,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(uri) ?: ("media_" + UUID.randomUUID().toString())

            // Copy content URI to temp cache file to safely stream via OkHttp
            val tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}_$fileName")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Could not open input stream for URI: $uri")

                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

                api.uploadExerciseMedia(part, exerciseId, displayOrder)
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }

    fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.path?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) it.substring(cut + 1) else it
            }
        }
        return name
    }

    fun getFileSize(uri: Uri): Long {
        var size: Long = 0
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    suspend fun getVideoThumbnail(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.frameAtTime
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
