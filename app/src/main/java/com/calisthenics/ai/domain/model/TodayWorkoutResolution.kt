package com.calisthenics.ai.domain.model

enum class WorkoutSource {
    COACH,
    PERSONAL,
    NONE
}

data class TodayWorkoutCandidate(
    val plan: UserWorkoutPlan,
    val assignmentId: Long?,
    val coachName: String?
)

data class TodayWorkoutResolution(
    val workoutDate: String,
    val activeCoach: Boolean,
    val source: WorkoutSource,
    val selectionRequired: Boolean,
    val selectedPlanId: Long?,
    val candidates: List<TodayWorkoutCandidate>
)
