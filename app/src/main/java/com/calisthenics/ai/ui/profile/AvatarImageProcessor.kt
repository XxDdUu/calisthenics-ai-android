package com.calisthenics.ai.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max

object AvatarImageProcessor {
    const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024
    private const val MAX_DIMENSION = 512
    private const val MAX_PIXEL_COUNT = 40_000_000L

    fun prepare(context: Context, uri: Uri): java.io.File {
        val source = context.contentResolver.openInputStream(uri)?.use(::readBounded)
            ?: throw IOException("Could not read the selected image.")
        if (!isSupportedImage(source)) {
            throw IOException("Choose a JPEG, PNG, or WebP image.")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > MAX_PIXEL_COUNT
        ) {
            throw IOException("The selected image is corrupted or too large to process.")
        }

        val sampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        val bitmap = BitmapFactory.decodeByteArray(
            source,
            0,
            source.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: throw IOException("The selected image could not be decoded.")

        val oriented = applyExifOrientation(source, bitmap)
        val scaled = resize(oriented)
        if (scaled !== oriented) oriented.recycle()
        if (bitmap !== oriented) bitmap.recycle()

        val output = java.io.File(context.cacheDir, "avatar-${UUID.randomUUID()}.jpg")
        try {
            output.outputStream().use { stream ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 88, stream)) {
                    throw IOException("The selected image could not be optimized.")
                }
            }
        } catch (exception: IOException) {
            output.delete()
            throw exception
        } finally {
            scaled.recycle()
        }
        if (output.length() > MAX_FILE_SIZE_BYTES) {
            output.delete()
            throw IOException("The optimized image exceeds the 5 MB limit.")
        }
        return output
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_FILE_SIZE_BYTES) {
                throw IOException("Avatar images must be 5 MB or smaller.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun isSupportedImage(bytes: ByteArray): Boolean =
        (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) ||
            (bytes.size >= 8 && bytes[0] == 0x89.toByte() &&
                bytes.copyOfRange(1, 4).contentEquals("PNG".toByteArray()) &&
                bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
                bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()) ||
            (bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
                bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray()))

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > MAX_DIMENSION * 2) {
            sample *= 2
        }
        return sample
    }

    private fun applyExifOrientation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / longestSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
