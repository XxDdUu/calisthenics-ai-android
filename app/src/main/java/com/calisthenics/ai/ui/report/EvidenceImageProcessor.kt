package com.calisthenics.ai.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

/** Downscales a picked image to a JPEG in the cache dir so it can be uploaded as report evidence. */
object EvidenceImageProcessor {
    const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024
    private const val MAX_DIMENSION = 1600

    @Throws(IOException::class)
    fun prepare(context: Context, uri: Uri): File {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not read the selected image.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("The selected file is not a valid image.")
        var sample = 1
        while (bounds.outWidth / sample > MAX_DIMENSION * 2 || bounds.outHeight / sample > MAX_DIMENSION * 2) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("The selected image could not be decoded.")
        val scale = minOf(1f, MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
        } else decoded
        val output = File(context.cacheDir, "report-evidence-${UUID.randomUUID()}.jpg")
        try {
            output.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        } finally {
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
        if (output.length() > MAX_FILE_SIZE_BYTES) {
            output.delete()
            throw IOException("The image exceeds the 5 MB limit.")
        }
        return output
    }
}
