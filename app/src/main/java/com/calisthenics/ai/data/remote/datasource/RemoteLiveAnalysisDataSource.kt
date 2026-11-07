package com.calisthenics.ai.data.remote.datasource

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
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
