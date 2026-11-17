package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.CreateClientCoachRequestDto
import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachConnectionOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.repository.CoachClientRepository
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import javax.inject.Inject

class CoachClientRepositoryImpl private constructor(
    private val api: CalisthenicsApi,
    private val workoutPlanRepository: WorkoutPlanRepository?,
    @Suppress("UNUSED_PARAMETER") unused: Boolean
) : CoachClientRepository {

    @Inject
    constructor(
        api: CalisthenicsApi,
        workoutPlanRepository: WorkoutPlanRepository
    ) : this(api, workoutPlanRepository, false)

    constructor(api: CalisthenicsApi) : this(api, null, false)

    override suspend fun loadOverview(): Result<CoachConnectionOverview> = runCatching {
        val currentUserId = api.getCurrentUser().id
        val coaches = api.getCoaches().map { it.toDomain() }
        val relationships = api.getCoachClientRelationships().map { it.toDomain() }
        CoachConnectionOverview(currentUserId, coaches, relationships)
    }

    override suspend fun loadCoaches(): Result<List<CoachProfile>> = runCatching {
        api.getCoaches().map { it.toDomain() }
    }

    override suspend fun loadRelationships(): Result<List<CoachClientRelationship>> = runCatching {
        api.getCoachClientRelationships().map { it.toDomain() }
    }

    override suspend fun getCoachProfile(id: Long): Result<CoachProfile> = runCatching {
        api.getCoachProfile(id).toDomain()
    }

    override suspend fun requestConnection(coachId: Long): Result<CoachClientRelationship> = runCatching {
        api.requestCoachConnection(CreateClientCoachRequestDto(coachId)).toDomain()
    }

    override suspend fun acceptRequest(relationshipId: Long): Result<CoachClientRelationship> = runCatching {
        api.acceptCoachConnection(relationshipId).toDomain()
    }

    override suspend fun rejectRequest(relationshipId: Long): Result<CoachClientRelationship> = runCatching {
        api.rejectCoachConnection(relationshipId).toDomain()
    }

    override suspend fun cancelRequest(relationshipId: Long): Result<CoachClientRelationship> = runCatching {
        val cancelled = api.cancelCoachConnection(relationshipId).toDomain()
        workoutPlanRepository?.invalidateCoachAssignedPlans()
        cancelled
    }

    override suspend fun terminateRelationship(relationshipId: Long): Result<CoachClientRelationship> = runCatching {
        val terminated = api.terminateCoachConnection(relationshipId).toDomain()
        workoutPlanRepository?.invalidateCoachAssignedPlans()
        terminated
    }
}
