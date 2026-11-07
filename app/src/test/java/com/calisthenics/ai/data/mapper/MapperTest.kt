package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.ExerciseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapperTest {

    @Test
    fun exerciseResponseDto_mapsToDomainCorrectly() {
        val dto = ExerciseResponseDto(
            id = 1L,
            name = "Straddle Planche",
            code = "STRADDLE_PLANCHE",
            category = "SKILL",
            difficulty = "ADVANCED",
            description = "Planche with legs straddled.",
            targetMuscles = "Shoulders, Core",
            instructions = "Protract scapula."
        )

        val domain = dto.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("Straddle Planche", domain.name)
        assertEquals("STRADDLE_PLANCHE", domain.code)
        assertEquals(ExerciseCategory.SKILL, domain.category)
        assertEquals(DifficultyLevel.ADVANCED, domain.difficulty)
    }

    @Test
    fun analysisResponseDto_mapsToDomainAndSplitsFeedback() {
        val dto = AnalysisResponseDto(
            id = 10L,
            userId = 5L,
            exerciseId = 1L,
            exerciseName = "Straddle Planche",
            mediaUrl = "http://localhost/files/video.mp4",
            mediaType = "VIDEO",
            overallScore = 85.0,
            holdDurationSeconds = 4.2,
            formFeedback = "Keep scapula protracted; Lock out elbows completely; Good body line",
            elbowAngle = 175.0,
            bodyLineAngle = 178.0,
            shoulderLeanAngle = 45.0,
            rawKeypointsJson = "{}",
            createdAt = "2026-09-28T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(10L, domain.id)
        assertEquals(85.0, domain.overallScore!!, 0.01)
        assertEquals(4.2, domain.holdDurationSeconds!!, 0.01)
        assertEquals(3, domain.formFeedbackList.size)
        assertEquals("Keep scapula protracted", domain.formFeedbackList[0])
        assertEquals("Lock out elbows completely", domain.formFeedbackList[1])
        assertEquals("Good body line", domain.formFeedbackList[2])
    }

    @Test
    fun unanalysedImage_keepsUnavailableMetricsNull() {
        val dto = AnalysisResponseDto(
            id = 11L,
            userId = 5L,
            exerciseId = 1L,
            exerciseName = "Full Planche",
            mediaUrl = "http://localhost/files/image.jpg",
            mediaType = "IMAGE",
            overallScore = null,
            holdDurationSeconds = null,
            formFeedback = null,
            elbowAngle = null,
            bodyLineAngle = null,
            shoulderLeanAngle = null,
            rawKeypointsJson = null,
            createdAt = "2026-10-05T03:24:33.199234Z"
        )

        val domain = dto.toDomain()

        assertNull(domain.overallScore)
        assertNull(domain.holdDurationSeconds)
        assertEquals("IMAGE", domain.mediaType)
    }

    @Test
    fun analysedImage_preservesAvailableMetricsWithoutHoldDuration() {
        val dto = AnalysisResponseDto(
            id = 12L,
            userId = 5L,
            exerciseId = 1L,
            exerciseName = "Full Planche",
            mediaUrl = "http://localhost/files/image.jpg",
            mediaType = "IMAGE",
            overallScore = 87.5,
            holdDurationSeconds = null,
            formFeedback = "Good body line",
            elbowAngle = 178.0,
            bodyLineAngle = 174.0,
            shoulderLeanAngle = 32.0,
            rawKeypointsJson = """{"nose":{"x":12,"y":18}}""",
            createdAt = "2026-10-05T03:24:33.199234Z"
        )

        val domain = dto.toDomain()

        assertEquals(87.5, domain.overallScore!!, 0.01)
        assertNull(domain.holdDurationSeconds)
        assertEquals(178.0, domain.elbowAngle!!, 0.01)
        assertEquals(174.0, domain.bodyLineAngle!!, 0.01)
        assertEquals(32.0, domain.shoulderLeanAngle!!, 0.01)
        assertEquals("""{"nose":{"x":12,"y":18}}""", domain.rawKeypointsJson)
    }
}
