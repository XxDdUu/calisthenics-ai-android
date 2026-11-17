package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.AiPerformanceSummaryDto
import com.calisthenics.ai.data.remote.dto.AnalysisHistorySummaryDto
import com.calisthenics.ai.data.remote.dto.AnalysisSessionResponseDto
import com.calisthenics.ai.data.remote.dto.CompleteAnalysisSessionRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAnalysisSessionRequestDto
import com.calisthenics.ai.data.remote.dto.LiveAnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.LiveExerciseCatalogDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class RemoteLiveAnalysisDataSource @Inject constructor(
    private val api: CalisthenicsApi
) {
    suspend fun getSupportedExercises(): LiveExerciseCatalogDto = api.getLiveExercises()

    suspend fun createAnalysisSession(request: CreateAnalysisSessionRequestDto): AnalysisSessionResponseDto =
        api.createAnalysisSession(request)

    suspend fun recordRep(sessionId: String, rep: com.calisthenics.ai.data.remote.dto.RepHistoryDto): AnalysisSessionResponseDto =
        api.recordRep(sessionId, rep)

    suspend fun completeAnalysisSession(
        sessionId: String,
        request: CompleteAnalysisSessionRequestDto
    ): AnalysisSessionResponseDto =
        api.completeAnalysisSession(sessionId, request)

    suspend fun generateAnalysisSessionAiSummary(sessionId: String): AiPerformanceSummaryDto =
        api.generateAnalysisSessionAiSummary(sessionId)

    suspend fun getAnalysisSessions(exerciseType: String? = null): List<AnalysisSessionResponseDto> =
        api.getAnalysisSessions(exerciseType)

    suspend fun getAnalysisSessionDetails(sessionId: String): AnalysisSessionResponseDto =
        api.getAnalysisSessionDetails(sessionId)

    suspend fun getAnalysisHistorySummary(exerciseType: String? = null): AnalysisHistorySummaryDto =
        api.getAnalysisHistorySummary(exerciseType)

    suspend fun analyzeFrame(
        imageFile: File,
        exercise: String,
        sessionId: String,
        frameSequence: Long,
        timestampMs: Long,
        sideView: Boolean,
        drawOverlay: Boolean,
        debug: Boolean
    ): LiveAnalysisResponseDto {
        val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val multipart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
        val exercisePart = exercise.toRequestBody("text/plain".toMediaTypeOrNull())
        val sessionPart = sessionId.toRequestBody("text/plain".toMediaTypeOrNull())
        val seqPart = frameSequence.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val timePart = timestampMs.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val sidePart = sideView.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val drawPart = drawOverlay.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val debugPart = debug.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        return api.analyzeLiveFrame(
            file = multipart,
            exercise = exercisePart,
            sessionId = sessionPart,
            frameSequence = seqPart,
            timestampMs = timePart,
            sideView = sidePart,
            drawOverlay = drawPart,
            debug = debugPart
        )
    }
}
