package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory

fun ExerciseResponseDto.toDomain(): Exercise {
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
        instructions = instructions ?: ""
    )
}
