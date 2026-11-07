package com.calisthenics.ai.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    val code: String,
    val category: ExerciseCategory,
    val difficulty: DifficultyLevel,
    val description: String,
    val targetMuscles: String,
    val instructions: String,
    val mediaItems: List<ExerciseMedia> = emptyList()
)

enum class ExerciseMediaType {
    IMAGE,
    VIDEO
}

data class ExerciseMedia(
    val type: ExerciseMediaType,
    val url: String,
    val thumbnailUrl: String? = null,
    val displayOrder: Int = 0
)

data class Progression(
    val id: Long,
    val name: String,
    val code: String,
    val difficulty: DifficultyLevel,
    val description: String,
    val targetMuscles: String,
    val instructions: String,
    val parentExerciseName: String
)
