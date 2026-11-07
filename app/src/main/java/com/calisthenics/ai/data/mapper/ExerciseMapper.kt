package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.ExerciseMediaResponseDto
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType

fun ExerciseResponseDto.toDomain(): Exercise {
    val mediaItems = buildList {
        mediaItems.orEmpty().forEachIndexed { index, item ->
            item.toDomainOrNull(index)?.let(::add)
        }
        imageUrl?.takeIf(String::isNotBlank)?.let { image ->
            val normalizedImage = image.toSecureMediaUrl()
            if (none { it.type == ExerciseMediaType.IMAGE && it.url == normalizedImage }) {
                add(ExerciseMedia(ExerciseMediaType.IMAGE, normalizedImage, displayOrder = size))
            }
        }
        videoUrl?.takeIf(String::isNotBlank)?.let { video ->
            val normalizedVideo = video.toSecureMediaUrl()
            if (none { it.type == ExerciseMediaType.VIDEO && it.url == normalizedVideo }) {
                add(ExerciseMedia(ExerciseMediaType.VIDEO, normalizedVideo, displayOrder = size))
            }
        }
    }.sortedWith(compareBy<ExerciseMedia> { it.displayOrder }.thenBy { it.type.ordinal })

    return Exercise(
        id = id,
        name = name ?: "Unknown Exercise",
        code = code ?: "",
        category = try {
            ExerciseCategory.valueOf(category?.uppercase() ?: "UNKNOWN")
        } catch (e: IllegalArgumentException) {
            ExerciseCategory.UNKNOWN
        },
        difficulty = try {
            DifficultyLevel.valueOf(difficulty?.uppercase() ?: "UNKNOWN")
        } catch (e: IllegalArgumentException) {
            DifficultyLevel.UNKNOWN
        },
        description = description ?: "",
        targetMuscles = targetMuscles ?: "",
        instructions = instructions ?: "",
        mediaItems = mediaItems
    )
}

fun ExerciseMediaResponseDto.toDomainOrNull(defaultDisplayOrder: Int = 0): ExerciseMedia? {
    val url = url?.takeIf(String::isNotBlank) ?: return null
    val type = ExerciseMediaType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) } ?: return null
    return ExerciseMedia(
        type = type,
        url = url.toSecureMediaUrl(),
        thumbnailUrl = thumbnailUrl?.takeIf(String::isNotBlank)?.toSecureMediaUrl(),
        displayOrder = displayOrder ?: defaultDisplayOrder
    )
}

/**
 * Production's reverse proxy serves media over HTTPS, but older records contain
 * the internal HTTP form of the same public URL. Cleartext traffic is disabled
 * for the app, so upgrade only legacy HTTP URLs while preserving other schemes.
 */
private fun String.toSecureMediaUrl(): String =
    if (startsWith("http://", ignoreCase = true)) {
        "https://${substring("http://".length)}"
    } else {
        this
    }
