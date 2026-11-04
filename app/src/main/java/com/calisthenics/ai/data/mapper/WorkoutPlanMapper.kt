package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.WorkoutPlanDayResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanTemplateResponseDto
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.UserWorkoutPlanTemplate
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType

fun WorkoutPlanResponseDto.toDomain(
    dayExercisesMap: Map<Long, List<UserWorkoutPlanExercise>> = emptyMap()
): UserWorkoutPlan {
    val split = try {
        WorkoutSplitType.valueOf(splitType.uppercase())
    } catch (_: Exception) {
        WorkoutSplitType.PPL
    }

    val domainDays = days?.map { dayDto ->
        val dayId = dayDto.id ?: 0L
        val passedExercises = dayExercisesMap[dayId].orEmpty()
        val finalExercises = if (passedExercises.isNotEmpty()) {
            passedExercises
        } else {
            dayDto.exercises?.map { it.toDomain() }?.sortedBy { it.displayOrder } ?: emptyList()
        }
        dayDto.toDomain(finalExercises)
    }?.sortedBy { it.dayOfWeek.order } ?: emptyList()

    return UserWorkoutPlan(
        id = id,
        name = name,
        splitType = split,
        createdAt = createdAt,
        updatedAt = updatedAt,
        days = domainDays
    )
}

fun WorkoutPlanDayResponseDto.toDomain(
    exercises: List<UserWorkoutPlanExercise> = emptyList()
): UserWorkoutPlanDay {
    val mappedExercises = if (exercises.isNotEmpty()) {
        exercises
    } else {
        this.exercises?.map { it.toDomain() }?.sortedBy { it.displayOrder } ?: emptyList()
    }
    return UserWorkoutPlanDay(
        id = id ?: 0L,
        dayOfWeek = WorkoutDayOfWeek.fromName(dayOfWeek),
        sessionType = WorkoutSessionType.fromName(sessionType),
        name = name,
        exercises = mappedExercises
    )
}

fun WorkoutPlanExerciseResponseDto.toDomain(): UserWorkoutPlanExercise {
    return UserWorkoutPlanExercise(
        id = id,
        exercise = exercise.toDomain(),
        sets = sets,
        reps = reps,
        durationSeconds = durationSeconds,
        restSeconds = restSeconds,
        displayOrder = displayOrder,
        notes = notes
    )
}

fun WorkoutPlanTemplateResponseDto.toDomain(): UserWorkoutPlanTemplate {
    val split = try {
        WorkoutSplitType.valueOf(splitType.uppercase())
    } catch (_: Exception) {
        WorkoutSplitType.PPL
    }

    return UserWorkoutPlanTemplate(
        splitType = split,
        days = days.map { it.toDomain() }.sortedBy { it.dayOfWeek.order }
    )
}
