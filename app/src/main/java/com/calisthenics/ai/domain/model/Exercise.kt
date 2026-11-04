package com.calisthenics.ai.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    val code: String,
    val category: ExerciseCategory,
    val difficulty: DifficultyLevel,
    val description: String,
    val targetMuscles: String,
    val instructions: String
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
