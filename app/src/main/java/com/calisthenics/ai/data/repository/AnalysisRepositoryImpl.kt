package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteAnalysisDataSource
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.repository.AnalysisRepository
import java.io.File
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteAnalysisDataSource
) : AnalysisRepository {

    override suspend fun analyzeVideo(
        videoFile: File,
        exerciseId: Long?
    ): Result<AnalysisResult> {
        return runCatching {
            remoteDataSource.analyzeVideo(videoFile, exerciseId).toDomain()
        }
    }

    override suspend fun analyzeImage(
        imageFile: File,
        exerciseId: Long?
    ): Result<AnalysisResult> {
        return runCatching {
            remoteDataSource.analyzeImage(imageFile, exerciseId).toDomain()
        }
    }

    override suspend fun getAnalysisHistory(): Result<List<AnalysisResult>> {
        return runCatching {
            remoteDataSource.getAnalysisHistory().map { it.toDomain() }
        }
    }

    override suspend fun getAnalysisById(id: Long): Result<AnalysisResult> {
        return runCatching {
            remoteDataSource.getAnalysisById(id).toDomain()
        }
    }
}
