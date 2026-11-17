package com.calisthenics.ai.domain.model

enum class WorkoutPlanAssignmentStatus {
    UPCOMING,
    ACTIVE,
    INACTIVE,
    CANCELLED,
    COMPLETED,
    REVOKED,
    ENDED;

    val isCancelledOrEnded: Boolean
        get() = this == CANCELLED || this == ENDED || this == REVOKED

    companion object {
        fun fromApi(value: String?): WorkoutPlanAssignmentStatus {
            val normalized = value?.trim()?.uppercase()
            if (normalized.isNullOrEmpty()) return ACTIVE
            if (normalized == "CANCELED") return CANCELLED
            return entries.firstOrNull { it.name == normalized } ?: ENDED
        }
    }
}

data class WorkoutPlanAssignment(
    val id: Long,
    val coachName: String?,
    val startDate: String?,
    val createdAt: String?,
    val workoutPlan: UserWorkoutPlan,
    val status: WorkoutPlanAssignmentStatus = WorkoutPlanAssignmentStatus.ACTIVE,
    val coachId: Long? = null,
    val clientId: Long? = null,
    val clientName: String? = null,
    val relationshipId: Long? = null,
    val updatedAt: String? = null,
    val endDate: String? = null
)
