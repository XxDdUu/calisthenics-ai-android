package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.data.remote.dto.*

interface AnalyticsRepository {
    suspend fun getTrainingOverview(): Result<TrainingOverviewDto>
    suspend fun getWeeklyAnalytics(): Result<WeeklyAnalyticsDto>
    suspend fun getMonthlyAnalytics(yearMonth: String? = null): Result<MonthlyAnalyticsDto>
    suspend fun getTrainingInsights(): Result<TrainingInsightsDto>
    suspend fun getExerciseHistory(exerciseId: Long): Result<List<ExerciseHistorySessionDto>>
    suspend fun getExerciseProgress(exerciseId: Long, filter: String? = null): Result<ExerciseProgressDto>
    suspend fun getExerciseRecords(exerciseId: Long): Result<List<PersonalRecordDto>>
    suspend fun getWorkoutSessionRecords(sessionId: Long): Result<List<PersonalRecordDto>>
    suspend fun sharePersonalRecord(prId: Long): Result<SharedPersonalRecordDto>
    suspend fun batchSharePRs(prIds: List<Long>): Result<List<SharedPersonalRecordDto>>
    suspend fun getClientSharedPRs(): Result<List<SharedPersonalRecordDto>>
    suspend fun getClientFeedbacks(): Result<List<SharedPersonalRecordDto>>
    suspend fun getCoachSharedPRs(unread: Boolean? = null): Result<List<SharedPersonalRecordDto>>
    suspend fun markPrShareAsRead(shareId: Long): Result<SharedPersonalRecordDto>
    suspend fun getCoachClientPRs(clientId: Long): Result<List<PersonalRecordDto>>
    suspend fun getCoachClientOverview(clientId: Long): Result<TrainingOverviewDto>
    suspend fun getCoachClientExerciseProgress(clientId: Long, exerciseId: Long, filter: String? = null): Result<ExerciseProgressDto>
    suspend fun addCoachFeedback(shareId: Long, feedback: String, rating: Int? = null): Result<SharedPersonalRecordDto>

    // Notifications
    suspend fun getNotifications(unreadOnly: Boolean? = null): Result<List<NotificationDto>>
    suspend fun getUnreadNotificationsCount(): Result<Long>
    suspend fun markNotificationAsRead(id: Long): Result<Unit>
    suspend fun markAllNotificationsAsRead(): Result<Unit>

    // PR Tracking & Analytics
    suspend fun createPersonalRecord(request: CreatePrRequestDto): Result<PersonalRecordDto>
    suspend fun getUserPersonalRecords(exerciseId: Long? = null, prType: String? = null, startDate: String? = null, endDate: String? = null): Result<List<PersonalRecordDto>>
    suspend fun getPrHistory(exerciseId: Long, prType: String? = null): Result<List<PersonalRecordDto>>
    suspend fun getLatestPr(exerciseId: Long, prType: String? = null): Result<PersonalRecordDto>
    suspend fun updatePersonalRecord(id: Long, request: UpdatePrRequestDto): Result<PersonalRecordDto>
    suspend fun deletePersonalRecord(id: Long): Result<Unit>
    suspend fun getCoachClientPrRecords(clientId: Long, exerciseId: Long? = null, prType: String? = null): Result<List<PersonalRecordDto>>
    suspend fun getCoachClientPrHistory(clientId: Long, exerciseId: Long, prType: String? = null): Result<List<PersonalRecordDto>>
}
