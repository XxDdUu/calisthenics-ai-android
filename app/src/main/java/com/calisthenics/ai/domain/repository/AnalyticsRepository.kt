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
    suspend fun sharePersonalRecord(
        prId: Long,
        recipientId: Long? = null,
        clientMessageId: String? = null
    ): Result<SharedPersonalRecordDto>
    suspend fun batchSharePRs(
        prIds: List<Long>,
        recipientId: Long? = null,
        clientMessageId: String? = null
    ): Result<List<SharedPersonalRecordDto>>
    suspend fun getClientSharedPRs(): Result<List<SharedPersonalRecordDto>>
    suspend fun getClientFeedbacks(): Result<List<SharedPersonalRecordDto>>
    suspend fun getCoachSharedPRs(unread: Boolean? = null): Result<List<SharedPersonalRecordDto>>
    suspend fun markPrShareAsRead(shareId: Long): Result<SharedPersonalRecordDto>
    suspend fun getCoachClientPRs(clientId: Long): Result<List<PersonalRecordDto>>
    suspend fun getCoachClientSharedPRs(clientId: Long): Result<List<SharedPersonalRecordDto>>
    suspend fun getCoachClientOverview(clientId: Long): Result<TrainingOverviewDto>
    suspend fun getCoachClientExerciseProgress(clientId: Long, exerciseId: Long, filter: String? = null): Result<ExerciseProgressDto>
    suspend fun addCoachFeedback(shareId: Long, feedback: String, rating: Int? = null): Result<SharedPersonalRecordDto>

    // Notifications
    val notificationUpdates: kotlinx.coroutines.flow.Flow<Unit>
        get() = kotlinx.coroutines.flow.emptyFlow()

    suspend fun getNotifications(unreadOnly: Boolean? = null): Result<List<NotificationDto>>
    suspend fun getNotifications(unreadOnly: Boolean? = null, page: Int?, size: Int?): Result<List<NotificationDto>> =
        getNotifications(unreadOnly)
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

internal object NoOpAnalyticsRepository : AnalyticsRepository {
    override suspend fun getTrainingOverview(): Result<TrainingOverviewDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getWeeklyAnalytics(): Result<WeeklyAnalyticsDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getMonthlyAnalytics(yearMonth: String?): Result<MonthlyAnalyticsDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getTrainingInsights(): Result<TrainingInsightsDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getExerciseHistory(exerciseId: Long): Result<List<ExerciseHistorySessionDto>> =
        Result.success(emptyList())
    override suspend fun getExerciseProgress(exerciseId: Long, filter: String?): Result<ExerciseProgressDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getExerciseRecords(exerciseId: Long): Result<List<PersonalRecordDto>> =
        Result.success(emptyList())
    override suspend fun getWorkoutSessionRecords(sessionId: Long): Result<List<PersonalRecordDto>> =
        Result.success(emptyList())
    override suspend fun sharePersonalRecord(prId: Long, recipientId: Long?, clientMessageId: String?): Result<SharedPersonalRecordDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun batchSharePRs(prIds: List<Long>, recipientId: Long?, clientMessageId: String?): Result<List<SharedPersonalRecordDto>> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getClientSharedPRs(): Result<List<SharedPersonalRecordDto>> = Result.success(emptyList())
    override suspend fun getClientFeedbacks(): Result<List<SharedPersonalRecordDto>> = Result.success(emptyList())
    override suspend fun getCoachSharedPRs(unread: Boolean?): Result<List<SharedPersonalRecordDto>> = Result.success(emptyList())
    override suspend fun markPrShareAsRead(shareId: Long): Result<SharedPersonalRecordDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getCoachClientPRs(clientId: Long): Result<List<PersonalRecordDto>> = Result.success(emptyList())
    override suspend fun getCoachClientSharedPRs(clientId: Long): Result<List<SharedPersonalRecordDto>> = Result.success(emptyList())
    override suspend fun getCoachClientOverview(clientId: Long): Result<TrainingOverviewDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getCoachClientExerciseProgress(clientId: Long, exerciseId: Long, filter: String?): Result<ExerciseProgressDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun addCoachFeedback(shareId: Long, feedback: String, rating: Int?): Result<SharedPersonalRecordDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getNotifications(unreadOnly: Boolean?): Result<List<NotificationDto>> = Result.success(emptyList())
    override suspend fun getUnreadNotificationsCount(): Result<Long> = Result.success(0L)
    override suspend fun markNotificationAsRead(id: Long): Result<Unit> = Result.success(Unit)
    override suspend fun markAllNotificationsAsRead(): Result<Unit> = Result.success(Unit)
    override suspend fun createPersonalRecord(request: CreatePrRequestDto): Result<PersonalRecordDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun getUserPersonalRecords(exerciseId: Long?, prType: String?, startDate: String?, endDate: String?): Result<List<PersonalRecordDto>> =
        Result.success(emptyList())
    override suspend fun getPrHistory(exerciseId: Long, prType: String?): Result<List<PersonalRecordDto>> = Result.success(emptyList())
    override suspend fun getLatestPr(exerciseId: Long, prType: String?): Result<PersonalRecordDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun updatePersonalRecord(id: Long, request: UpdatePrRequestDto): Result<PersonalRecordDto> =
        Result.failure(UnsupportedOperationException())
    override suspend fun deletePersonalRecord(id: Long): Result<Unit> = Result.success(Unit)
    override suspend fun getCoachClientPrRecords(clientId: Long, exerciseId: Long?, prType: String?): Result<List<PersonalRecordDto>> =
        Result.success(emptyList())
    override suspend fun getCoachClientPrHistory(clientId: Long, exerciseId: Long, prType: String?): Result<List<PersonalRecordDto>> =
        Result.success(emptyList())
}
