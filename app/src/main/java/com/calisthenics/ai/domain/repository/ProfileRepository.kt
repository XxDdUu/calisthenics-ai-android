package com.calisthenics.ai.domain.repository

import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.ClientTrainingOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

interface ProfileRepository {

    val profileUpdateEvents: SharedFlow<ClientProfile>
        get() = MutableSharedFlow()

    suspend fun getMyCoachProfile(): Result<CoachProfile>

    suspend fun updateMyCoachProfile(request: CoachProfileRequestDto): Result<CoachProfile>

    suspend fun getPublicCoachProfile(id: Long): Result<CoachProfile>

    suspend fun getMyClientProfile(): Result<ClientProfile>

    suspend fun updateMyClientProfile(request: ClientProfileRequestDto): Result<ClientProfile>

    suspend fun getClientProfile(id: Long): Result<ClientProfile>

    suspend fun getClientOverview(id: Long): Result<ClientTrainingOverview>

    suspend fun getMyClientOverview(): Result<ClientTrainingOverview>

    suspend fun uploadAvatar(file: File): Result<String>

    suspend fun getProfileMedia(userId: Long, profileType: ProfileType): Result<List<ProfileMediaItem>>

    suspend fun uploadProfileMedia(profileType: ProfileType, file: File, thumbnail: File? = null, mimeType: String = "image/jpeg"): Result<ProfileMediaItem>

    suspend fun deleteProfileMedia(id: Long): Result<Unit>

    suspend fun reorderProfileMedia(profileType: ProfileType, mediaIds: List<Long>): Result<List<ProfileMediaItem>>

    suspend fun setProfileMediaCover(id: Long): Result<List<ProfileMediaItem>>
}
