package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.datasource.RemoteAnalysisDataSource
import com.calisthenics.ai.domain.model.AnalysisResult
import com.calisthenics.ai.domain.model.VideoKeypointsDataModel
import com.calisthenics.ai.domain.repository.AnalysisRepository
import java.io.File
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteAnalysisDataSource
) : AnalysisRepository {

    override suspend fun uploadVideoForAnalysis(
        videoFile: File,
        exerciseId: Long?
    ): Result<AnalysisResult> {
        return runCatching {
            remoteDataSource.uploadVideoForAnalysis(videoFile, exerciseId).toDomain()
        }
    }

    override suspend fun getVideoAnalysisById(id: Long): Result<AnalysisResult> {
        return runCatching {
            val analysis = remoteDataSource.getVideoAnalysisById(id).toDomain()
            if (analysis.status == "COMPLETED") {
                val keypoints = runCatching { remoteDataSource.getVideoAnalysisKeypoints(id).toDomain() }.getOrNull()
                analysis.copy(keypointsData = keypoints)
            } else {
                analysis
            }
        }
    }

    override suspend fun getVideoAnalysisKeypoints(id: Long): Result<VideoKeypointsDataModel> {
        return runCatching {
            remoteDataSource.getVideoAnalysisKeypoints(id).toDomain()
        }
    }

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
            val videoAnalyses = runCatching {
                remoteDataSource.getVideoAnalysisHistory().map { it.toDomain() }
            }.getOrDefault(emptyList())

            val legacyAnalyses = runCatching {
                remoteDataSource.getAnalysisHistory().map { it.toDomain() }
            }.getOrDefault(emptyList())

            (videoAnalyses + legacyAnalyses).sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getAnalysisById(id: Long): Result<AnalysisResult> {
        return runCatching {
            // First try video analysis endpoint
            try {
                val videoAnalysis = remoteDataSource.getVideoAnalysisById(id).toDomain()
                val keypoints = runCatching { remoteDataSource.getVideoAnalysisKeypoints(id).toDomain() }.getOrNull()
                videoAnalysis.copy(keypointsData = keypoints)
            } catch (e: Exception) {
                // Fallback to legacy endpoint
                remoteDataSource.getAnalysisById(id).toDomain()
            }
        }
    }
}
