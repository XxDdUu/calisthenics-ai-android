package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.LiveAnalysisResult
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
}
