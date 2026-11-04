package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PersonalRecordDto(
    val id: Long = 0L,
    val exerciseId: Long? = null,
    val exerciseName: String = "",
    @SerializedName("prType", alternate = ["recordType"])
    val recordType: String = "",
    @SerializedName("value", alternate = ["recordValue"])
    val recordValue: Double = 0.0,
    val unit: String? = null,
    @SerializedName("formattedValue", alternate = ["formattedRecord"])
    val formattedValue: String? = null,
    val formattedPrevious: String? = null,
    val improvementPercentage: Double? = null,
    val achievedAt: String = "",
    @SerializedName("workoutSessionId", alternate = ["sessionId"])
    val workoutSessionId: Long? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val displayFormattedValue: String
        get() = formattedValue?.takeIf { it.isNotBlank() }
            ?: run {
                val numStr = if (recordValue % 1.0 == 0.0) "${recordValue.toLong()}" else "$recordValue"
                val u = unit ?: when (recordType) {
                    "MAX_WEIGHT" -> "kg"
                    "MAX_REPS", "BODYWEIGHT" -> "reps"
                    "MAX_HOLD", "MAX_DURATION" -> "seconds"
                    "MAX_DISTANCE" -> "km"
                    else -> ""
                }
                when (recordType) {
                    "MAX_WEIGHT" -> "+$numStr $u"
                    "MAX_REPS", "BODYWEIGHT" -> "$numStr $u"
                    "MAX_HOLD" -> "$numStr $u hold"
                    else -> if (u.isNotBlank()) "$numStr $u" else numStr
                }
            }
}

data class CreatePrRequestDto(
    val exerciseId: Long? = null,
    val exerciseName: String? = null,
    @SerializedName("prType", alternate = ["recordType"])
    val prType: String,
    @SerializedName("value", alternate = ["recordValue"])
    val value: Double,
    val unit: String,
    val achievedAt: String,
    val notes: String? = null
)

data class UpdatePrRequestDto(
    @SerializedName("prType", alternate = ["recordType"])
    val prType: String? = null,
    @SerializedName("value", alternate = ["recordValue"])
    val value: Double? = null,
    val unit: String? = null,
    val achievedAt: String? = null,
    val notes: String? = null
)

data class SharedPersonalRecordDto(
    val id: Long = 0L,
    val personalRecord: PersonalRecordDto = PersonalRecordDto(),
    @SerializedName("clientUserId", alternate = ["clientId"])
    val clientUserId: Long = 0L,
    val clientName: String = "",
    @SerializedName("coachUserId", alternate = ["coachId"])
    val coachUserId: Long = 0L,
    val coachName: String = "",
    val sharedAt: String = "",
    val coachFeedback: String? = null,
    val feedbackAt: String? = null
)

data class CoachFeedbackRequestDto(
    val feedback: String
)

data class ProgressChartDataPointDto(
    val date: String = "",
    val value: Double = 0.0,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val totalVolume: Double? = null,
    val isPersonalRecord: Boolean = false,
    val sessionId: Long? = null
)

data class ProgressiveOverloadComparisonDto(
    val metricName: String = "",
    val previousValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val difference: Double = 0.0,
    val percentageChange: Double = 0.0,
    val improved: Boolean = false
)

data class ExerciseHistorySetDto(
    val setNumber: Int = 1,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val completed: Boolean = true,
    val isPr: Boolean = false
)

data class ExerciseHistorySessionDto(
    val sessionId: Long = 0L,
    val sessionTitle: String = "",
    val workoutDate: String = "",
    val completedAt: String? = null,
    val totalVolume: Double = 0.0,
    val sets: List<ExerciseHistorySetDto> = emptyList()
)

data class ExerciseProgressDto(
    val exerciseId: Long = 0L,
    val exerciseName: String = "",
    @SerializedName("exerciseCategory", alternate = ["category"])
    val exerciseCategory: String = "",
    @SerializedName("totalSessionsCompleted", alternate = ["totalCompletedSessions"])
    val totalSessionsCompleted: Long = 0,
    @SerializedName("totalSetsCompleted", alternate = ["totalSets"])
    val totalSetsCompleted: Long = 0,
    @SerializedName("totalRepsCompleted", alternate = ["totalReps"])
    val totalRepsCompleted: Long = 0,
    val maxWeightKg: Double? = null,
    val maxReps: Int? = null,
    val maxHoldDurationSeconds: Int? = null,
    @SerializedName("bestSetSummary", alternate = ["bestSet"])
    val bestSetSummary: String? = null,
    val lastPerformedDate: String? = null,
    @SerializedName("overloadComparison", alternate = ["latestVsPrevious"])
    val overloadComparison: ProgressiveOverloadComparisonDto? = null,
    val chartData: List<ProgressChartDataPointDto> = emptyList(),
    @SerializedName("history", alternate = ["sessions"])
    val history: List<ExerciseHistorySessionDto> = emptyList()
)

data class WeeklyAnalyticsDto(
    val startDate: String = "",
    val endDate: String = "",
    @SerializedName("totalWorkouts", alternate = ["currentWeekWorkouts"])
    val totalWorkouts: Long = 0,
    @SerializedName("totalSets", alternate = ["currentWeekSets"])
    val totalSets: Long = 0,
    @SerializedName("totalReps", alternate = ["currentWeekReps"])
    val totalReps: Long = 0,
    @SerializedName("totalVolumeKg", alternate = ["currentWeekVolume"])
    val totalVolumeKg: Double = 0.0,
    @SerializedName("totalDurationSeconds", alternate = ["currentWeekDurationSeconds"])
    val totalDurationSeconds: Long = 0,
    val activeDays: Int = 0,
    val comparisonVsPreviousWeek: Map<String, Double> = emptyMap(),
    @SerializedName("workoutsPerDay", alternate = ["dailyWorkouts"])
    val workoutsPerDay: Map<String, Long> = emptyMap()
)

data class MonthlyAnalyticsDto(
    val yearMonth: String = "",
    @SerializedName("totalWorkouts", alternate = ["completedWorkouts"])
    val totalWorkouts: Long = 0,
    val totalSets: Long = 0,
    val totalReps: Long = 0,
    @SerializedName("totalVolumeKg", alternate = ["totalVolume"])
    val totalVolumeKg: Double = 0.0,
    val activeDays: Int = 0,
    val weeklyBreakdown: Map<String, Long> = emptyMap()
)

data class TrainingInsightsDto(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val weeklyFrequency: Double = 0.0,
    val activeDaysCount: Int = 0,
    val insights: List<String> = emptyList(),
    val recentPRs: List<PersonalRecordDto> = emptyList()
)

data class TrainingOverviewDto(
    @SerializedName("totalWorkoutsCompleted", alternate = ["totalWorkouts"])
    val totalWorkoutsCompleted: Long = 0,
    @SerializedName("currentStreakDays", alternate = ["currentStreak"])
    val currentStreakDays: Int = 0,
    @SerializedName("longestStreakDays", alternate = ["longestStreak"])
    val longestStreakDays: Int = 0,
    @SerializedName("activeDaysThisMonth", alternate = ["totalActiveDays"])
    val activeDaysThisMonth: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalExercisesTracked: Long = 0,
    @SerializedName("totalPersonalRecords", alternate = ["totalPRs"])
    val totalPersonalRecords: Long = 0,
    val recentPRs: List<PersonalRecordDto> = emptyList()
)
