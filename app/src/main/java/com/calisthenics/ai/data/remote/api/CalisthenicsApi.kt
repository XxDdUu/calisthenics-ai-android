package com.calisthenics.ai.data.remote.api

import com.calisthenics.ai.data.remote.dto.AnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.AdminDashboardDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDeleteResponseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDto
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.AdminUserRoleDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDto
import com.calisthenics.ai.data.remote.dto.CreateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateExerciseStatusRequestDto
import com.calisthenics.ai.data.remote.dto.AuthResponseDto
import com.calisthenics.ai.data.remote.dto.AvatarUploadResponseDto
import com.calisthenics.ai.data.remote.dto.ClientProfileDto
import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.ClientTrainingOverviewDto
import com.calisthenics.ai.data.remote.dto.CoachClientRelationshipDto
import com.calisthenics.ai.data.remote.dto.CoachProfileDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CreateClientCoachRequestDto
import com.calisthenics.ai.data.remote.dto.CreateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.FileUploadResponseDto
import com.calisthenics.ai.data.remote.dto.GoogleLoginRequestDto
import com.calisthenics.ai.data.remote.dto.LoginRequestDto
import com.calisthenics.ai.data.remote.dto.LiveAnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.LiveExerciseCatalogDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordResponseDto
import com.calisthenics.ai.data.remote.dto.RegisterRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterInitiationResponseDto
import com.calisthenics.ai.data.remote.dto.VerifyRegistrationOtpRequestDto
import com.calisthenics.ai.data.remote.dto.ResendRegistrationOtpRequestDto
import com.calisthenics.ai.data.remote.dto.ResetPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.ResetPasswordResponseDto
import com.calisthenics.ai.data.remote.dto.VerifyOtpRequestDto
import com.calisthenics.ai.data.remote.dto.VerifyOtpResponseDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.UserProfileResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutDayExercisesBatchRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanTemplateResponseDto
import com.calisthenics.ai.data.remote.dto.TodayWorkoutResolutionResponseDto
import com.calisthenics.ai.data.remote.dto.StartWorkoutSessionRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutSessionExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutSessionSetRequestDto
import com.calisthenics.ai.data.remote.dto.FinishWorkoutSessionRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutSessionPageResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutSessionResponseDto
import com.calisthenics.ai.data.remote.dto.PersonalRecordDto
import com.calisthenics.ai.data.remote.dto.CreatePrRequestDto
import com.calisthenics.ai.data.remote.dto.UpdatePrRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateAdminUserRoleRequestDto
import com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto
import com.calisthenics.ai.data.remote.dto.BatchSharePrRequestDto
import com.calisthenics.ai.data.remote.dto.NotificationDto
import com.calisthenics.ai.data.remote.dto.UnreadCountDto
import retrofit2.http.PATCH
import com.calisthenics.ai.data.remote.dto.CoachFeedbackRequestDto
import com.calisthenics.ai.data.remote.dto.ExerciseHistorySessionDto
import com.calisthenics.ai.data.remote.dto.ExerciseProgressDto
import com.calisthenics.ai.data.remote.dto.TrainingOverviewDto
import com.calisthenics.ai.data.remote.dto.WeeklyAnalyticsDto
import com.calisthenics.ai.data.remote.dto.MonthlyAnalyticsDto
import com.calisthenics.ai.data.remote.dto.TrainingInsightsDto
import com.calisthenics.ai.data.remote.dto.BodyMeasurementDto
import com.calisthenics.ai.data.remote.dto.CurrentBodyMetricsDto
import com.calisthenics.ai.data.remote.dto.BodyMetricsHistoryDto
import com.calisthenics.ai.data.remote.dto.CoachClientBodyMetricsDto
import com.calisthenics.ai.data.remote.dto.RecordBodyMeasurementRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateHeightRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface CalisthenicsApi {

    @GET("api/v1/admin/dashboard")
    suspend fun getAdminDashboard(): AdminDashboardDto

    @GET("api/v1/admin/users")
    suspend fun getAdminUsers(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String?,
        @Query("role") role: String?,
        @Query("sortBy") sortBy: String,
        @Query("sortDirection") sortDirection: String
    ): AdminPageDto<AdminUserDto>

    @GET("api/v1/admin/users/{userId}")
    suspend fun getAdminUser(@Path("userId") userId: Long): AdminUserDto

    @GET("api/v1/admin/users/{userId}/role")
    suspend fun getAdminUserRole(@Path("userId") userId: Long): AdminUserRoleDto

    @PUT("api/v1/admin/users/{userId}/role")
    suspend fun updateAdminUserRole(
        @Path("userId") userId: Long,
        @Body request: UpdateAdminUserRoleRequestDto
    ): AdminUserDto

    @GET("api/v1/admin/workout-plans")
    suspend fun getAdminWorkoutPlans(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): AdminPageDto<AdminWorkoutPlanDto>

    @GET("api/v1/admin/workout-plans/{planId}")
    suspend fun getAdminWorkoutPlan(@Path("planId") planId: Long): AdminWorkoutPlanDetailDto

    @GET("api/v1/admin/workout-sessions")
    suspend fun getAdminWorkoutSessions(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): AdminPageDto<AdminWorkoutSessionDto>

    @GET("api/v1/admin/workout-sessions/{sessionId}")
    suspend fun getAdminWorkoutSession(@Path("sessionId") sessionId: Long): AdminWorkoutSessionDetailDto

    @GET("api/v1/admin/exercises")
    suspend fun getAdminExercises(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String?,
        @Query("category") category: String?,
        @Query("difficulty") difficulty: String?,
        @Query("active") active: Boolean?,
        @Query("sortBy") sortBy: String,
        @Query("sortDirection") sortDirection: String
    ): AdminPageDto<AdminExerciseDto>

    @GET("api/v1/admin/exercises/{exerciseId}")
    suspend fun getAdminExercise(@Path("exerciseId") exerciseId: Long): AdminExerciseDto

    @POST("api/v1/admin/exercises")
    suspend fun createAdminExercise(@Body request: CreateAdminExerciseRequestDto): AdminExerciseDto

    @PUT("api/v1/admin/exercises/{exerciseId}")
    suspend fun updateAdminExercise(
        @Path("exerciseId") exerciseId: Long,
        @Body request: UpdateAdminExerciseRequestDto
    ): AdminExerciseDto

    @PATCH("api/v1/admin/exercises/{exerciseId}/status")
    suspend fun updateAdminExerciseStatus(
        @Path("exerciseId") exerciseId: Long,
        @Body request: UpdateExerciseStatusRequestDto
    ): AdminExerciseDto

    @DELETE("api/v1/admin/exercises/{exerciseId}")
    suspend fun deleteAdminExercise(
        @Path("exerciseId") exerciseId: Long,
        @Query("softDelete") softDelete: Boolean
    ): AdminExerciseDeleteResponseDto

    @GET("api/v1/exercises")
    suspend fun getExercises(): List<ExerciseResponseDto>

    @Multipart
    @POST("api/v1/analysis/live/frame")
    suspend fun analyzeLiveFrame(
        @Part file: MultipartBody.Part,
        @Part("exercise") exercise: RequestBody,
        @Part("session_id") sessionId: RequestBody,
        @Part("frame_sequence") frameSequence: RequestBody,
        @Part("timestamp_ms") timestampMs: RequestBody,
        @Part("side_view") sideView: RequestBody,
        @Part("draw_overlay") drawOverlay: RequestBody,
        @Part("debug") debug: RequestBody
    ): LiveAnalysisResponseDto

    @GET("api/v1/analysis/live/exercises")
    suspend fun getLiveExercises(): LiveExerciseCatalogDto

    @GET("api/v1/exercises/{id}")
    suspend fun getExerciseById(@Path("id") id: Long): ExerciseResponseDto

    @GET("api/v1/exercises/code/{code}")
    suspend fun getExerciseByCode(@Path("code") code: String): ExerciseResponseDto

    @Multipart
    @POST("api/v1/analysis/video")
    suspend fun analyzeVideo(
        @Query("exerciseId") exerciseId: Long?,
        @Part file: MultipartBody.Part
    ): AnalysisResponseDto

    @Multipart
    @POST("api/v1/analysis/image")
    suspend fun analyzeImage(
        @Query("exerciseId") exerciseId: Long?,
        @Part file: MultipartBody.Part
    ): AnalysisResponseDto

    @GET("api/v1/analysis/history")
    suspend fun getAnalysisHistory(): List<AnalysisResponseDto>

    @GET("api/v1/analysis/{id}")
    suspend fun getAnalysisById(@Path("id") id: Long): AnalysisResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto

    @POST("api/v1/auth/google")
    suspend fun authenticateWithGoogle(@Body request: GoogleLoginRequestDto): AuthResponseDto

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): RegisterInitiationResponseDto

    @POST("api/v1/auth/register/verify-otp")
    suspend fun verifyRegistrationOtp(@Body request: VerifyRegistrationOtpRequestDto): AuthResponseDto

    @POST("api/v1/auth/register/resend-otp")
    suspend fun resendRegistrationOtp(@Body request: ResendRegistrationOtpRequestDto): RegisterInitiationResponseDto

    @POST("api/v1/auth/forgot-password")
    suspend fun requestPasswordResetOtp(@Body request: ForgotPasswordRequestDto): ForgotPasswordResponseDto

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyPasswordResetOtp(@Body request: VerifyOtpRequestDto): VerifyOtpResponseDto

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): ResetPasswordResponseDto

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: com.calisthenics.ai.data.remote.dto.RefreshTokenRequestDto): AuthResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): UserProfileResponseDto

    @GET("api/v1/coaches/me")
    suspend fun getCoachProfileMe(): CoachProfileDto

    @PUT("api/v1/coaches/me")
    suspend fun updateCoachProfileMe(@Body request: CoachProfileRequestDto): CoachProfileDto

    @GET("api/v1/coaches")
    suspend fun getCoaches(): List<CoachProfileDto>

    @GET("api/v1/coaches/{id}")
    suspend fun getCoachProfile(@Path("id") id: Long): CoachProfileDto

    @GET("api/v1/clients/me")
    suspend fun getClientProfileMe(): ClientProfileDto

    @PUT("api/v1/clients/me")
    suspend fun updateClientProfileMe(@Body request: ClientProfileRequestDto): ClientProfileDto

    @GET("api/v1/clients/{id}")
    suspend fun getClientProfile(@Path("id") id: Long): ClientProfileDto

    @GET("api/v1/clients/me/overview")
    suspend fun getClientMyOverview(): ClientTrainingOverviewDto

    @GET("api/v1/clients/{id}/overview")
    suspend fun getClientOverview(@Path("id") id: Long): ClientTrainingOverviewDto

    @Multipart
    @POST("api/v1/users/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): AvatarUploadResponseDto

    @Multipart
    @POST("api/v1/storage/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): FileUploadResponseDto

    @Multipart
    @POST("api/v1/storage/exercise-media/upload")
    suspend fun uploadExerciseMedia(
        @Part file: MultipartBody.Part,
        @Query("exerciseId") exerciseId: Long? = null,
        @Query("displayOrder") displayOrder: Int? = null
    ): FileUploadResponseDto

    @POST("api/v1/coach-client-relationships/requests")
    suspend fun requestCoachConnection(
        @Body request: CreateClientCoachRequestDto
    ): CoachClientRelationshipDto

    @POST("api/v1/coach-client-relationships/{id}/accept")
    suspend fun acceptCoachConnection(@Path("id") id: Long): CoachClientRelationshipDto

    @POST("api/v1/coach-client-relationships/{id}/reject")
    suspend fun rejectCoachConnection(@Path("id") id: Long): CoachClientRelationshipDto

    @POST("api/v1/coach-client-relationships/{id}/cancel")
    suspend fun cancelCoachConnection(@Path("id") id: Long): CoachClientRelationshipDto

    @DELETE("api/v1/coach-client-relationships/{id}")
    suspend fun terminateCoachConnection(@Path("id") id: Long): CoachClientRelationshipDto

    @GET("api/v1/workout-plans")
    suspend fun getWorkoutPlans(): List<WorkoutPlanResponseDto>

    @GET("api/v1/workout-plans/today")
    suspend fun resolveTodayWorkout(
        @Query("date") date: String,
        @Query("activePlanId") activePlanId: Long?
    ): TodayWorkoutResolutionResponseDto

    @GET("api/v1/workout-plans/templates")
    suspend fun getWorkoutPlanTemplates(): List<WorkoutPlanTemplateResponseDto>

    @POST("api/v1/workout-plans")
    suspend fun createWorkoutPlan(@Body request: CreateWorkoutPlanRequestDto): WorkoutPlanResponseDto

    @GET("api/v1/workout-plans/{id}")
    suspend fun getWorkoutPlanById(@Path("id") id: Long): WorkoutPlanResponseDto

    @PUT("api/v1/workout-plans/{id}")
    suspend fun updateWorkoutPlan(
        @Path("id") id: Long,
        @Body request: UpdateWorkoutPlanRequestDto
    ): WorkoutPlanResponseDto

    @DELETE("api/v1/workout-plans/{id}")
    suspend fun deleteWorkoutPlan(@Path("id") id: Long): Response<Unit>

    @GET("api/v1/workout-plans/{planId}/days/{dayId}/exercises")
    suspend fun getWorkoutPlanDayExercises(
        @Path("planId") planId: Long,
        @Path("dayId") dayId: Long
    ): List<WorkoutPlanExerciseResponseDto>

    @PUT("api/v1/workout-plans/{planId}/days/{dayId}/exercises")
    suspend fun updateWorkoutPlanDayExercises(
        @Path("planId") planId: Long,
        @Path("dayId") dayId: Long,
        @Body request: WorkoutDayExercisesBatchRequestDto
    ): List<WorkoutPlanExerciseResponseDto>

    @POST("api/v1/workout-plans/{planId}/days/{dayId}/exercises")
    suspend fun addExerciseToWorkoutPlanDay(
        @Path("planId") planId: Long,
        @Path("dayId") dayId: Long,
        @Body request: WorkoutPlanExerciseRequestDto
    ): WorkoutPlanExerciseResponseDto

    @PUT("api/v1/workout-plans/{planId}/days/{dayId}/exercises/{assignmentId}")
    suspend fun updateWorkoutPlanExercise(
        @Path("planId") planId: Long,
        @Path("dayId") dayId: Long,
        @Path("assignmentId") assignmentId: Long,
        @Body request: WorkoutPlanExerciseRequestDto
    ): WorkoutPlanExerciseResponseDto

    @DELETE("api/v1/workout-plans/{planId}/days/{dayId}/exercises/{assignmentId}")
    suspend fun removeExerciseFromWorkoutPlanDay(
        @Path("planId") planId: Long,
        @Path("dayId") dayId: Long,
        @Path("assignmentId") assignmentId: Long
    ): Response<Unit>

    @POST("api/v1/workout-sessions")
    suspend fun startOrResumeWorkoutSession(
        @Body request: StartWorkoutSessionRequestDto
    ): WorkoutSessionResponseDto

    @GET("api/v1/workout-sessions/active")
    suspend fun getActiveWorkoutSessions(): List<WorkoutSessionResponseDto>

    @GET("api/v1/workout-sessions")
    suspend fun getWorkoutSessionHistory(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): WorkoutSessionPageResponseDto

    @GET("api/v1/workout-sessions/{sessionId}")
    suspend fun getWorkoutSession(@Path("sessionId") sessionId: Long): WorkoutSessionResponseDto

    @PUT("api/v1/workout-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    suspend fun updateWorkoutSessionSet(
        @Path("sessionId") sessionId: Long,
        @Path("exerciseId") exerciseId: Long,
        @Path("setId") setId: Long,
        @Body request: UpdateWorkoutSessionSetRequestDto
    ): WorkoutSessionResponseDto

    @PUT("api/v1/workout-sessions/{sessionId}/exercises/{exerciseId}")
    suspend fun updateWorkoutSessionExercise(
        @Path("sessionId") sessionId: Long,
        @Path("exerciseId") exerciseId: Long,
        @Body request: UpdateWorkoutSessionExerciseRequestDto
    ): WorkoutSessionResponseDto

    @POST("api/v1/workout-sessions/{sessionId}/finish")
    suspend fun finishWorkoutSession(
        @Path("sessionId") sessionId: Long,
        @Body request: FinishWorkoutSessionRequestDto
    ): WorkoutSessionResponseDto

    @GET("api/v1/chat/conversations")
    suspend fun getChatConversations(): List<com.calisthenics.ai.data.remote.dto.ChatConversationDto>

    @POST("api/v1/chat/conversations")
    suspend fun createOrGetChatConversation(
        @Body request: com.calisthenics.ai.data.remote.dto.CreateChatConversationRequestDto
    ): com.calisthenics.ai.data.remote.dto.ChatConversationDto

    @GET("api/v1/chat/conversations/{conversationId}/messages")
    suspend fun getChatMessages(
        @Path("conversationId") conversationId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("direction") direction: String = "desc"
    ): List<com.calisthenics.ai.data.remote.dto.ChatMessageDto>

    @POST("api/v1/chat/conversations/{conversationId}/messages")
    suspend fun sendChatMessage(
        @Path("conversationId") conversationId: Long,
        @Body request: com.calisthenics.ai.data.remote.dto.SendChatMessageRequestDto
    ): com.calisthenics.ai.data.remote.dto.ChatMessageDto

    @POST("api/v1/chat/conversations/{conversationId}/read")
    suspend fun markChatConversationAsRead(
        @Path("conversationId") conversationId: Long
    ): Response<Unit>

    @GET("api/v1/chat/unread-count")
    suspend fun getChatUnreadCount(): com.calisthenics.ai.data.remote.dto.UnreadCountDto

    @GET("api/v1/chat/presence")
    suspend fun getChatPresence(): List<com.calisthenics.ai.data.remote.dto.UserPresenceDto>

    @GET("api/v1/coach-client-relationships")
    suspend fun getCoachClientRelationships(): List<CoachClientRelationshipDto>

    // Training Analytics & Progress Tracking
    @GET("api/v1/analytics/overview")
    suspend fun getTrainingOverview(): TrainingOverviewDto

    @GET("api/v1/analytics/weekly")
    suspend fun getWeeklyAnalytics(): WeeklyAnalyticsDto

    @GET("api/v1/analytics/monthly")
    suspend fun getMonthlyAnalytics(
        @Query("yearMonth") yearMonth: String? = null
    ): MonthlyAnalyticsDto

    @GET("api/v1/analytics/insights")
    suspend fun getTrainingInsights(): TrainingInsightsDto

    @GET("api/v1/exercises/{exerciseId}/history")
    suspend fun getExerciseHistory(
        @Path("exerciseId") exerciseId: Long
    ): List<ExerciseHistorySessionDto>

    @GET("api/v1/exercises/{exerciseId}/progress")
    suspend fun getExerciseProgress(
        @Path("exerciseId") exerciseId: Long,
        @Query("filter") filter: String? = null
    ): ExerciseProgressDto

    @GET("api/v1/exercises/{exerciseId}/records")
    suspend fun getExerciseRecords(
        @Path("exerciseId") exerciseId: Long
    ): List<PersonalRecordDto>

    @GET("api/v1/workout-sessions/{sessionId}/records")
    suspend fun getWorkoutSessionRecords(
        @Path("sessionId") sessionId: Long
    ): List<PersonalRecordDto>

    // Personal Records CRUD & History
    @POST("api/analytics/pr")
    suspend fun createPersonalRecord(
        @Body request: CreatePrRequestDto
    ): PersonalRecordDto

    @GET("api/analytics/pr")
    suspend fun getUserPersonalRecords(
        @Query("exerciseId") exerciseId: Long? = null,
        @Query("prType") prType: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): List<PersonalRecordDto>

    @GET("api/analytics/pr/{exerciseId}/history")
    suspend fun getPrHistory(
        @Path("exerciseId") exerciseId: Long,
        @Query("prType") prType: String? = null
    ): List<PersonalRecordDto>

    @GET("api/analytics/pr/{exerciseId}/latest")
    suspend fun getLatestPr(
        @Path("exerciseId") exerciseId: Long,
        @Query("prType") prType: String? = null
    ): PersonalRecordDto

    @PUT("api/analytics/pr/{id}")
    suspend fun updatePersonalRecord(
        @Path("id") id: Long,
        @Body request: UpdatePrRequestDto
    ): PersonalRecordDto

    @DELETE("api/analytics/pr/{id}")
    suspend fun deletePersonalRecord(
        @Path("id") id: Long
    )

    @GET("api/v1/coach/clients/{clientId}/personal-records")
    suspend fun getCoachClientPrRecords(
        @Path("clientId") clientId: Long,
        @Query("exerciseId") exerciseId: Long? = null,
        @Query("prType") prType: String? = null
    ): List<PersonalRecordDto>

    @GET("api/v1/coach/clients/{clientId}/pr/{exerciseId}/history")
    suspend fun getCoachClientPrHistory(
        @Path("clientId") clientId: Long,
        @Path("exerciseId") exerciseId: Long,
        @Query("prType") prType: String? = null
    ): List<PersonalRecordDto>

    // PR Sharing & Coach Analytics
    @POST("api/v1/clients/me/pr-shares")
    suspend fun batchSharePRs(
        @Body request: BatchSharePrRequestDto
    ): List<SharedPersonalRecordDto>

    @POST("api/v1/prs/{prId}/share")
    suspend fun sharePersonalRecord(
        @Path("prId") prId: Long
    ): SharedPersonalRecordDto

    @GET("api/v1/clients/me/pr-shares")
    suspend fun getClientSharedPRs(): List<SharedPersonalRecordDto>

    @GET("api/v1/clients/me/feedback")
    suspend fun getClientFeedbacks(): List<SharedPersonalRecordDto>

    @GET("api/v1/coach/me/pr-shares")
    suspend fun getCoachSharedPRs(
        @Query("unread") unread: Boolean? = null
    ): List<SharedPersonalRecordDto>

    @PATCH("api/v1/coach/me/pr-shares/{shareId}/read")
    suspend fun markPrShareAsRead(
        @Path("shareId") shareId: Long
    ): SharedPersonalRecordDto

    @GET("api/v1/coach/clients/{clientId}/prs")
    suspend fun getCoachClientPRs(
        @Path("clientId") clientId: Long
    ): List<PersonalRecordDto>

    @GET("api/v1/coach/clients/{clientId}/progress")
    suspend fun getCoachClientOverview(
        @Path("clientId") clientId: Long
    ): TrainingOverviewDto

    @GET("api/v1/coach/clients/{clientId}/exercises/{exerciseId}/progress")
    suspend fun getCoachClientExerciseProgress(
        @Path("clientId") clientId: Long,
        @Path("exerciseId") exerciseId: Long,
        @Query("filter") filter: String? = null
    ): ExerciseProgressDto

    @POST("api/v1/coach/me/pr-shares/{shareId}/feedback")
    suspend fun addCoachFeedback(
        @Path("shareId") shareId: Long,
        @Body request: CoachFeedbackRequestDto
    ): SharedPersonalRecordDto

    // Notifications
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("unreadOnly") unreadOnly: Boolean? = null
    ): List<NotificationDto>

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadNotificationsCount(): UnreadCountDto

    @PATCH("api/v1/notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") id: Long
    ): NotificationDto

    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Map<String, String>

    // Body Metrics
    @GET("api/v1/clients/me/body-metrics/current")
    suspend fun getCurrentBodyMetrics(): CurrentBodyMetricsDto

    @POST("api/v1/clients/me/body-metrics")
    suspend fun recordBodyMeasurement(
        @Body request: RecordBodyMeasurementRequestDto
    ): BodyMeasurementDto

    @PUT("api/v1/clients/me/body-metrics/height")
    suspend fun updateHeight(
        @Body request: UpdateHeightRequestDto
    ): CurrentBodyMetricsDto

    @GET("api/v1/clients/me/body-metrics/history")
    suspend fun getBodyMetricsHistory(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("sort") sort: String = "desc"
    ): BodyMetricsHistoryDto

    @GET("api/v1/coach/clients/{clientId}/body-metrics")
    suspend fun getCoachClientBodyMetrics(
        @Path("clientId") clientId: Long,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("sort") sort: String = "asc"
    ): CoachClientBodyMetricsDto
}
