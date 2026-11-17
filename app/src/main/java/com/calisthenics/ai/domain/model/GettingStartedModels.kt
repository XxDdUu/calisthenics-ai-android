package com.calisthenics.ai.domain.model

enum class TargetAudience {
    LAST_14_DAYS,
    LAST_30_DAYS,
    ALL_ATHLETES;

    companion object {
        fun fromString(value: String?): TargetAudience {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ALL_ATHLETES
        }
    }
}

enum class GettingStartedDayStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED;

    companion object {
        fun fromString(value: String?, fallbackCompleted: Boolean = false): GettingStartedDayStatus {
            val matched = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            if (matched != null) return matched
            return if (fallbackCompleted) COMPLETED else NOT_STARTED
        }
    }
}

data class GettingStartedScheduleConflict(
    val date: String,
    val gettingStartedDayNumber: Int,
    val gettingStartedDayTitle: String,
    val existingWorkoutTitle: String,
    val existingSource: String,
    val workoutSessionId: Long? = null,
    val workoutPlanId: Long? = null
)

data class GettingStartedEnrollPreview(
    val programId: Long,
    val programTitle: String?,
    val alreadyEnrolled: Boolean,
    val alreadyAddedToWorkoutPlan: Boolean,
    val startDate: String,
    val plannedDates: List<String> = emptyList(),
    val hasConflicts: Boolean = false,
    val conflicts: List<GettingStartedScheduleConflict> = emptyList()
)

class GettingStartedScheduleConflictException(
    override val message: String,
    val conflicts: List<GettingStartedScheduleConflict>
) : RuntimeException(message)

data class GettingStartedDayProgress(
    val dayNumber: Int,
    val title: String,
    val status: GettingStartedDayStatus,
    val scheduledDate: String? = null,
    val workoutSessionId: Long? = null,
    val startedAt: String? = null,
    val completedAt: String? = null
)

data class GettingStartedExercise(
    val id: Long? = null,
    val exerciseId: Long? = null,
    val exerciseName: String,
    val sets: Int = 3,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val restSeconds: Int = 60,
    val instructions: String? = null,
    val displayOrder: Int = 0
)

data class GettingStartedDay(
    val id: Long? = null,
    val dayNumber: Int,
    val title: String,
    val description: String? = null,
    val exercises: List<GettingStartedExercise> = emptyList(),
    val status: GettingStartedDayStatus = GettingStartedDayStatus.NOT_STARTED,
    val completed: Boolean = status == GettingStartedDayStatus.COMPLETED,
    val scheduledDate: String? = null,
    val workoutSessionId: Long? = null,
    val startedAt: String? = null,
    val completedAt: String? = null
)

data class GettingStartedEnrollment(
    val id: Long,
    val programId: Long,
    val programTitle: String? = null,
    val programDescription: String? = null,
    val programVersion: Int = 1,
    val status: String = "NOT_STARTED",
    val addedToWorkoutPlan: Boolean = false,
    val startDate: String? = null,
    val enrolledAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val completedDays: List<Int> = emptyList(),
    val dayProgress: List<GettingStartedDayProgress> = emptyList(),
    val completedDaysCount: Int = 0,
    val totalDays: Int = 3,
    val isCompleted: Boolean = status.equals("COMPLETED", ignoreCase = true)
) {
    val isNotStarted: Boolean
        get() = !isCompleted && (status.equals("NOT_STARTED", ignoreCase = true) ||
            (status.equals("ACTIVE", ignoreCase = true) && dayProgress.none { it.status != GettingStartedDayStatus.NOT_STARTED }))

    val isInProgress: Boolean
        get() = !isCompleted && (status.equals("IN_PROGRESS", ignoreCase = true) ||
            dayProgress.any { it.status != GettingStartedDayStatus.NOT_STARTED })
}

data class GettingStartedProgram(
    val id: Long? = null,
    val title: String,
    val description: String? = null,
    val targetAudience: TargetAudience = TargetAudience.ALL_ATHLETES,
    val published: Boolean = false,
    val version: Int = 1,
    val creatorId: Long? = null,
    val creatorName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val publishedAt: String? = null,
    val eligible: Boolean = true,
    val userDismissalState: OnboardingStatus = OnboardingStatus.NOT_SHOWN,
    val days: List<GettingStartedDay> = emptyList(),
    val enrollment: GettingStartedEnrollment? = null
)

data class AdminExerciseDraft(
    val exerciseId: Long? = null,
    val exerciseName: String,
    val sets: Int = 3,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val restSeconds: Int = 60,
    val instructions: String = "",
    val displayOrder: Int = 0
)

data class AdminDayDraft(
    val dayNumber: Int,
    val title: String,
    val description: String = "",
    val exercises: List<AdminExerciseDraft> = emptyList()
)

data class AdminProgramDraft(
    val title: String,
    val description: String = "",
    val targetAudience: TargetAudience = TargetAudience.ALL_ATHLETES,
    val days: List<AdminDayDraft>
)

typealias AdminProgramExerciseDraft = AdminExerciseDraft
typealias AdminProgramDayDraft = AdminDayDraft
