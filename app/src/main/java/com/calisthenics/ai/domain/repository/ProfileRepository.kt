package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.ClientTrainingOverview
import com.calisthenics.ai.domain.model.CoachProfile
import java.io.File

interface ProfileRepository {

    suspend fun getMyCoachProfile(): Result<CoachProfile>

    suspend fun updateMyCoachProfile(request: CoachProfileRequestDto): Result<CoachProfile>

    suspend fun getPublicCoachProfile(id: Long): Result<CoachProfile>

    suspend fun getMyClientProfile(): Result<ClientProfile>

    suspend fun updateMyClientProfile(request: ClientProfileRequestDto): Result<ClientProfile>

    suspend fun getClientProfile(id: Long): Result<ClientProfile>

    suspend fun getClientOverview(id: Long): Result<ClientTrainingOverview>

    suspend fun getMyClientOverview(): Result<ClientTrainingOverview>

    suspend fun uploadAvatar(file: File): Result<String>
}
