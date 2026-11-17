package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.WorkoutSessionExerciseDto
import com.calisthenics.ai.data.remote.dto.WorkoutSessionResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutSessionSetDto
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.model.WorkoutSessionExercise
import com.calisthenics.ai.domain.model.WorkoutSessionSet

fun WorkoutSessionResponseDto.toDomain() = WorkoutSession(
    id,
    workoutPlanId,
    workoutPlanDayId,
    assignmentId,
    source,
    title,
    workoutDate,
    status,
    startedAt,
    lastSavedAt,
    completedAt,
    durationSeconds,
    completedExercises,
    skippedExercises,
    unfinishedExercises,
    completedSets,
    totalReps,
    totalHoldDurationSeconds,
    exercises.map { it.toDomain() },
    classification,
    gettingStartedEnrollmentId,
    gettingStartedDayNumber
)

private fun WorkoutSessionExerciseDto.toDomain() = WorkoutSessionExercise(
    id,
    version,
    exerciseId,
    exerciseName,
    instructions,
    notes,
    orderIndex,
    targetSets,
    targetReps,
    targetHoldDurationSeconds,
    restSeconds,
    status,
    sets.map { it.toDomain() }
)

private fun WorkoutSessionSetDto.toDomain() = WorkoutSessionSet(
    id,
    version,
    setNumber,
    reps,
    holdDurationSeconds,
    weightKg,
    assistanceWeightKg,
    completed,
    completedAt,
    notes
)
