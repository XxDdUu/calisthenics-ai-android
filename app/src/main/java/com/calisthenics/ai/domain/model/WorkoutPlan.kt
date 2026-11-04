package com.calisthenics.ai.domain.model

import kotlinx.serialization.Serializable

/**
 * Legacy locally persisted weekly-plan model used by the original device-only plan editor and
 * session flow. Server-backed plan management uses [UserWorkoutPlan].
 */
@Serializable
data class WorkoutPlan(
    val id: String,
    val name: String,
    val workouts: List<PlannedWorkout> = emptyList()
)

@Serializable
data class PlannedWorkout(
    val id: String,
    /** ISO day-of-week value: Monday = 1 … Sunday = 7. */
    val weekday: Int,
    val name: String,
    val category: String,
    val estimatedDurationMinutes: Int,
    val notes: String = "",
    val exercises: List<PlannedExercise> = emptyList()
)

@Serializable
data class PlannedExercise(
    val id: String,
    val exerciseId: Long? = null,
    val name: String,
    val category: String = "",
    val sets: Int = 3,
    val repetitions: Int? = 8,
    val holdDurationSeconds: Int? = null,
    val restSeconds: Int = 60,
    val notes: String = ""
) {
    val prescription: String
        get() = if (holdDurationSeconds != null) {
            "$sets sets × $holdDurationSeconds sec"
        } else {
            "$sets sets × ${repetitions ?: 0} reps"
        }
}

@Serializable
data class WorkoutSessionProgress(
    val workoutId: String,
    /** ISO-8601 local date, evaluated in the device's current local date. */
    val date: String,
    val completedExerciseIds: Set<String> = emptySet(),
    val isWorkoutCompleted: Boolean = false
)

data class WorkoutPlanSnapshot(
    val plan: WorkoutPlan?,
    val sessionProgress: List<WorkoutSessionProgress>
)
