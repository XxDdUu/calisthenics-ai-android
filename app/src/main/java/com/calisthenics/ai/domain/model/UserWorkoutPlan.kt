package com.calisthenics.ai.domain.model

enum class WorkoutSplitType(val displayName: String) {
    PPL("Push / Pull / Legs"),
    UPPER_LOWER("Upper / Lower"),
    FULL_BODY("Full Body")
}

enum class WorkoutDayOfWeek(val order: Int, val displayName: String, val shortName: String) {
    MONDAY(1, "Monday", "Mon"),
    TUESDAY(2, "Tuesday", "Tue"),
    WEDNESDAY(3, "Wednesday", "Wed"),
    THURSDAY(4, "Thursday", "Thu"),
    FRIDAY(5, "Friday", "Fri"),
    SATURDAY(6, "Saturday", "Sat"),
    SUNDAY(7, "Sunday", "Sun");

    companion object {
        fun fromOrder(order: Int): WorkoutDayOfWeek = entries.firstOrNull { it.order == order } ?: MONDAY
        fun fromName(name: String): WorkoutDayOfWeek = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MONDAY
    }
}

enum class WorkoutSessionType(val displayName: String) {
    PUSH("Push"),
    PULL("Pull"),
    LEGS("Legs"),
    UPPER("Upper"),
    LOWER("Lower"),
    FULL_BODY("Full Body"),
    REST("Rest");

    val isRest: Boolean get() = this == REST

    companion object {
        fun fromName(name: String): WorkoutSessionType = entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: REST
    }
}

data class UserWorkoutPlan(
    val id: Long,
    val name: String,
    val splitType: WorkoutSplitType,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val days: List<UserWorkoutPlanDay> = emptyList()
) {
    val trainingDayCount: Int
        get() = days.count { !it.sessionType.isRest }

    val totalExerciseCount: Int
        get() = days.sumOf { it.exercises.size }
}

data class UserWorkoutPlanDay(
    val id: Long,
    val dayOfWeek: WorkoutDayOfWeek,
    val sessionType: WorkoutSessionType,
    val name: String? = null,
    val exercises: List<UserWorkoutPlanExercise> = emptyList()
)

data class UserWorkoutPlanExercise(
    val id: Long,
    val exercise: Exercise,
    val sets: Int,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val restSeconds: Int = 60,
    val displayOrder: Int = 1,
    val notes: String? = null
) {
    val prescription: String
        get() = when {
            durationSeconds != null && durationSeconds > 0 -> "$sets sets × ${durationSeconds}s hold"
            reps != null && reps > 0 -> "$sets sets × $reps reps"
            else -> "$sets sets"
        }
}

data class UserWorkoutPlanTemplate(
    val splitType: WorkoutSplitType,
    val days: List<UserWorkoutPlanDay>
)
