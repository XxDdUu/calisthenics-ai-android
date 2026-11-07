package com.calisthenics.ai.data.mapper

import com.calisthenics.ai.data.local.database.CachedWorkoutPlanDayEntity
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanEntity
import com.calisthenics.ai.data.local.database.CachedWorkoutPlanExerciseEntity
import com.calisthenics.ai.domain.model.DifficultyLevel
import com.calisthenics.ai.domain.model.Exercise
import com.calisthenics.ai.domain.model.ExerciseCategory
import com.calisthenics.ai.domain.model.UserWorkoutPlan
import com.calisthenics.ai.domain.model.UserWorkoutPlanDay
import com.calisthenics.ai.domain.model.UserWorkoutPlanExercise
import com.calisthenics.ai.domain.model.WorkoutDayOfWeek
import com.calisthenics.ai.domain.model.WorkoutSessionType
import com.calisthenics.ai.domain.model.WorkoutSplitType

fun CachedWorkoutPlanEntity.toDomain(days: List<UserWorkoutPlanDay>): UserWorkoutPlan {
    val split = try {
        WorkoutSplitType.valueOf(splitType)
    } catch (_: Exception) {
        WorkoutSplitType.PPL
    }
    return UserWorkoutPlan(
        id = id,
        name = name,
        splitType = split,
        days = days
    )
}

fun CachedWorkoutPlanDayEntity.toDomain(exercises: List<UserWorkoutPlanExercise>): UserWorkoutPlanDay {
    val weekday = WorkoutDayOfWeek.fromName(dayOfWeek)
    val session = WorkoutSessionType.fromName(sessionType)
    return UserWorkoutPlanDay(
        id = id,
        dayOfWeek = weekday,
        sessionType = if (isRestDay || session.isRest) WorkoutSessionType.REST else session,
        name = name,
        description = description,
        isRestDay = isRestDay || session.isRest,
        exercises = exercises
    )
}

fun CachedWorkoutPlanExerciseEntity.toDomain(): UserWorkoutPlanExercise {
    val categoryEnum = try {
        ExerciseCategory.valueOf(category)
    } catch (_: Exception) {
        ExerciseCategory.UNKNOWN
    }
    val difficultyEnum = try {
        DifficultyLevel.valueOf(difficulty)
    } catch (_: Exception) {
        DifficultyLevel.BEGINNER
    }

    return UserWorkoutPlanExercise(
        id = id,
        exercise = Exercise(
            id = exerciseId,
            name = exerciseName,
            code = "",
            category = categoryEnum,
            difficulty = difficultyEnum,
            targetMuscles = muscleGroup,
            description = description,
            instructions = ""
        ),
        sets = sets,
        reps = reps,
        durationSeconds = durationSeconds,
        restSeconds = restSeconds,
        displayOrder = displayOrder,
        notes = notes
    )
}

fun UserWorkoutPlan.toEntity(
    isCoachAssigned: Boolean = false,
    coachName: String? = null,
    assignmentId: Long? = null,
    isActive: Boolean = false
): CachedWorkoutPlanEntity {
    return CachedWorkoutPlanEntity(
        id = id,
        name = name,
        splitType = splitType.name,
        isCoachAssigned = isCoachAssigned,
        coachName = coachName,
        assignmentId = assignmentId,
        isActive = isActive,
        updatedAt = System.currentTimeMillis()
    )
}

fun UserWorkoutPlanDay.toEntity(planId: Long): CachedWorkoutPlanDayEntity {
    return CachedWorkoutPlanDayEntity(
        id = id,
        planId = planId,
        dayOfWeek = dayOfWeek.name,
        sessionType = sessionType.name,
        name = name,
        description = description,
        isRestDay = isRestDay || sessionType.isRest
    )
}

fun UserWorkoutPlanExercise.toEntity(planId: Long, dayId: Long): CachedWorkoutPlanExerciseEntity {
    return CachedWorkoutPlanExerciseEntity(
        id = id,
        planId = planId,
        dayId = dayId,
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        category = exercise.category.name,
        difficulty = exercise.difficulty.name,
        muscleGroup = exercise.targetMuscles,
        description = exercise.description,
        sets = sets,
        reps = reps,
        durationSeconds = durationSeconds,
        restSeconds = restSeconds,
        displayOrder = displayOrder,
        notes = notes
    )
}
