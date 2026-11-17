package com.calisthenics.ai.domain.model

import java.time.LocalDate

enum class WorkoutSessionClassification {
    UPCOMING,
    UNFINISHED,
    COMPLETED,
    CANCELLED
}

data class WorkoutSession(
    val id: Long,
    val workoutPlanId: Long?,
    val workoutPlanDayId: Long?,
    val assignmentId: Long?,
    val source: String,
    val title: String,
    val workoutDate: String?,
    val status: String,
    val startedAt: String?,
    val lastSavedAt: String?,
    val completedAt: String?,
    val durationSeconds: Long?,
    val completedExercises: Int,
    val skippedExercises: Int,
    val unfinishedExercises: Int,
    val completedSets: Int,
    val totalReps: Int,
    val totalHoldDurationSeconds: Int,
    val exercises: List<WorkoutSessionExercise>,
    val classification: String? = null,
    val gettingStartedEnrollmentId: Long? = null,
    val gettingStartedDayNumber: Int? = null
) {
    fun effectiveClassification(today: LocalDate = LocalDate.now()): WorkoutSessionClassification {
        classification?.let { raw ->
            runCatching { return WorkoutSessionClassification.valueOf(raw.uppercase()) }
        }
        return when (status.uppercase()) {
            "COMPLETED" -> WorkoutSessionClassification.COMPLETED
            "CANCELLED" -> WorkoutSessionClassification.CANCELLED
            else -> {
                val parsedDate = workoutDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (parsedDate != null && parsedDate.isAfter(today)) {
                    WorkoutSessionClassification.UPCOMING
                } else {
                    WorkoutSessionClassification.UNFINISHED
                }
            }
        }
    }

    fun updateSet(
        exerciseId: Long,
        setId: Long,
        update: WorkoutSessionSetUpdate
    ): WorkoutSession? {
        if (status != "IN_PROGRESS") return null
        var found = false
        val updatedExercises = exercises.map { exercise ->
            if (exercise.id != exerciseId) exercise else {
                val updatedSets = exercise.sets.map { set ->
                    if (set.id != setId) set else {
                        found = true
                        set.copy(
                            reps = update.reps,
                            holdDurationSeconds = update.holdDurationSeconds,
                            weightKg = update.weightKg,
                            assistanceWeightKg = update.assistanceWeightKg,
                            completed = update.completed,
                            completedAt = if (update.completed) java.time.Instant.now().toString() else null,
                            notes = update.notes
                        )
                    }
                }
                val allCompleted = updatedSets.isNotEmpty() && updatedSets.all { it.completed }
                exercise.copy(
                    sets = updatedSets,
                    status = if (allCompleted) "COMPLETED" else "IN_PROGRESS"
                )
            }
        }
        if (!found) return null
        val completed = updatedExercises.count { it.status == "COMPLETED" }
        val skipped = updatedExercises.count { it.status == "SKIPPED" }
        val unfinished = updatedExercises.count {
            it.status == "IN_PROGRESS" || it.status == "INCOMPLETE"
        }
        val totalCompletedSets = updatedExercises.sumOf { ex -> ex.sets.count { it.completed } }
        val totalRepsCount = updatedExercises.sumOf { ex -> ex.sets.filter { it.completed }.sumOf { it.reps ?: 0 } }
        val totalHoldSeconds = updatedExercises.sumOf { ex -> ex.sets.filter { it.completed }.sumOf { it.holdDurationSeconds ?: 0 } }
        return copy(
            exercises = updatedExercises,
            completedExercises = completed,
            skippedExercises = skipped,
            unfinishedExercises = unfinished,
            completedSets = totalCompletedSets,
            totalReps = totalRepsCount,
            totalHoldDurationSeconds = totalHoldSeconds
        )
    }

    fun updateExerciseStatus(exerciseId: Long, newStatus: String): WorkoutSession? {
        if (status != "IN_PROGRESS") return null
        val updated = exercises.map { if (it.id == exerciseId) it.copy(status = newStatus) else it }
        if (updated == exercises) return null
        return copy(
            exercises = updated,
            completedExercises = updated.count { it.status == "COMPLETED" },
            skippedExercises = updated.count { it.status == "SKIPPED" },
            unfinishedExercises = updated.count { it.status == "IN_PROGRESS" || it.status == "INCOMPLETE" }
        )
    }
}

data class WorkoutSessionExercise(
    val id: Long,
    val version: Long,
    val exerciseId: Long?,
    val exerciseName: String,
    val instructions: String?,
    val notes: String?,
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int?,
    val targetHoldDurationSeconds: Int?,
    val restSeconds: Int,
    val status: String,
    val sets: List<WorkoutSessionSet>
)

data class WorkoutSessionSet(
    val id: Long,
    val version: Long,
    val setNumber: Int,
    val reps: Int?,
    val holdDurationSeconds: Int?,
    val weightKg: Double?,
    val assistanceWeightKg: Double?,
    val completed: Boolean,
    val completedAt: String?,
    val notes: String?
)

data class WorkoutSessionSetUpdate(
    val reps: Int?,
    val holdDurationSeconds: Int?,
    val weightKg: Double?,
    val assistanceWeightKg: Double?,
    val completed: Boolean,
    val notes: String?,
    val expectedVersion: Long = 0L
)

data class WorkoutSessionHistoryPage(
    val content: List<WorkoutSession>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
