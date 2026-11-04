package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.Exercise

interface ExerciseRepository {
    suspend fun getExercises(): Result<List<Exercise>>
    suspend fun getExerciseById(id: Long): Result<Exercise>
    suspend fun getExerciseByCode(code: String): Result<Exercise>
}
