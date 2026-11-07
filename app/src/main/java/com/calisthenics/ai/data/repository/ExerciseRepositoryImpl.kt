package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteExerciseDataSource
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.repository.ExerciseRepository
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteExerciseDataSource,
) : ExerciseRepository {

    override suspend fun getExercises(): Result<List<Exercise>> {
        return runCatching {
            remoteDataSource.getExercises().map { it.toDomain() }
        }
    }

    override suspend fun getExerciseById(id: Long): Result<Exercise> {
        return runCatching {
            remoteDataSource.getExerciseById(id).toDomain()
        }
    }

    override suspend fun getExerciseByCode(code: String): Result<Exercise> {
        return runCatching {
            remoteDataSource.getExerciseByCode(code).toDomain()
        }
    }
}
