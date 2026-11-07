package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.domain.model.CoachClientRelationship
import com.calisthenics.ai.domain.model.CoachConnectionOverview
import com.calisthenics.ai.domain.model.CoachProfile

interface CoachClientRepository {
    suspend fun loadOverview(): Result<CoachConnectionOverview>
    suspend fun loadCoaches(): Result<List<CoachProfile>> = Result.success(emptyList())
    suspend fun loadRelationships(): Result<List<CoachClientRelationship>> = Result.success(emptyList())
    suspend fun getCoachProfile(id: Long): Result<CoachProfile>
    suspend fun requestConnection(coachId: Long): Result<CoachClientRelationship>
    suspend fun acceptRequest(relationshipId: Long): Result<CoachClientRelationship>
    suspend fun rejectRequest(relationshipId: Long): Result<CoachClientRelationship>
    suspend fun cancelRequest(relationshipId: Long): Result<CoachClientRelationship>
    suspend fun terminateRelationship(relationshipId: Long): Result<CoachClientRelationship>
}
