package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.model.*
import java.time.LocalDate

fun GettingStartedExerciseResponseDto.toDomain() = GettingStartedExercise(
    id = id,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    sets = sets,
    reps = reps,
    durationSeconds = durationSeconds,
    restSeconds = restSeconds,
    instructions = instructions,
    displayOrder = displayOrder
)

fun GettingStartedDayResponseDto.toDomain(): GettingStartedDay {
    val resolvedStatus = GettingStartedDayStatus.fromString(status, fallbackCompleted = completed)
    return GettingStartedDay(
        id = id,
        dayNumber = dayNumber,
        title = title,
        description = description,
        exercises = exercises.map { it.toDomain() },
        status = resolvedStatus,
        completed = resolvedStatus == GettingStartedDayStatus.COMPLETED,
        scheduledDate = scheduledDate,
        workoutSessionId = workoutSessionId,
        startedAt = startedAt,
        completedAt = completedAt
    )
}

fun GettingStartedDayProgressDto.toDomain() = GettingStartedDayProgress(
    dayNumber = dayNumber,
    title = title ?: "Day $dayNumber",
    status = GettingStartedDayStatus.fromString(status),
    scheduledDate = scheduledDate,
    workoutSessionId = workoutSessionId,
    startedAt = startedAt,
    completedAt = completedAt
)

fun GettingStartedScheduleConflictDto.toDomain() = GettingStartedScheduleConflict(
    date = date,
    gettingStartedDayNumber = gettingStartedDayNumber,
    gettingStartedDayTitle = gettingStartedDayTitle ?: "Day $gettingStartedDayNumber",
    existingWorkoutTitle = existingWorkoutTitle ?: "Scheduled Workout",
    existingSource = existingSource ?: "PERSONAL",
    workoutSessionId = workoutSessionId,
    workoutPlanId = workoutPlanId
)

fun GettingStartedEnrollPreviewDto.toDomain() = GettingStartedEnrollPreview(
    programId = programId,
    programTitle = programTitle,
    alreadyEnrolled = alreadyEnrolled,
    alreadyAddedToWorkoutPlan = alreadyAddedToWorkoutPlan,
    startDate = startDate ?: LocalDate.now().toString(),
    plannedDates = plannedDates,
    hasConflicts = hasConflicts,
    conflicts = conflicts.map { it.toDomain() }
)

fun GettingStartedEnrollmentResponseDto.toDomain() = GettingStartedEnrollment(
    id = id,
    programId = programId,
    programTitle = programTitle,
    programDescription = programDescription,
    programVersion = programVersion,
    status = status,
    addedToWorkoutPlan = addedToWorkoutPlan,
    startDate = startDate,
    enrolledAt = enrolledAt,
    startedAt = startedAt,
    completedAt = completedAt,
    completedDays = completedDays,
    dayProgress = dayProgress.map { it.toDomain() },
    completedDaysCount = completedDaysCount,
    totalDays = totalDays,
    isCompleted = isCompleted || status.equals("COMPLETED", ignoreCase = true)
)

fun GettingStartedProgramResponseDto.toDomain() = GettingStartedProgram(
    id = id,
    title = title,
    description = description,
    targetAudience = TargetAudience.fromString(targetAudience),
    published = published,
    version = version,
    creatorId = creatorId,
    creatorName = creatorName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    publishedAt = publishedAt,
    eligible = eligible,
    userDismissalState = OnboardingStatus.fromApi(userDismissalState),
    days = days.map { it.toDomain() },
    enrollment = enrollment?.toDomain()
)

fun AdminProgramDraft.toDto() = AdminGettingStartedProgramRequestDto(
    title = title,
    description = description,
    targetAudience = targetAudience.name,
    days = days.map { day ->
        AdminGettingStartedDayRequestDto(
            dayNumber = day.dayNumber,
            title = day.title,
            description = day.description,
            exercises = day.exercises.map { ex ->
                AdminGettingStartedExerciseRequestDto(
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    sets = ex.sets,
                    reps = ex.reps,
                    durationSeconds = ex.durationSeconds,
                    restSeconds = ex.restSeconds,
                    instructions = ex.instructions,
                    displayOrder = ex.displayOrder
                )
            }
        )
    }
)
