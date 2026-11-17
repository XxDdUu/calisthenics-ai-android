package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.mapper.toDto
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.EnrollGettingStartedRequestDto
import com.calisthenics.ai.data.remote.dto.GettingStartedCampaignEventRequestDto
import com.calisthenics.ai.data.remote.dto.GettingStartedScheduleConflictDto
import com.calisthenics.ai.data.remote.dto.UpdateDismissalStatusRequestDto
import com.calisthenics.ai.domain.model.AdminProgramDraft
import com.calisthenics.ai.domain.model.GettingStartedEnrollPreview
import com.calisthenics.ai.domain.model.GettingStartedEnrollment
import com.calisthenics.ai.domain.model.GettingStartedProgram
import com.calisthenics.ai.domain.model.GettingStartedScheduleConflictException
import com.calisthenics.ai.domain.model.OnboardingStatus
import com.calisthenics.ai.domain.model.WorkoutSession
import com.calisthenics.ai.domain.repository.GettingStartedRepository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GettingStartedRepositoryImpl @Inject constructor(
    private val api: CalisthenicsApi
) : GettingStartedRepository {

    private val gson = Gson()
    private val _enrollmentUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    override val enrollmentUpdates: Flow<Unit> = _enrollmentUpdates.asSharedFlow()

    override suspend fun notifyEnrollmentChanged() {
        _enrollmentUpdates.tryEmit(Unit)
    }

    override suspend fun getAdminProgram(): Result<GettingStartedProgram?> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getAdminGettingStartedProgram()
            if (response.code() == 204 || response.code() == 404 || response.body() == null) {
                null
            } else {
                response.body()!!.toDomain()
            }
        }
    }

    override suspend fun saveAdminProgram(draft: AdminProgramDraft): Result<GettingStartedProgram> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = draft.toDto()
            api.saveAdminGettingStartedProgram(dto).toDomain()
        }
    }

    override suspend fun publishProgram(): Result<GettingStartedProgram> = withContext(Dispatchers.IO) {
        runCatching {
            api.publishGettingStartedProgram().toDomain()
        }
    }

    override suspend fun unpublishProgram(): Result<GettingStartedProgram> = withContext(Dispatchers.IO) {
        runCatching {
            api.unpublishGettingStartedProgram().toDomain()
        }
    }

    override suspend fun deleteProgram(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.deleteGettingStartedProgram()
            if (!response.isSuccessful && response.code() != 404) {
                throw IllegalStateException("Failed to delete program: ${response.code()}")
            }
            Unit
        }
    }

    override suspend fun getPublishedProgram(): Result<GettingStartedProgram?> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getPublishedGettingStartedProgram()
            if (response.code() == 404 || response.code() == 204 || response.body() == null) {
                null
            } else {
                response.body()!!.toDomain()
            }
        }
    }

    override suspend fun updateDismissalStatus(status: OnboardingStatus): Result<GettingStartedProgram> = withContext(Dispatchers.IO) {
        runCatching {
            api.updateGettingStartedDismissalStatus(UpdateDismissalStatusRequestDto(status.name)).toDomain()
        }
    }

    override suspend fun previewEnrollment(startDate: String?): Result<GettingStartedEnrollPreview> = withContext(Dispatchers.IO) {
        runCatching {
            api.previewGettingStartedEnrollment(startDate).toDomain()
        }
    }

    override suspend fun enroll(): Result<GettingStartedEnrollment> =
        enroll(addToWorkoutPlan = false, confirmConflicts = false, startDate = null)

    override suspend fun enroll(
        addToWorkoutPlan: Boolean,
        confirmConflicts: Boolean,
        startDate: String?
    ): Result<GettingStartedEnrollment> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val result = api.enrollGettingStartedProgram(
                    EnrollGettingStartedRequestDto(
                        addToWorkoutPlan = addToWorkoutPlan,
                        confirmConflicts = confirmConflicts,
                        startDate = startDate
                    )
                ).toDomain()
                _enrollmentUpdates.tryEmit(Unit)
                result
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    val rawError = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
                    val parsed = rawError?.let {
                        runCatching { gson.fromJson(it, ScheduleConflictErrorBody::class.java) }.getOrNull()
                    }
                    if (parsed?.code == "SCHEDULE_CONFLICT" || !parsed?.conflicts.isNullOrEmpty()) {
                        throw GettingStartedScheduleConflictException(
                            message = parsed?.message
                                ?: "Your Workout Plan already has workouts scheduled on overlapping dates.",
                            conflicts = parsed?.conflicts.orEmpty().map { it.toDomain() }
                        )
                    }
                }
                throw e
            }
        }
    }

    override suspend fun startDaySession(dayNumber: Int, workoutDate: String?): Result<WorkoutSession> = withContext(Dispatchers.IO) {
        runCatching {
            val session = api.startGettingStartedDay(dayNumber, workoutDate).toDomain()
            _enrollmentUpdates.tryEmit(Unit)
            session
        }
    }

    override suspend fun getUserEnrollments(): Result<List<GettingStartedEnrollment>> = withContext(Dispatchers.IO) {
        runCatching {
            api.getGettingStartedEnrollments().map { it.toDomain() }
        }
    }

    override suspend fun completeDay(dayNumber: Int): Result<GettingStartedEnrollment> = withContext(Dispatchers.IO) {
        runCatching {
            val result = api.completeGettingStartedDay(dayNumber).toDomain()
            _enrollmentUpdates.tryEmit(Unit)
            result
        }
    }

    override suspend fun trackCampaignEvent(eventType: String, programId: Long?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            api.recordGettingStartedCampaignEvent(GettingStartedCampaignEventRequestDto(eventType, programId))
            Unit
        }
    }

    private data class ScheduleConflictErrorBody(
        @SerializedName("code") val code: String? = null,
        @SerializedName("message") val message: String? = null,
        @SerializedName("conflicts") val conflicts: List<GettingStartedScheduleConflictDto>? = null
    )
}
