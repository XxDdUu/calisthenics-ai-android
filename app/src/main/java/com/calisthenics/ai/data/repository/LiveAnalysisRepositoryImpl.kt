package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.remote.datasource.RemoteLiveAnalysisDataSource
import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.model.LiveFeedback
import com.calisthenics.ai.domain.model.LiveLandmark
import com.calisthenics.ai.domain.model.LiveTiming
import com.calisthenics.ai.domain.model.SupportedExercise
import com.calisthenics.ai.domain.repository.LiveAnalysisRepository
import java.io.File
import javax.inject.Inject

class LiveAnalysisRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteLiveAnalysisDataSource
) : LiveAnalysisRepository {

    override suspend fun getSupportedExercises(): Result<List<SupportedExercise>> = runCatching {
        remoteDataSource.getSupportedExercises().allCatalog.map { dto ->
            SupportedExercise(
                exercise = dto.exercise.orEmpty(),
                name = dto.name.orEmpty(),
                type = dto.type.orEmpty(),
                cameraView = dto.cameraView.orEmpty(),
                variations = dto.variations
            )
        }
    }

    override suspend fun analyzeFrame(
        imageFile: File,
        exercise: String,
        sessionId: String,
        frameSequence: Long,
        timestampMs: Long,
        sideView: Boolean,
        drawOverlay: Boolean,
        debug: Boolean
    ): Result<LiveAnalysisResult> = runCatching {
        val response = remoteDataSource.analyzeFrame(
            imageFile = imageFile,
            exercise = exercise,
            sessionId = sessionId,
            frameSequence = frameSequence,
            timestampMs = timestampMs,
            sideView = sideView,
            drawOverlay = drawOverlay,
            debug = debug
        )

        LiveAnalysisResult(
            status = response.status.orEmpty(),
            exercise = response.exercise.orEmpty(),
            score = response.score,
            confidence = response.confidence,
            poseDetected = response.poseDetected ?: false,
            isValidHold = response.isValidHold ?: false,
            holdDurationSec = response.holdDurationSec,
            repCount = response.repCount,
            repPhase = response.repPhase,
            measurements = response.measurements,
            missingLandmarks = response.missingLandmarks,
            feedback = response.feedback.map {
                LiveFeedback(
                    code = it.code.orEmpty(),
                    message = it.message.orEmpty(),
                    severity = it.severity.orEmpty()
                )
            },
            landmarks = response.landmarks.map {
                LiveLandmark(
                    name = it.name.orEmpty(),
                    x = it.x ?: 0.0,
                    y = it.y ?: 0.0,
                    xNorm = it.xNorm ?: 0.0,
                    yNorm = it.yNorm ?: 0.0,
                    confidence = it.confidence ?: 0.0,
                    visible = it.visible ?: false
                )
            },
            timing = response.timing?.let {
                LiveTiming(
                    preprocessingMs = it.preprocessingMs,
                    inferenceMs = it.inferenceMs,
                    analysisMs = it.analysisMs,
                    totalMs = it.totalMs
                )
            },
            sessionId = response.sessionId
        )
    }
}
