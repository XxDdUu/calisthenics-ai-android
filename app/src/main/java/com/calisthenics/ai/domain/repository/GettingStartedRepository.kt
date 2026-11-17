package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.AdminProgramDraft
import com.calisthenics.ai.domain.model.GettingStartedEnrollPreview
import com.calisthenics.ai.domain.model.GettingStartedEnrollment
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.OnboardingStatus
import com.calisthenics.ai.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface GettingStartedRepository {
    val enrollmentUpdates: Flow<Unit>
        get() = emptyFlow()

    suspend fun notifyEnrollmentChanged() {}

    suspend fun getAdminProgram(): Result<GettingStartedProgram?>
    suspend fun saveAdminProgram(draft: AdminProgramDraft): Result<GettingStartedProgram>
    suspend fun publishProgram(): Result<GettingStartedProgram>
    suspend fun unpublishProgram(): Result<GettingStartedProgram>
    suspend fun deleteProgram(): Result<Unit>

    suspend fun getPublishedProgram(): Result<GettingStartedProgram?>
    suspend fun updateDismissalStatus(status: OnboardingStatus): Result<GettingStartedProgram>
    suspend fun previewEnrollment(startDate: String? = null): Result<GettingStartedEnrollPreview> =
        Result.failure(UnsupportedOperationException("Enrollment preview is not supported"))
    suspend fun enroll(): Result<GettingStartedEnrollment>
    suspend fun enroll(
        addToWorkoutPlan: Boolean,
        confirmConflicts: Boolean = false,
        startDate: String? = null
    ): Result<GettingStartedEnrollment> = enroll()
    suspend fun startDaySession(dayNumber: Int, workoutDate: String? = null): Result<WorkoutSession> =
        Result.failure(UnsupportedOperationException("Starting a Getting Started day session is not supported"))
    suspend fun getUserEnrollments(): Result<List<GettingStartedEnrollment>> = Result.success(emptyList())
    suspend fun completeDay(dayNumber: Int): Result<GettingStartedEnrollment>
    suspend fun trackCampaignEvent(eventType: String, programId: Long? = null): Result<Unit>
}

internal object NoOpGettingStartedRepository : GettingStartedRepository {
    override suspend fun getAdminProgram(): Result<GettingStartedProgram?> = Result.success(null)
    override suspend fun saveAdminProgram(draft: AdminProgramDraft): Result<GettingStartedProgram> =
        Result.failure(UnsupportedOperationException())
    override suspend fun publishProgram(): Result<GettingStartedProgram> =
        Result.failure(UnsupportedOperationException())
    override suspend fun unpublishProgram(): Result<GettingStartedProgram> =
        Result.failure(UnsupportedOperationException())
    override suspend fun deleteProgram(): Result<Unit> = Result.success(Unit)
    override suspend fun getPublishedProgram(): Result<GettingStartedProgram?> = Result.success(null)
    override suspend fun updateDismissalStatus(status: OnboardingStatus): Result<GettingStartedProgram> =
        Result.failure(UnsupportedOperationException())
    override suspend fun enroll(): Result<GettingStartedEnrollment> =
        Result.failure(UnsupportedOperationException())
    override suspend fun completeDay(dayNumber: Int): Result<GettingStartedEnrollment> =
        Result.failure(UnsupportedOperationException())
    override suspend fun trackCampaignEvent(eventType: String, programId: Long?): Result<Unit> =
        Result.success(Unit)
}
