package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.remote.datasource.RemoteLiveAnalysisDataSource
import com.calisthenics.ai.data.remote.dto.AiPerformanceSummaryDto
import com.calisthenics.ai.data.remote.dto.AnalysisSessionResponseDto
import com.calisthenics.ai.data.remote.dto.CompleteAnalysisSessionRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAnalysisSessionRequestDto
import com.calisthenics.ai.data.remote.dto.DetectedFormErrorDto
import com.calisthenics.ai.data.remote.dto.ExerciseFormMetricDto
import com.calisthenics.ai.data.remote.dto.RepHistoryDto
import com.calisthenics.ai.domain.model.AiPerformanceSummary
import com.calisthenics.ai.domain.model.DetectedFormError
import com.calisthenics.ai.domain.model.ErrorSeverity
import com.calisthenics.ai.domain.model.ExerciseFormMetric
import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.model.LiveAnalysisSummary
import com.calisthenics.ai.domain.model.LiveFeedback
import com.calisthenics.ai.domain.model.LiveLandmark
import com.calisthenics.ai.domain.model.LiveRepRecord
import com.calisthenics.ai.domain.model.LiveScoringMetric
import com.calisthenics.ai.domain.model.LiveSessionRecord
import com.calisthenics.ai.domain.model.LiveSessionSummary
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
            currentScore = response.currentScore ?: response.score,
            scoreConfidence = response.scoreConfidence,
            confidence = response.confidence,
            poseDetected = response.poseDetected ?: false,
            isValidHold = response.isValidHold ?: false,
            holdDurationSec = response.holdDurationSec,
            holdDurationMs = response.holdDurationMs ?: response.holdDurationSec?.let { (it * 1000).toLong() },
            repCount = response.repCount,
            repPhase = response.repPhase,
            phase = response.phase ?: response.repPhase,
            currentState = response.currentState,
            kneeAngle = response.kneeAngle ?: response.measurements["knee_angle"],
            minKneeAngle = response.minKneeAngle ?: response.measurements["min_knee_angle"],
            elbowAngle = response.elbowAngle ?: response.measurements["elbow_angle"],
            minElbowAngle = response.minElbowAngle ?: response.measurements["min_elbow_angle"],
            bodyAlignment = response.bodyAlignment ?: response.measurements["body_alignment"],
            depth = response.depth,
            isTracking = response.isTracking,
            measurements = response.measurements,
            scoringMetrics = response.metrics.map {
                LiveScoringMetric(
                    name = it.name.orEmpty(),
                    value = it.value ?: 0.0,
                    score = it.score ?: 0.0,
                    weight = it.weight ?: 0.0,
                    confidence = it.confidence ?: 0.0,
                    status = it.status.orEmpty()
                )
            },
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
            sessionId = response.sessionId,
            sessionSummary = response.sessionSummary?.let {
                LiveSessionSummary(
                    sessionId = it.sessionId.orEmpty(),
                    exercise = it.exercise.orEmpty(),
                    totalFrames = it.totalFrames ?: 0,
                    durationSec = it.durationSec ?: 0.0,
                    averageScore = it.averageScore ?: 0.0,
                    peakScore = it.peakScore ?: 0.0,
                    lowestScore = it.lowestScore ?: 0.0,
                    peakHoldDurationSec = it.peakHoldDurationSec ?: 0.0,
                    totalReps = it.totalReps ?: 0
                )
            }
        )
    }

    override suspend fun createSession(exerciseCode: String, sessionId: String?): Result<LiveSessionRecord> = runCatching {
        remoteDataSource.createAnalysisSession(
            CreateAnalysisSessionRequestDto(
                exerciseType = exerciseCode,
                sessionId = sessionId
            )
        ).toDomain()
    }

    override suspend fun recordRep(sessionId: String, rep: LiveRepRecord): Result<LiveSessionRecord> = runCatching {
        remoteDataSource.recordRep(
            sessionId = sessionId,
            rep = rep.toDto()
        ).toDomain()
    }

    override suspend fun completeSession(
        sessionId: String,
        totalReps: Int,
        durationSeconds: Double,
        overallScore: Double?,
        averageFormScore: Double?,
        bestFormScore: Double?,
        lowestFormScore: Double?,
        analysisConfidence: Double?,
        peakHoldDurationSeconds: Double?,
        averageKneeAngle: Double?,
        deepestKneeAngle: Double?,
        averageJointAngle: Double?,
        deepestJointAngle: Double?,
        correctReps: Int?,
        incorrectReps: Int?,
        exerciseMetrics: List<ExerciseFormMetric>,
        detectedErrors: List<DetectedFormError>,
        originalVideoUrl: String?,
        annotatedVideoUrl: String?,
        reps: List<LiveRepRecord>
    ): Result<LiveSessionRecord> = runCatching {
        remoteDataSource.completeAnalysisSession(
            sessionId = sessionId,
            request = CompleteAnalysisSessionRequestDto(
                totalReps = totalReps,
                durationSeconds = durationSeconds,
                overallScore = overallScore,
                averageFormScore = averageFormScore,
                bestFormScore = bestFormScore,
                lowestFormScore = lowestFormScore,
                analysisConfidence = analysisConfidence,
                peakHoldDurationSeconds = peakHoldDurationSeconds,
                averageKneeAngle = averageKneeAngle,
                deepestKneeAngle = deepestKneeAngle,
                averageJointAngle = averageJointAngle ?: averageKneeAngle,
                deepestJointAngle = deepestJointAngle ?: deepestKneeAngle,
                correctReps = correctReps,
                incorrectReps = incorrectReps,
                analysisStatus = "COMPLETED",
                exerciseMetrics = exerciseMetrics.map { it.toDto() },
                detectedErrors = detectedErrors.map { it.toDto() },
                originalVideoUrl = originalVideoUrl,
                annotatedVideoUrl = annotatedVideoUrl,
                reps = reps.map { it.toDto() }
            )
        ).toDomain()
    }

    override suspend fun generateSessionAiSummary(sessionId: String): Result<AiPerformanceSummary> = runCatching {
        remoteDataSource.generateAnalysisSessionAiSummary(sessionId).toDomain()
    }

    override suspend fun getSessions(exerciseType: String?): Result<List<LiveSessionRecord>> = runCatching {
        remoteDataSource.getAnalysisSessions(exerciseType).map { it.toDomain() }
    }

    override suspend fun getSessionDetails(sessionId: String): Result<LiveSessionRecord> = runCatching {
        remoteDataSource.getAnalysisSessionDetails(sessionId).toDomain()
    }

    override suspend fun getHistorySummary(exerciseType: String?): Result<LiveAnalysisSummary> = runCatching {
        val dto = remoteDataSource.getAnalysisHistorySummary(exerciseType)
        LiveAnalysisSummary(
            totalSessions = dto.totalSessions,
            totalReps = dto.totalReps,
            overallAverageScore = dto.overallAverageScore,
            totalDurationSeconds = dto.totalDurationSeconds,
            mostFrequentExercise = dto.mostFrequentExercise
        )
    }
}

