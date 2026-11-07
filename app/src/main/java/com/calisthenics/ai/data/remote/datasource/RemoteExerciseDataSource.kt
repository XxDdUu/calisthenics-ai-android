package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import javax.inject.Inject

class RemoteExerciseDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun getExercises(): List<ExerciseResponseDto> {
        return api.getExercises()
    }

    suspend fun getExerciseById(id: Long): ExerciseResponseDto {
        return api.getExerciseById(id)
    }

    suspend fun getExerciseByCode(code: String): ExerciseResponseDto {
        return api.getExerciseByCode(code)
    }
}
