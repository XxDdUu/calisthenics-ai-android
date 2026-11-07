package com.calisthenics.ai.ui.home

import com.calisthenics.ai.domain.model.ExerciseMedia
import com.calisthenics.ai.domain.model.ExerciseMediaType
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay

data class ResolvedWorkoutMedia(
    val media: ExerciseMedia?,
    val fallbackMedia: ExerciseMedia?,
    val globalFallback: ExerciseMedia?,
    val usesGlobalFallback: Boolean
)

fun resolveWorkoutMedia(
    workout: UserWorkoutPlanDay,
    categoryFallback: ExerciseMedia?,
    globalFallback: ExerciseMedia?
): ResolvedWorkoutMedia {
    val ownMedia = workout.exercises
        .asSequence()
        .flatMap { it.exercise.mediaItems.asSequence() }
        .toList()
        .preferredMedia()

    return ResolvedWorkoutMedia(
        media = ownMedia,
        fallbackMedia = categoryFallback,
        globalFallback = globalFallback,
        usesGlobalFallback = ownMedia == null && categoryFallback == null
    )
}

private fun isVisualMedia(media: ExerciseMedia): Boolean =
    media.type == ExerciseMediaType.IMAGE || media.type == ExerciseMediaType.VIDEO

fun List<ExerciseMedia>.preferredMedia(): ExerciseMedia? =
    filter(::isVisualMedia)
        .minWithOrNull(
            compareBy<ExerciseMedia> { if (it.type == ExerciseMediaType.VIDEO) 0 else 1 }
                .thenBy { it.displayOrder }
        )
