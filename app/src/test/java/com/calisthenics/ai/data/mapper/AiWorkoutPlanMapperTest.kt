package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.AiWorkoutExercisePreviewDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutMetadataDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutPlanPreviewDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutSessionPreviewDto
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiWorkoutPlanMapperTest {

    @Test
    fun toDomain_mapsFullPlanCorrectly() {
        val dto = AiWorkoutPlanPreviewDto(
            planName = "6-Day Planche & Front Lever Specialization",
            description = "High intensity gymnastics strength training",
            durationWeeks = 6,
            startDate = "2026-11-16",
            splitType = "CUSTOM",
            sessions = listOf(
                AiWorkoutSessionPreviewDto(
                    dayOfWeek = "MONDAY",
                    title = "Planche Strength",
                    restDay = false,
                    exercises = listOf(
                        AiWorkoutExercisePreviewDto(
                            exerciseId = 10L,
                            exerciseName = "Straddle Planche Hold",
                            exerciseCode = "planche_straddle_hold",
                            category = "PUSH",
                            sets = 4,
                            reps = null,
                            holdSeconds = 15,
                            restSeconds = 120,
                            notes = "Keep arms locked and protract scapulae",
                            requiresReview = false
                        )
                    )
                ),
                AiWorkoutSessionPreviewDto(
                    dayOfWeek = "SUNDAY",
                    title = "Rest & Recovery",
                    restDay = true,
                    exercises = emptyList()
                )
            ),
            metadata = AiWorkoutMetadataDto(
                modelUsed = "gemini-2.5-flash-lite",
                inputTokens = 1200,
                outputTokens = 650,
                latencyMs = 980L,
                hasConflicts = true,
                existingActivePlanName = "Beginner PPL"
            )
        )

        val domain = dto.toDomain()

        assertEquals("6-Day Planche & Front Lever Specialization", domain.planName)
        assertEquals(6, domain.durationWeeks)
        assertEquals(WorkoutSplitType.CUSTOM, domain.splitType)
        assertEquals(2, domain.sessions.size)

        val session1 = domain.sessions[0]
        assertEquals(WorkoutDayOfWeek.MONDAY, session1.dayOfWeek)
        assertEquals("Planche Strength", session1.title)
        assertFalse(session1.isRestDay)
        assertEquals(1, session1.exercises.size)

        val ex1 = session1.exercises[0]
        assertEquals(10L, ex1.exerciseId)
        assertEquals("4 sets × 15s hold", ex1.prescription)
        assertEquals(120, ex1.restSeconds)

        val session2 = domain.sessions[1]
        assertEquals(WorkoutDayOfWeek.SUNDAY, session2.dayOfWeek)
        assertTrue(session2.isRestDay)
        assertTrue(session2.exercises.isEmpty())

        assertTrue(domain.metadata.hasConflicts)
        assertEquals("Beginner PPL", domain.metadata.existingActivePlanName)
        assertEquals("gemini-2.5-flash-lite", domain.metadata.modelUsed)
    }

    @Test
    fun toDto_roundTripPreservesValues() {
        val dto = AiWorkoutPlanPreviewDto(
            planName = "Upper Lower",
            description = "4 week routine",
            durationWeeks = 4,
            startDate = "2026-11-10",
            splitType = "UPPER_LOWER",
            sessions = listOf(
                AiWorkoutSessionPreviewDto(
                    dayOfWeek = "TUESDAY",
                    title = "Pull Strength",
                    restDay = false,
                    exercises = listOf(
                        AiWorkoutExercisePreviewDto(
                            exerciseId = 5L,
                            exerciseName = "Weighted Pull-Up",
                            category = "PULL",
                            sets = 3,
                            reps = 8,
                            holdSeconds = null,
                            restSeconds = 90,
                            notes = "Full ROM",
                            requiresReview = false
                        )
                    )
                )
            ),
            metadata = AiWorkoutMetadataDto(
                modelUsed = "gemini-2.5-flash-lite",
                inputTokens = 800,
                outputTokens = 400,
                latencyMs = 700L,
                hasConflicts = false,
                existingActivePlanName = null
            )
        )

        val domain = dto.toDomain()
        val mappedDto = domain.toDto()

        assertEquals(dto.planName, mappedDto.planName)
        assertEquals(dto.splitType, mappedDto.splitType)
        assertEquals(dto.durationWeeks, mappedDto.durationWeeks)
        assertEquals(1, mappedDto.sessions.size)
        assertEquals("TUESDAY", mappedDto.sessions[0].dayOfWeek)
        assertEquals(8, mappedDto.sessions[0].exercises[0].reps)
    }

    @Test
    fun toDomain_and_toDto_sanitizeZeroRepsAndZeroHoldSecondsToNull() {
        val dtoWithZeros = AiWorkoutExercisePreviewDto(
            exerciseId = 2L,
            exerciseName = "Advanced Tuck Planche",
            category = "SKILL",
            sets = 4,
            reps = 0,
            holdSeconds = 12,
            restSeconds = 180
        )

        val domain = dtoWithZeros.toDomain()
        assertEquals(null, domain.reps)
        assertEquals(12, domain.holdSeconds)

        val zeroHoldDomain = domain.copy(holdSeconds = 0, reps = 0)
        val sanitizedDto = zeroHoldDomain.toDto()
        assertEquals(null, sanitizedDto.reps)
        assertEquals(null, sanitizedDto.holdSeconds)
    }
}
