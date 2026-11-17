package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EnrollGettingStartedRequestDto(
    @SerializedName("addToWorkoutPlan") val addToWorkoutPlan: Boolean = false,
    @SerializedName("confirmConflicts") val confirmConflicts: Boolean = false,
    @SerializedName("startDate") val startDate: String? = null
)

data class GettingStartedScheduleConflictDto(
    @SerializedName("date") val date: String,
    @SerializedName("gettingStartedDayNumber") val gettingStartedDayNumber: Int,
    @SerializedName("gettingStartedDayTitle") val gettingStartedDayTitle: String? = null,
    @SerializedName("existingWorkoutTitle") val existingWorkoutTitle: String? = null,
    @SerializedName("existingSource") val existingSource: String? = null,
    @SerializedName("workoutSessionId") val workoutSessionId: Long? = null,
    @SerializedName("workoutPlanId") val workoutPlanId: Long? = null
)

data class GettingStartedEnrollPreviewDto(
    @SerializedName("programId") val programId: Long,
    @SerializedName("programTitle") val programTitle: String? = null,
    @SerializedName("alreadyEnrolled") val alreadyEnrolled: Boolean = false,
    @SerializedName("alreadyAddedToWorkoutPlan") val alreadyAddedToWorkoutPlan: Boolean = false,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("plannedDates") val plannedDates: List<String> = emptyList(),
    @SerializedName("hasConflicts") val hasConflicts: Boolean = false,
    @SerializedName("conflicts") val conflicts: List<GettingStartedScheduleConflictDto> = emptyList()
)

data class GettingStartedDayProgressDto(
    @SerializedName("dayNumber") val dayNumber: Int,
    @SerializedName("title") val title: String? = null,
    @SerializedName("status") val status: String = "NOT_STARTED",
    @SerializedName("scheduledDate") val scheduledDate: String? = null,
    @SerializedName("workoutSessionId") val workoutSessionId: Long? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null
)

data class GettingStartedExerciseResponseDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("sets") val sets: Int = 3,
    @SerializedName("reps") val reps: Int? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("restSeconds") val restSeconds: Int = 60,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("displayOrder") val displayOrder: Int = 0
)

data class GettingStartedDayResponseDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("dayNumber") val dayNumber: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("exercises") val exercises: List<GettingStartedExerciseResponseDto> = emptyList(),
    @SerializedName("status") val status: String = "NOT_STARTED",
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("scheduledDate") val scheduledDate: String? = null,
    @SerializedName("workoutSessionId") val workoutSessionId: Long? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null
)

data class GettingStartedEnrollmentResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("programId") val programId: Long,
    @SerializedName("programTitle") val programTitle: String? = null,
    @SerializedName("programDescription") val programDescription: String? = null,
    @SerializedName("programVersion") val programVersion: Int = 1,
    @SerializedName("status") val status: String = "NOT_STARTED",
    @SerializedName("addedToWorkoutPlan") val addedToWorkoutPlan: Boolean = false,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("enrolledAt") val enrolledAt: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("completedDays") val completedDays: List<Int> = emptyList(),
    @SerializedName("dayProgress") val dayProgress: List<GettingStartedDayProgressDto> = emptyList(),
    @SerializedName("completedDaysCount") val completedDaysCount: Int = 0,
    @SerializedName("totalDays") val totalDays: Int = 3,
    @SerializedName("isCompleted") val isCompleted: Boolean = false
)

data class GettingStartedProgramResponseDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("targetAudience") val targetAudience: String? = "ALL_ATHLETES",
    @SerializedName("published") val published: Boolean = false,
    @SerializedName("version") val version: Int = 1,
    @SerializedName("creatorId") val creatorId: Long? = null,
    @SerializedName("creatorName") val creatorName: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("publishedAt") val publishedAt: String? = null,
    @SerializedName("eligible") val eligible: Boolean = true,
    @SerializedName("userDismissalState") val userDismissalState: String? = null,
    @SerializedName("days") val days: List<GettingStartedDayResponseDto> = emptyList(),
    @SerializedName("enrollment") val enrollment: GettingStartedEnrollmentResponseDto? = null
)

data class UpdateDismissalStatusRequestDto(
    @SerializedName("status") val status: String
)

data class GettingStartedCampaignEventRequestDto(
    @SerializedName("eventType") val eventType: String,
    @SerializedName("programId") val programId: Long? = null
)

data class AdminGettingStartedExerciseRequestDto(
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("exerciseName") val exerciseName: String,
    @SerializedName("sets") val sets: Int = 3,
    @SerializedName("reps") val reps: Int? = null,
    @SerializedName("durationSeconds") val durationSeconds: Int? = null,
    @SerializedName("restSeconds") val restSeconds: Int = 60,
    @SerializedName("instructions") val instructions: String? = null,
    @SerializedName("displayOrder") val displayOrder: Int = 0
)

data class AdminGettingStartedDayRequestDto(
    @SerializedName("dayNumber") val dayNumber: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("exercises") val exercises: List<AdminGettingStartedExerciseRequestDto> = emptyList()
)

data class AdminGettingStartedProgramRequestDto(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("targetAudience") val targetAudience: String = "ALL_ATHLETES",
    @SerializedName("days") val days: List<AdminGettingStartedDayRequestDto> = emptyList()
)
