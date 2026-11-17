package com.calisthenics.ai.domain.model

data class AiWorkoutPlanPreview(
    val planName: String,
    val description: String?,
    val durationWeeks: Int,
    val startDate: String?,
    val splitType: WorkoutSplitType,
    val sessions: List<AiWorkoutSessionPreview>,
    val metadata: AiWorkoutMetadata
) {
    val totalSessionsCount: Int
        get() = sessions.distinctBy { it.dayOfWeek }.size.coerceIn(0, 7)

    val trainingDaysPerWeek: Int
        get() = sessions
            .distinctBy { it.dayOfWeek }
            .count { !it.isRestDay }
            .coerceIn(0, 7)

    val restDaysPerWeek: Int
        get() = sessions
            .distinctBy { it.dayOfWeek }
            .count { it.isRestDay }
            .coerceIn(0, 7)

    val trainingSessionsCount: Int get() = trainingDaysPerWeek
    val restSessionsCount: Int get() = restDaysPerWeek
    val totalExerciseCount: Int
        get() = sessions
            .distinctBy { it.dayOfWeek }
            .sumOf { it.exercises.size }
}

data class AiWorkoutSessionPreview(
    val dayOfWeek: WorkoutDayOfWeek,
    val title: String,
    val isRestDay: Boolean,
    val exercises: List<AiWorkoutExercisePreview>
)

data class AiWorkoutExercisePreview(
    val exerciseId: Long?,
    val exerciseName: String,
    val exerciseCode: String?,
    val category: String?,
    val sets: Int,
    val reps: Int?,
    val holdSeconds: Int?,
    val restSeconds: Int,
    val notes: String?,
    val requiresReview: Boolean
) {
    val prescription: String
        get() = when {
            holdSeconds != null && holdSeconds > 0 -> "$sets sets × ${holdSeconds}s hold"
            reps != null && reps > 0 -> "$sets sets × $reps reps"
            else -> "$sets sets"
        }
}

data class AiWorkoutMetadata(
    val modelUsed: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val latencyMs: Long,
    val hasConflicts: Boolean,
    val existingActivePlanName: String?
)
