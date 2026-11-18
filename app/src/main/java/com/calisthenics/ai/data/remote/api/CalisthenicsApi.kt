package com.calisthenics.ai.data.remote.api

import com.calisthenics.ai.data.remote.dto.AdminGettingStartedProgramRequestDto
import com.calisthenics.ai.data.remote.dto.GettingStartedEnrollmentResponseDto
import com.calisthenics.ai.data.remote.dto.GettingStartedProgramResponseDto
import com.calisthenics.ai.data.remote.dto.UpdateDismissalStatusRequestDto
import com.calisthenics.ai.data.remote.dto.AiConversationDetailDto
import com.calisthenics.ai.data.remote.dto.AiConversationSummaryDto
import com.calisthenics.ai.data.remote.dto.AiMessageDto
import com.calisthenics.ai.data.remote.dto.AiPerformanceSummaryDto
import com.calisthenics.ai.data.remote.dto.CreateAiConversationRequestDto
import com.calisthenics.ai.data.remote.dto.SendAiMessageRequestDto
import com.calisthenics.ai.data.remote.dto.SharePrRequestDto
import com.calisthenics.ai.data.remote.dto.AnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.AnalysisSessionResponseDto
import com.calisthenics.ai.data.remote.dto.AnalysisHistorySummaryDto
import com.calisthenics.ai.data.remote.dto.CompleteAnalysisSessionRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAnalysisSessionRequestDto
import com.calisthenics.ai.data.remote.dto.RepHistoryDto
import com.calisthenics.ai.data.remote.dto.VideoAnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.VideoAnalysisKeypointsDto
import com.calisthenics.ai.data.remote.dto.AdminAiChatRequestDto
import com.calisthenics.ai.data.remote.dto.AdminAiChatResponseDto
import com.calisthenics.ai.data.remote.dto.AdminAiConversationSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminAiMessageDto
import com.calisthenics.ai.data.remote.dto.AdminAiRecommendationDto
import com.calisthenics.ai.data.remote.dto.AdminCoachClientStatisticsDto
import com.calisthenics.ai.data.remote.dto.AdminDashboardDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDeleteResponseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseDto
import com.calisthenics.ai.data.remote.dto.AdminExerciseStatsDto
import com.calisthenics.ai.data.remote.dto.BulkExerciseGenerationItemDto
import com.calisthenics.ai.data.remote.dto.BulkExerciseGenerationJobDto
import com.calisthenics.ai.data.remote.dto.BulkExerciseGenerationRequestDto
import com.calisthenics.ai.data.remote.dto.BulkUpdateExerciseStatusRequestDto
import com.calisthenics.ai.data.remote.dto.AdminGlobalUserStatisticsDto
import com.calisthenics.ai.data.remote.dto.AdminGlobalUserSummaryDto
import com.calisthenics.ai.data.remote.dto.AdminPageDto
import com.calisthenics.ai.data.remote.dto.AdminPagedResultDto
import com.calisthenics.ai.data.remote.dto.AdminPlatformEngagementDto
import com.calisthenics.ai.data.remote.dto.AdminPlatformOverviewDto
import com.calisthenics.ai.data.remote.dto.AdminTrainingAnalyticsDto
import com.calisthenics.ai.data.remote.dto.AdminUserDetailDossierDto
import com.calisthenics.ai.data.remote.dto.AdminUserDto
import com.calisthenics.ai.data.remote.dto.AdminUserRoleDto
import com.calisthenics.ai.data.remote.dto.AdminUserSegmentFilterRequestDto
import com.calisthenics.ai.data.remote.dto.AdminUserSegmentsDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutPlanDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDetailDto
import com.calisthenics.ai.data.remote.dto.AdminWorkoutSessionDto
import com.calisthenics.ai.data.remote.dto.CreateAdminAiConversationRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAdminExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.CreateAdminRecommendationRequestDto
import com.calisthenics.ai.data.remote.dto.ReviewAdminRecommendationRequestDto
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
import com.calisthenics.ai.data.remote.dto.WorkoutPlanAssignmentResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanAssignmentDto
import com.calisthenics.ai.data.remote.dto.CreateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.ExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.FileUploadResponseDto
import com.calisthenics.ai.data.remote.dto.GoogleLoginRequestDto
import com.calisthenics.ai.data.remote.dto.LoginRequestDto
import com.calisthenics.ai.data.remote.dto.LiveAnalysisResponseDto
import com.calisthenics.ai.data.remote.dto.LiveExerciseCatalogDto
import com.calisthenics.ai.data.remote.dto.FirebaseLoginRequestDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.ForgotPasswordResponseDto
import com.calisthenics.ai.data.remote.dto.RegisterRequestDto
import com.calisthenics.ai.data.remote.dto.RegisterInitiationResponseDto
import com.calisthenics.ai.data.remote.dto.VerifyRegistrationOtpRequestDto
import com.calisthenics.ai.data.remote.dto.ResendRegistrationOtpRequestDto
import com.calisthenics.ai.data.remote.dto.ResetPasswordRequestDto
import com.calisthenics.ai.data.remote.dto.ResetPasswordResponseDto
import com.calisthenics.ai.data.remote.dto.VerifyOtpRequestDto
import com.calisthenics.ai.data.remote.dto.GenerateAiWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.ModifyAiWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.CommitAiWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.AiWorkoutPlanPreviewDto
import com.calisthenics.ai.data.remote.dto.VerifyOtpResponseDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutPlanRequestDto
import com.calisthenics.ai.data.remote.dto.UserProfileResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutDayExercisesBatchRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseRequestDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanExerciseResponseDto
import com.calisthenics.ai.data.remote.dto.WorkoutPlanResponseDto
import com.calisthenics.ai.data.remote.dto.CreateWorkoutPlanAssignmentRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateWorkoutPlanAssignmentStatusRequestDto
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
import com.calisthenics.ai.data.remote.dto.UpdateGenderRequestDto
import com.calisthenics.ai.data.remote.dto.UpdateUserProfileRequestDto
import com.calisthenics.ai.data.remote.dto.SharedPersonalRecordDto
import com.calisthenics.ai.data.remote.dto.BatchSharePrRequestDto
import com.calisthenics.ai.data.remote.dto.NotificationDto
import com.calisthenics.ai.data.remote.dto.ProfileMediaDto
import com.calisthenics.ai.data.remote.dto.ReorderProfileMediaRequestDto
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
import com.calisthenics.ai.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
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

    @POST("api/v1/admin/users/{userId}/pro/grant")
    suspend fun grantAdminPro(
        @Path("userId") userId: Long,
        @Body request: com.calisthenics.ai.data.remote.dto.AdminProActionRequestDto =
            com.calisthenics.ai.data.remote.dto.AdminProActionRequestDto()
    ): AdminUserDto

    @POST("api/v1/admin/users/{userId}/pro/revoke")
    suspend fun revokeAdminPro(
        @Path("userId") userId: Long,
        @Body request: com.calisthenics.ai.data.remote.dto.AdminProActionRequestDto =
            com.calisthenics.ai.data.remote.dto.AdminProActionRequestDto()
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
        @Query("sortDirection") sortDirection: String,
        @Query("status") status: String? = null,
        @Query("origin") origin: String? = null,
        @Query("trainingType") trainingType: String? = null
    ): AdminPageDto<AdminExerciseDto>

    @POST("api/v1/admin/exercises/bulk-generate")
    suspend fun startBulkExerciseGeneration(
        @Body request: BulkExerciseGenerationRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): BulkExerciseGenerationJobDto

    @GET("api/v1/admin/exercises/bulk-generate")
    suspend fun listBulkExerciseGenerationJobs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): AdminPageDto<BulkExerciseGenerationJobDto>

    @GET("api/v1/admin/exercises/bulk-generate/{jobId}")
    suspend fun getBulkExerciseGenerationJob(
        @Path("jobId") jobId: Long
    ): BulkExerciseGenerationJobDto

    @GET("api/v1/admin/exercises/bulk-generate/{jobId}/items")
    suspend fun getBulkExerciseGenerationItems(
        @Path("jobId") jobId: Long,
        @Query("outcome") outcome: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 25
    ): AdminPageDto<BulkExerciseGenerationItemDto>

    @POST("api/v1/admin/exercises/bulk-generate/{jobId}/resume")
    suspend fun resumeBulkExerciseGenerationJob(
        @Path("jobId") jobId: Long
    ): BulkExerciseGenerationJobDto

    @GET("api/v1/admin/exercises/stats")
    suspend fun getAdminExerciseStats(): AdminExerciseStatsDto

    @PATCH("api/v1/admin/exercises/bulk-status")
    suspend fun bulkUpdateAdminExerciseStatus(
        @Body request: BulkUpdateExerciseStatusRequestDto
    ): List<AdminExerciseDto>

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

    @GET("api/v1/admin/getting-started-program")
    suspend fun getAdminGettingStartedProgram(): retrofit2.Response<GettingStartedProgramResponseDto>

    @POST("api/v1/admin/getting-started-program")
    suspend fun saveAdminGettingStartedProgram(
        @Body request: AdminGettingStartedProgramRequestDto
    ): GettingStartedProgramResponseDto

    @POST("api/v1/admin/getting-started-program/publish")
    suspend fun publishGettingStartedProgram(): GettingStartedProgramResponseDto

    @POST("api/v1/admin/getting-started-program/unpublish")
    suspend fun unpublishGettingStartedProgram(): GettingStartedProgramResponseDto

    @DELETE("api/v1/admin/getting-started-program")
    suspend fun deleteGettingStartedProgram(): retrofit2.Response<Unit>

    @GET("api/v1/getting-started-program")
    suspend fun getPublishedGettingStartedProgram(): retrofit2.Response<GettingStartedProgramResponseDto>

    @PATCH("api/v1/getting-started-program/dismissal-status")
    suspend fun updateGettingStartedDismissalStatus(
        @Body request: UpdateDismissalStatusRequestDto
    ): GettingStartedProgramResponseDto

    @GET("api/v1/getting-started-program/enroll/preview")
    suspend fun previewGettingStartedEnrollment(
        @Query("startDate") startDate: String? = null
    ): GettingStartedEnrollPreviewDto

    @GET("api/v1/getting-started-program/enrollments")
    suspend fun getGettingStartedEnrollments(): List<GettingStartedEnrollmentResponseDto>

    @POST("api/v1/getting-started-program/enroll")
    suspend fun enrollGettingStartedProgram(
        @Body request: EnrollGettingStartedRequestDto = EnrollGettingStartedRequestDto()
    ): GettingStartedEnrollmentResponseDto

    @POST("api/v1/getting-started-program/days/{dayNumber}/start")
    suspend fun startGettingStartedDay(
        @Path("dayNumber") dayNumber: Int,
        @Query("workoutDate") workoutDate: String? = null
    ): WorkoutSessionResponseDto

    @POST("api/v1/getting-started-program/complete-day/{dayNumber}")
    suspend fun completeGettingStartedDay(
        @Path("dayNumber") dayNumber: Int
    ): GettingStartedEnrollmentResponseDto

    @POST("api/v1/workout-sessions/{sessionId}/start")
    suspend fun startScheduledWorkoutSession(
        @Path("sessionId") sessionId: Long
    ): WorkoutSessionResponseDto

    @POST("api/v1/getting-started-program/events")
    suspend fun recordGettingStartedCampaignEvent(
        @Body request: GettingStartedCampaignEventRequestDto
    ): retrofit2.Response<Unit>

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

    @POST("api/v1/analysis/sessions")
    suspend fun createAnalysisSession(
        @Body request: CreateAnalysisSessionRequestDto
    ): AnalysisSessionResponseDto

    @POST("api/v1/analysis/sessions/{sessionId}/reps")
    suspend fun recordRep(
        @Path("sessionId") sessionId: String,
        @Body rep: RepHistoryDto
    ): AnalysisSessionResponseDto

    @POST("api/v1/analysis/sessions/{sessionId}/complete")
    suspend fun completeAnalysisSession(
        @Path("sessionId") sessionId: String,
        @Body request: CompleteAnalysisSessionRequestDto
    ): AnalysisSessionResponseDto

    @POST("api/v1/analysis/sessions/{sessionId}/ai-summary")
    suspend fun generateAnalysisSessionAiSummary(
        @Path("sessionId") sessionId: String
    ): AiPerformanceSummaryDto

    @GET("api/v1/analysis/sessions")
    suspend fun getAnalysisSessions(
        @Query("exerciseType") exerciseType: String? = null
    ): List<AnalysisSessionResponseDto>

    @GET("api/v1/analysis/sessions/{sessionId}")
    suspend fun getAnalysisSessionDetails(
        @Path("sessionId") sessionId: String
    ): AnalysisSessionResponseDto

    @GET("api/v1/analysis/sessions/summary")
    suspend fun getAnalysisHistorySummary(
        @Query("exerciseType") exerciseType: String? = null
    ): AnalysisHistorySummaryDto

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

    @POST("api/v1/auth/firebase")
    suspend fun authenticateWithFirebase(@Body request: FirebaseLoginRequestDto): AuthResponseDto

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

    @PUT("api/v1/users/me")
    suspend fun updateCurrentUser(@Body request: UpdateUserProfileRequestDto): UserProfileResponseDto

    @PATCH("api/v1/users/me/gender")
    suspend fun updateCurrentUserGender(@Body request: UpdateGenderRequestDto): UserProfileResponseDto

    @PATCH("api/v1/users/me/onboarding-status")
    suspend fun updateCurrentUserOnboardingStatus(@Body request: UpdateOnboardingStatusRequestDto): UserProfileResponseDto

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

    @GET("api/v1/profile-media")
    suspend fun getProfileMedia(
        @Query("userId") userId: Long,
        @Query("profileType") profileType: String
    ): List<ProfileMediaDto>

    @Multipart
    @POST("api/v1/profile-media")
    suspend fun uploadProfileMedia(
        @Query("profileType") profileType: String,
        @Part file: MultipartBody.Part,
        @Part thumbnail: MultipartBody.Part? = null
    ): ProfileMediaDto

    @DELETE("api/v1/profile-media/{id}")
    suspend fun deleteProfileMedia(@Path("id") id: Long): Response<Unit>

    @PUT("api/v1/profile-media/reorder")
    suspend fun reorderProfileMedia(@Body request: ReorderProfileMediaRequestDto): List<ProfileMediaDto>

    @PATCH("api/v1/profile-media/{id}/cover")
    suspend fun setProfileMediaCover(@Path("id") id: Long): List<ProfileMediaDto>

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

    @GET("api/v1/workout-plans/clients/{clientId}/personal")
    suspend fun getClientPersonalWorkoutPlans(
        @Path("clientId") clientId: Long
    ): List<WorkoutPlanResponseDto>

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

    @POST("api/v1/ai/workout-plans/generate")
    suspend fun generateAiWorkoutPlan(
        @Body request: GenerateAiWorkoutPlanRequestDto
    ): AiWorkoutPlanPreviewDto

    @POST("api/v1/ai/workout-plans/modify")
    suspend fun modifyAiWorkoutPlan(
        @Body request: ModifyAiWorkoutPlanRequestDto
    ): AiWorkoutPlanPreviewDto

    @POST("api/v1/ai/workout-plans/commit")
    suspend fun commitAiWorkoutPlan(
        @Body request: CommitAiWorkoutPlanRequestDto
    ): WorkoutPlanResponseDto

    @POST("api/v1/workout-sessions")
    suspend fun startOrResumeWorkoutSession(
        @Body request: StartWorkoutSessionRequestDto
    ): WorkoutSessionResponseDto

    @GET("api/v1/workout-sessions/active")
    suspend fun getActiveWorkoutSessions(
        @Query("localDate") localDate: String? = null
    ): List<WorkoutSessionResponseDto>

    @GET("api/v1/workout-sessions/upcoming")
    suspend fun getUpcomingWorkoutSessions(
        @Query("localDate") localDate: String? = null
    ): List<WorkoutSessionResponseDto>

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

    @POST("api/v1/workout-sessions/{sessionId}/exercises/{exerciseId}/sets")
    suspend fun addWorkoutSessionSet(
        @Path("sessionId") sessionId: Long,
        @Path("exerciseId") exerciseId: Long
    ): WorkoutSessionResponseDto

    @DELETE("api/v1/workout-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    suspend fun deleteWorkoutSessionSet(
        @Path("sessionId") sessionId: Long,
        @Path("exerciseId") exerciseId: Long,
        @Path("setId") setId: Long
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

    @POST("api/v1/coach-client-relationships/{relationshipId}/assignments")
    suspend fun assignWorkoutPlan(
        @Path("relationshipId") relationshipId: Long,
        @Body request: CreateWorkoutPlanAssignmentRequestDto
    ): WorkoutPlanAssignmentResponseDto

    @GET("api/v1/coach/me/workout-plan-assignments")
    suspend fun getMyCoachWorkoutPlanAssignments(): List<WorkoutPlanAssignmentDto>

    @PATCH("api/v1/coach/assignments/{assignmentId}/status")
    suspend fun updateWorkoutPlanAssignmentStatus(
        @Path("assignmentId") assignmentId: Long,
        @Body request: UpdateWorkoutPlanAssignmentStatusRequestDto
    ): WorkoutPlanAssignmentDto

    @GET("api/v1/clients/me/workout-plan-assignments")
    suspend fun getMyWorkoutPlanAssignments(): List<WorkoutPlanAssignmentDto>

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
        @Path("prId") prId: Long,
        @Body request: SharePrRequestDto? = null
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
    ): List<SharedPersonalRecordDto>

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
        @Query("unreadOnly") unreadOnly: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
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

    // Video Analysis Endpoints
    @Multipart
    @POST("api/v1/video-analysis")
    suspend fun uploadVideoForAnalysis(
        @Header("Idempotency-Key") idempotencyKey: String? = null,
        @Part video: MultipartBody.Part,
        @Part("exerciseId") exerciseId: RequestBody? = null,
        @Part("exerciseType") exerciseType: RequestBody? = null,
        @Part("sideView") sideView: RequestBody? = null
    ): VideoAnalysisResponseDto

    @GET("api/v1/video-analysis/quota")
    suspend fun getVideoAnalysisQuota(): QuotaEntitlementDto

    @GET("api/v1/users/me/entitlements")
    suspend fun getUserEntitlements(): QuotaEntitlementDto

    @GET("api/v1/users/me/usage")
    suspend fun getUserUsage(): UserUsageResponseDto

    @PUT("api/v1/users/me/membership")
    suspend fun updateMembershipTier(
        @Body request: UpdateMembershipRequestDto
    ): QuotaEntitlementDto

    @GET("api/v1/video-analysis/{id}")
    suspend fun getVideoAnalysisById(
        @Path("id") id: Long
    ): VideoAnalysisResponseDto

    @GET("api/v1/video-analysis/{id}/keypoints")
    suspend fun getVideoAnalysisKeypoints(
        @Path("id") id: Long
    ): VideoAnalysisKeypointsDto

    @GET("api/v1/video-analysis/history")
    suspend fun getVideoAnalysisHistory(): List<VideoAnalysisResponseDto>

    // AI Assistant Endpoints
    @GET("api/v1/ai/conversations")
    suspend fun getAiConversations(): List<AiConversationSummaryDto>

    @GET("api/v1/ai/conversations/latest")
    suspend fun getLatestAiConversation(): retrofit2.Response<AiConversationSummaryDto?>

    @POST("api/v1/ai/conversations")
    suspend fun createAiConversation(
        @Body request: CreateAiConversationRequestDto
    ): AiConversationSummaryDto

    @GET("api/v1/ai/conversations/{id}")
    suspend fun getAiConversation(
        @Path("id") id: Long
    ): AiConversationDetailDto

    @DELETE("api/v1/ai/conversations/{id}")
    suspend fun deleteAiConversation(
        @Path("id") id: Long
    ): retrofit2.Response<Unit>

    @POST("api/v1/ai/conversations/{id}/messages")
    suspend fun sendAiMessage(
        @Path("id") id: Long,
        @Body request: SendAiMessageRequestDto
    ): AiMessageDto

    @POST("api/v1/ai/conversations/messages")
    suspend fun sendQuickAiMessage(
        @Body request: SendAiMessageRequestDto
    ): AiMessageDto

    // Dedicated Admin AI Agent Endpoints
    @POST("api/v1/admin/ai/chat")
    suspend fun sendAdminAiChat(
        @Body request: AdminAiChatRequestDto,
        @Header("Idempotency-Key") idempotencyKey: String? = null
    ): AdminAiChatResponseDto

    @POST("api/v1/admin/ai/confirmations/{token}")
    suspend fun resolveAdminAiConfirmation(
        @Path("token") confirmationToken: String,
        @Body request: AdminConfirmationDecisionRequestDto
    ): AdminConfirmationDecisionResponseDto

    @GET("api/v1/admin/ai/conversations")
    suspend fun getAdminAiConversations(): List<AdminAiConversationSummaryDto>

    @POST("api/v1/admin/ai/conversations")
    suspend fun createAdminAiConversation(
        @Body request: CreateAdminAiConversationRequestDto = CreateAdminAiConversationRequestDto()
    ): AdminAiConversationSummaryDto

    @GET("api/v1/admin/ai/conversations/{id}/messages")
    suspend fun getAdminAiConversationMessages(
        @Path("id") id: Long
    ): List<AdminAiMessageDto>

    @GET("api/v1/admin/ai/analytics/overview")
    suspend fun getAdminAiPlatformOverview(
        @Query("range") range: String = "LAST_30_DAYS",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): AdminPlatformOverviewDto

    @GET("api/v1/admin/ai/analytics/training")
    suspend fun getAdminAiTrainingAnalytics(
        @Query("range") range: String = "LAST_30_DAYS",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): AdminTrainingAnalyticsDto

    @GET("api/v1/admin/ai/recommendations")
    suspend fun getAdminAiRecommendations(
        @Query("type") type: String? = null,
        @Query("status") status: String? = null
    ): List<AdminAiRecommendationDto>

    @POST("api/v1/admin/ai/recommendations")
    suspend fun createAdminAiRecommendations(
        @Body request: CreateAdminRecommendationRequestDto
    ): List<AdminAiRecommendationDto>

    @PATCH("api/v1/admin/ai/recommendations/{id}/approve")
    suspend fun approveAdminAiRecommendation(
        @Path("id") id: Long,
        @Body request: ReviewAdminRecommendationRequestDto = ReviewAdminRecommendationRequestDto()
    ): AdminAiRecommendationDto

    @PATCH("api/v1/admin/ai/recommendations/{id}/reject")
    suspend fun rejectAdminAiRecommendation(
        @Path("id") id: Long,
        @Body request: ReviewAdminRecommendationRequestDto = ReviewAdminRecommendationRequestDto()
    ): AdminAiRecommendationDto

    @PATCH("api/v1/admin/ai/recommendations/{id}/implement")
    suspend fun implementAdminAiRecommendation(
        @Path("id") id: Long,
        @Body request: ReviewAdminRecommendationRequestDto = ReviewAdminRecommendationRequestDto()
    ): AdminAiRecommendationDto

    @GET("api/v1/admin/ai/analytics/users")
    suspend fun getAdminAiGlobalUserStatistics(
        @Query("range") range: String = "LAST_30_DAYS",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): AdminGlobalUserStatisticsDto

    @GET("api/v1/admin/ai/users")
    suspend fun getAdminAiGlobalUsers(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("search") search: String? = null,
        @Query("experienceLevel") experienceLevel: String? = null,
        @Query("fitnessGoal") fitnessGoal: String? = null,
        @Query("activityStatus") activityStatus: String? = null,
        @Query("inactiveDaysThreshold") inactiveDaysThreshold: Int? = null,
        @Query("maxCompletionRate") maxCompletionRate: Double? = null,
        @Query("coachRelationshipStatus") coachRelationshipStatus: String? = null,
        @Query("roleFilter") roleFilter: String? = null,
        @Query("skillKeyword") skillKeyword: String? = null,
        @Query("includeEmail") includeEmail: Boolean = false,
        @Query("sortBy") sortBy: String? = null,
        @Query("range") range: String = "LAST_30_DAYS",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): AdminPagedResultDto<AdminGlobalUserSummaryDto>

    @GET("api/v1/admin/ai/users/{userId}")
    suspend fun getAdminAiGlobalUserDetail(
        @Path("userId") userId: Long,
        @Query("includeEmail") includeEmail: Boolean = false,
        @Query("range") range: String = "LAST_30_DAYS",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): AdminUserDetailDossierDto

    @GET("api/v1/admin/ai/analytics/coaching")
    suspend fun getAdminAiCoachClientStatistics(
        @Query("range") range: String = "LAST_30_DAYS",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): AdminCoachClientStatisticsDto

    @GET("api/v1/admin/ai/analytics/engagement")
    suspend fun getAdminAiPlatformEngagement(
        @Query("range") range: String = "LAST_30_DAYS",
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): AdminPlatformEngagementDto

    @POST("api/v1/admin/ai/analytics/segments")
    suspend fun analyzeAdminAiUserSegments(
        @Body filter: AdminUserSegmentFilterRequestDto
    ): AdminUserSegmentsDto

    // ---- Coach reviews ----
    @GET("api/v1/coaches/{coachId}/reviews/summary")
    suspend fun getCoachReviewSummary(@Path("coachId") coachId: Long): CoachReviewSummaryDto

    @GET("api/v1/coaches/{coachId}/reviews")
    suspend fun getCoachReviews(
        @Path("coachId") coachId: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): CoachReviewPageDto

    @GET("api/v1/coaches/{coachId}/reviews/mine")
    suspend fun getMyCoachReview(@Path("coachId") coachId: Long): MyReviewStatusDto

    @POST("api/v1/coaches/{coachId}/reviews")
    suspend fun createCoachReview(
        @Path("coachId") coachId: Long,
        @Body request: ReviewRequestDto
    ): CoachReviewDto

    @PUT("api/v1/coaches/{coachId}/reviews/mine")
    suspend fun updateMyCoachReview(
        @Path("coachId") coachId: Long,
        @Body request: ReviewRequestDto
    ): CoachReviewDto

    @GET("api/v1/admin/reviews")
    suspend fun getAdminReviews(
        @Query("status") status: String?,
        @Query("coachId") coachId: Long?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): AdminPageDto<AdminReviewDto>

    @GET("api/v1/admin/reviews/{id}")
    suspend fun getAdminReview(@Path("id") id: Long): AdminReviewDto

    @PUT("api/v1/admin/reviews/{id}/moderation")
    suspend fun moderateAdminReview(
        @Path("id") id: Long,
        @Body request: ModerateReviewRequestDto
    ): AdminReviewDto

    // ---- User reports ----
    @POST("api/v1/users/{userId}/reports")
    suspend fun reportUser(
        @Path("userId") userId: Long,
        @Body request: CreateUserReportRequestDto
    ): UserReportCreatedDto

    @GET("api/v1/admin/user-reports/summary")
    suspend fun getAdminUserReportSummary(): UserReportSummaryDto

    @GET("api/v1/admin/user-reports")
    suspend fun getAdminUserReports(
        @Query("status") status: String?,
        @Query("reportedUserId") reportedUserId: Long?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): AdminPageDto<AdminUserReportDto>

    @GET("api/v1/admin/user-reports/{id}")
    suspend fun getAdminUserReport(@Path("id") id: Long): AdminUserReportDto

    @PUT("api/v1/admin/user-reports/{id}/status")
    suspend fun updateAdminUserReportStatus(
        @Path("id") id: Long,
        @Body request: UpdateReportStatusRequestDto
    ): AdminUserReportDto

    @POST("api/v1/admin/user-reports/{id}/notes")
    suspend fun addAdminUserReportNote(
        @Path("id") id: Long,
        @Body request: AddReportNoteRequestDto
    ): AdminUserReportDto

    @POST("api/v1/admin/user-reports/{id}/actions")
    suspend fun addAdminUserReportAction(
        @Path("id") id: Long,
        @Body request: ReportActionRequestDto
    ): AdminUserReportDto
}
