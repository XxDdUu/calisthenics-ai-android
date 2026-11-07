package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanDayResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanResponseDto
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkoutPlanMapperTest {

    @Test
    fun workoutPlanResponseDto_mapsToDomainWithDays() {
        val dto = WorkoutPlanResponseDto(
            id = 101L,
            name = "Advanced Planche Plan",
            splitType = "PPL",
            createdAt = "2026-10-26T08:00:00Z",
            updatedAt = "2026-10-26T08:00:00Z",
            days = listOf(
                WorkoutPlanDayResponseDto(id = 1L, dayOfWeek = "MONDAY", sessionType = "PUSH"),
                WorkoutPlanDayResponseDto(id = 2L, dayOfWeek = "TUESDAY", sessionType = "PULL"),
                WorkoutPlanDayResponseDto(id = 3L, dayOfWeek = "WEDNESDAY", sessionType = "REST")
            )
        )

        val domain = dto.toDomain()

        assertEquals(101L, domain.id)
        assertEquals("Advanced Planche Plan", domain.name)
        assertEquals(WorkoutSplitType.PPL, domain.splitType)
        assertEquals(3, domain.days.size)
        assertEquals(2, domain.trainingDayCount)
        assertEquals(WorkoutDayOfWeek.MONDAY, domain.days[0].dayOfWeek)
        assertEquals(WorkoutSessionType.PUSH, domain.days[0].sessionType)
        assertEquals(1L, domain.days[0].id)
    }

    @Test
    fun workoutPlanExerciseResponseDto_mapsToDomain() {
        val exerciseDto = ExerciseResponseDto(
            id = 12L,
            name = "Straddle Planche",
            code = "STRADDLE_PLANCHE",
            category = "SKILL",
            difficulty = "ADVANCED",
            description = "Hold parallel to floor",
            targetMuscles = "Shoulders",
            instructions = "Protract scapula"
        )
        val dto = WorkoutPlanExerciseResponseDto(
            id = 55L,
            exercise = exerciseDto,
            sets = 4,
            reps = null,
            durationSeconds = 15,
            restSeconds = 90,
            displayOrder = 1,
            notes = "Focus on protraction"
        )

        val domain = dto.toDomain()

        assertEquals(55L, domain.id)
        assertEquals(12L, domain.exercise.id)
        assertEquals(4, domain.sets)
        assertEquals(15, domain.durationSeconds)
        assertEquals(90, domain.restSeconds)
        assertEquals(1, domain.displayOrder)
        assertEquals("4 sets × 15s hold", domain.prescription)
    }
}