private fun LiveRepRecord.toDto() = RepHistoryDto(
    id = id,
    repNumber = repNumber,
    startedAt = startedAt,
    completedAt = completedAt,
    durationMs = durationMs,
    timestampOffsetMs = timestampOffsetMs,
    minimumKneeAngle = minKneeAngle,
    minimumJointAngle = minJointAngle ?: minElbowAngle ?: minKneeAngle,
    formScore = score,
    depthRating = depth,
    completionStatus = completionStatus,
    feedback = feedback,
    detectedErrors = detectedErrors,
    metrics = metrics.takeIf { it.isNotEmpty() }
)

private fun ExerciseFormMetric.toDto() = ExerciseFormMetricDto(
    name = name,
    label = label,
    value = value,
    unit = unit,
    score = score,
    confidence = confidence,
    status = status,
    unavailableReason = unavailableReason
)

private fun DetectedFormError.toDto() = DetectedFormErrorDto(
    code = code,
    errorName = errorName,
    occurrences = occurrences,
    severity = severity.name,
    timestampsMs = timestampsMs,
    repNumbers = repNumbers,
    suggestedCorrection = suggestedCorrection
)

private fun AnalysisSessionResponseDto.toDomain() = LiveSessionRecord(
    id = this.id,
    userId = this.userId,
    exerciseType = this.exerciseType,
    exerciseId = this.exerciseId,
    status = this.analysisStatus ?: "COMPLETED",
    totalReps = this.totalReps,
    correctReps = this.correctReps ?: this.reps?.count { (it.formScore ?: 0.0) >= 70.0 && !it.depthRating.equals("SHALLOW", ignoreCase = true) } ?: 0,
    incorrectReps = this.incorrectReps ?: this.reps?.count { (it.formScore ?: 0.0) < 70.0 || it.depthRating.equals("SHALLOW", ignoreCase = true) } ?: 0,
    overallScore = this.overallScore ?: this.averageFormScore,
    averageScore = this.averageFormScore,
    peakScore = this.bestFormScore,
    lowestScore = this.lowestFormScore,
    analysisConfidence = this.analysisConfidence,
    peakHoldDurationSeconds = this.peakHoldDurationSeconds,
    averageKneeAngle = this.averageKneeAngle,
    deepestKneeAngle = this.deepestKneeAngle,
    averageJointAngle = this.averageJointAngle ?: this.averageKneeAngle,
    deepestJointAngle = this.deepestJointAngle ?: this.deepestKneeAngle,
    durationSeconds = this.durationSeconds ?: 0.0,
    cameraView = this.cameraView ?: "SIDE",
    createdAt = this.createdAt ?: this.startedAt,
    startedAt = this.startedAt,
    completedAt = this.completedAt,
    exerciseMetrics = this.exerciseMetrics?.map { it.toDomain() } ?: emptyList(),
    detectedErrors = this.detectedErrors?.map { it.toDomain() } ?: emptyList(),
    aiSummary = this.aiSummary,
    aiRecommendations = this.aiRecommendations?.toDomain(),
    aiSummaryStatus = this.aiSummaryStatus ?: "PENDING",
    originalVideoUrl = this.originalVideoUrl,
    annotatedVideoUrl = this.annotatedVideoUrl,
    reps = this.reps?.map {
        LiveRepRecord(
            id = it.id,
            repNumber = it.repNumber,
            score = it.formScore,
            minKneeAngle = it.minimumKneeAngle,
            minJointAngle = it.minimumJointAngle ?: it.minimumKneeAngle,
            minElbowAngle = it.minimumJointAngle,
            depth = it.depthRating,
            completionStatus = it.completionStatus,
            durationMs = it.durationMs,
            timestampOffsetMs = it.timestampOffsetMs,
            startedAt = it.startedAt,
            completedAt = it.completedAt,
            isValid = (it.formScore ?: 0.0) >= 70.0 && !it.depthRating.equals("SHALLOW", ignoreCase = true),
            feedback = it.feedback,
            detectedErrors = it.detectedErrors ?: emptyList(),
            metrics = it.metrics ?: emptyMap()
        )
    } ?: emptyList()
)

