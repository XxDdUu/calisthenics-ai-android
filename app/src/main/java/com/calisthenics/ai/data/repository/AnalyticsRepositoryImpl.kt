package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.*
import com.calisthenics.ai.domain.repository.AnalyticsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val api: CalisthenicsApi
) : AnalyticsRepository {

    override suspend fun getTrainingOverview(): Result<TrainingOverviewDto> = runCatching {
        api.getTrainingOverview()
    }

    override suspend fun getWeeklyAnalytics(): Result<WeeklyAnalyticsDto> = runCatching {
        api.getWeeklyAnalytics()
    }

    override suspend fun getMonthlyAnalytics(yearMonth: String?): Result<MonthlyAnalyticsDto> = runCatching {
        api.getMonthlyAnalytics(yearMonth)
    }

    override suspend fun getTrainingInsights(): Result<TrainingInsightsDto> = runCatching {
        api.getTrainingInsights()
    }

    override suspend fun getExerciseHistory(exerciseId: Long): Result<List<ExerciseHistorySessionDto>> = runCatching {
        api.getExerciseHistory(exerciseId)
    }

    override suspend fun getExerciseProgress(exerciseId: Long, filter: String?): Result<ExerciseProgressDto> = runCatching {
        api.getExerciseProgress(exerciseId, filter)
    }

    override suspend fun getExerciseRecords(exerciseId: Long): Result<List<PersonalRecordDto>> = runCatching {
        api.getExerciseRecords(exerciseId)
    }

    override suspend fun getWorkoutSessionRecords(sessionId: Long): Result<List<PersonalRecordDto>> = runCatching {
        api.getWorkoutSessionRecords(sessionId)
    }

    override suspend fun sharePersonalRecord(prId: Long): Result<SharedPersonalRecordDto> = runCatching {
        api.sharePersonalRecord(prId)
    }

    override suspend fun batchSharePRs(prIds: List<Long>): Result<List<SharedPersonalRecordDto>> = runCatching {
        api.batchSharePRs(BatchSharePrRequestDto(prIds))
    }

    override suspend fun getClientSharedPRs(): Result<List<SharedPersonalRecordDto>> = runCatching {
        api.getClientSharedPRs()
    }

    override suspend fun getClientFeedbacks(): Result<List<SharedPersonalRecordDto>> = runCatching {
        api.getClientFeedbacks()
    }

    override suspend fun getCoachSharedPRs(unread: Boolean?): Result<List<SharedPersonalRecordDto>> = runCatching {
        api.getCoachSharedPRs(unread)
    }

    override suspend fun markPrShareAsRead(shareId: Long): Result<SharedPersonalRecordDto> = runCatching {
        api.markPrShareAsRead(shareId)
    }

    override suspend fun getCoachClientPRs(clientId: Long): Result<List<PersonalRecordDto>> = runCatching {
        api.getCoachClientPRs(clientId)
    }

    override suspend fun getCoachClientOverview(clientId: Long): Result<TrainingOverviewDto> = runCatching {
        api.getCoachClientOverview(clientId)
    }

    override suspend fun getCoachClientExerciseProgress(
        clientId: Long,
        exerciseId: Long,
        filter: String?
    ): Result<ExerciseProgressDto> = runCatching {
        api.getCoachClientExerciseProgress(clientId, exerciseId, filter)
    }

    override suspend fun addCoachFeedback(shareId: Long, feedback: String, rating: Int?): Result<SharedPersonalRecordDto> = runCatching {
        api.addCoachFeedback(shareId, CoachFeedbackRequestDto(message = feedback, rating = rating))
    }

    override suspend fun getNotifications(unreadOnly: Boolean?): Result<List<NotificationDto>> = runCatching {
        api.getNotifications(unreadOnly)
    }

    override suspend fun getUnreadNotificationsCount(): Result<Long> = runCatching {
        api.getUnreadNotificationsCount().unreadCount
    }

    override suspend fun markNotificationAsRead(id: Long): Result<Unit> = runCatching {
        api.markNotificationAsRead(id)
    }

    override suspend fun markAllNotificationsAsRead(): Result<Unit> = runCatching {
        api.markAllNotificationsAsRead()
    }

    // PR Tracking & Analytics
    override suspend fun createPersonalRecord(request: CreatePrRequestDto): Result<PersonalRecordDto> = runCatching {
        api.createPersonalRecord(request)
    }

    override suspend fun getUserPersonalRecords(
        exerciseId: Long?,
        prType: String?,
        startDate: String?,
        endDate: String?
    ): Result<List<PersonalRecordDto>> = runCatching {
        api.getUserPersonalRecords(exerciseId, prType, startDate, endDate)
    }

    override suspend fun getPrHistory(exerciseId: Long, prType: String?): Result<List<PersonalRecordDto>> = runCatching {
        api.getPrHistory(exerciseId, prType)
    }

    override suspend fun getLatestPr(exerciseId: Long, prType: String?): Result<PersonalRecordDto> = runCatching {
        api.getLatestPr(exerciseId, prType)
    }

    override suspend fun updatePersonalRecord(id: Long, request: UpdatePrRequestDto): Result<PersonalRecordDto> = runCatching {
        api.updatePersonalRecord(id, request)
    }

    override suspend fun deletePersonalRecord(id: Long): Result<Unit> = runCatching {
        api.deletePersonalRecord(id)
    }

    override suspend fun getCoachClientPrRecords(
        clientId: Long,
        exerciseId: Long?,
        prType: String?
    ): Result<List<PersonalRecordDto>> = runCatching {
        api.getCoachClientPrRecords(clientId, exerciseId, prType)
    }

    override suspend fun getCoachClientPrHistory(
        clientId: Long,
        exerciseId: Long,
        prType: String?
    ): Result<List<PersonalRecordDto>> = runCatching {
        api.getCoachClientPrHistory(clientId, exerciseId, prType)
    }
}
