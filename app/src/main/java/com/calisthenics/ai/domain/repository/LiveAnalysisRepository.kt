package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AiPerformanceSummary
import com.calisthenics.ai.domain.model.DetectedFormError
import com.calisthenics.ai.domain.model.ExerciseFormMetric
import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.model.LiveAnalysisSummary
import com.calisthenics.ai.domain.model.LiveRepRecord
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.model.SupportedExercise
import java.io.File

interface LiveAnalysisRepository {
    suspend fun getSupportedExercises(): Result<List<SupportedExercise>>
    suspend fun analyzeFrame(
        imageFile: File,
        exercise: String,
        sessionId: String,
        frameSequence: Long,
        timestampMs: Long,
        sideView: Boolean,
        drawOverlay: Boolean = false,
        debug: Boolean = false
    ): Result<LiveAnalysisResult>
    suspend fun createSession(exerciseCode: String, sessionId: String? = null): Result<LiveSessionRecord>
    suspend fun recordRep(sessionId: String, rep: LiveRepRecord): Result<LiveSessionRecord>
    suspend fun completeSession(
        sessionId: String,
        totalReps: Int,
        durationSeconds: Double,
        overallScore: Double? = null,
        averageFormScore: Double? = null,
        bestFormScore: Double? = null,
        lowestFormScore: Double? = null,
        analysisConfidence: Double? = null,
        peakHoldDurationSeconds: Double? = null,
        averageKneeAngle: Double? = null,
        deepestKneeAngle: Double? = null,
        averageJointAngle: Double? = null,
        deepestJointAngle: Double? = null,
        correctReps: Int? = null,
        incorrectReps: Int? = null,
        exerciseMetrics: List<ExerciseFormMetric> = emptyList(),
        detectedErrors: List<DetectedFormError> = emptyList(),
        originalVideoUrl: String? = null,
        annotatedVideoUrl: String? = null,
        reps: List<LiveRepRecord> = emptyList()
    ): Result<LiveSessionRecord>
    suspend fun generateSessionAiSummary(sessionId: String): Result<AiPerformanceSummary> =
        Result.failure(UnsupportedOperationException("AI summary generation unavailable"))
    suspend fun getSessions(exerciseType: String? = null): Result<List<LiveSessionRecord>>
    suspend fun getSessionDetails(sessionId: String): Result<LiveSessionRecord>
    suspend fun getHistorySummary(exerciseType: String? = null): Result<LiveAnalysisSummary>
}