private fun ExerciseFormMetricDto.toDomain() = ExerciseFormMetric(
    name = name.orEmpty(),
    label = label ?: name.orEmpty(),
    value = value,
    unit = unit.orEmpty(),
    score = score,
    confidence = confidence ?: 0.0,
    status = status ?: "UNAVAILABLE",
    unavailableReason = unavailableReason
)

private fun DetectedFormErrorDto.toDomain() = DetectedFormError(
    code = code.orEmpty(),
    errorName = errorName ?: code.orEmpty(),
    occurrences = occurrences ?: 1,
    severity = ErrorSeverity.fromString(severity),
    timestampsMs = timestampsMs ?: emptyList(),
    repNumbers = repNumbers ?: emptyList(),
    suggestedCorrection = suggestedCorrection
)

private fun AiPerformanceSummaryDto.toDomain() = AiPerformanceSummary(
    overallAssessment = overallAssessment,
    strengths = strengths ?: emptyList(),
    weaknesses = weaknesses ?: emptyList(),
    recommendations = recommendations ?: emptyList(),
    nextSessionFocus = nextSessionFocus,
    modelUsed = modelUsed,
    inputTokens = inputTokens,
    outputTokens = outputTokens,
    latencyMs = latencyMs,
    status = status ?: "PENDING",
    generatedAt = generatedAt
)
