package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.local.GenderSessionStore
import com.calisthenics.ai.data.local.TokenManager
import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.data.remote.dto.ReorderProfileMediaRequestDto
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.ClientTrainingOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.model.ProfileMediaItem
import com.calisthenics.ai.domain.model.ProfileType
import com.calisthenics.ai.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: CalisthenicsApi,
    private val tokenManager: TokenManager,
    private val genderSessionStore: GenderSessionStore
) : ProfileRepository {

    private val _profileUpdateEvents = MutableSharedFlow<ClientProfile>(replay = 1, extraBufferCapacity = 1)
    override val profileUpdateEvents: SharedFlow<ClientProfile> = _profileUpdateEvents.asSharedFlow()

    override suspend fun getMyCoachProfile(): Result<CoachProfile> = runCatching {
        api.getCoachProfileMe().toDomain().also { profile ->
            tokenManager.saveAccountGender(profile.userId, profile.gender?.apiValue)
            genderSessionStore.publish(profile.userId, profile.gender, isBackendConfirmed = true)
        }
    }

    override suspend fun updateMyCoachProfile(request: CoachProfileRequestDto): Result<CoachProfile> = runCatching {
        api.updateCoachProfileMe(request).toDomain().also { profile ->
            tokenManager.saveAccountGender(profile.userId, profile.gender?.apiValue)
            genderSessionStore.publish(profile.userId, profile.gender, isBackendConfirmed = true)
        }
    }

    override suspend fun getPublicCoachProfile(id: Long): Result<CoachProfile> = runCatching {
        api.getCoachProfile(id).toDomain()
    }

    override suspend fun getMyClientProfile(): Result<ClientProfile> = runCatching {
        api.getClientProfileMe().toDomain().also { profile ->
            tokenManager.saveAccountGender(profile.userId, profile.gender?.apiValue)
            genderSessionStore.publish(profile.userId, profile.gender, isBackendConfirmed = true)
            _profileUpdateEvents.tryEmit(profile)
        }
    }

    override suspend fun updateMyClientProfile(request: ClientProfileRequestDto): Result<ClientProfile> = runCatching {
        api.updateClientProfileMe(request).toDomain().also { profile ->
            tokenManager.saveAccountGender(profile.userId, profile.gender?.apiValue)
            genderSessionStore.publish(profile.userId, profile.gender, isBackendConfirmed = true)
            _profileUpdateEvents.tryEmit(profile)
        }
    }

    override suspend fun getClientProfile(id: Long): Result<ClientProfile> = runCatching {
        api.getClientProfile(id).toDomain()
    }

    override suspend fun getClientOverview(id: Long): Result<ClientTrainingOverview> = runCatching {
        api.getClientOverview(id).toDomain()
    }

    override suspend fun getMyClientOverview(): Result<ClientTrainingOverview> = runCatching {
        api.getClientMyOverview().toDomain()
    }

    override suspend fun uploadAvatar(file: File): Result<String> = runCatching {
        val mediaType = "image/jpeg".toMediaType()
        val requestBody = file.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        api.uploadAvatar(part).avatarUrl
    }

    override suspend fun getProfileMedia(userId: Long, profileType: ProfileType): Result<List<ProfileMediaItem>> = runCatching {
        api.getProfileMedia(userId, profileType.name).map { it.toDomain() }
    }

    override suspend fun uploadProfileMedia(
        profileType: ProfileType,
        file: File,
        thumbnail: File?,
        mimeType: String
    ): Result<ProfileMediaItem> = runCatching {
        val fileBody = file.asRequestBody(mimeType.toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)
        val thumbPart = thumbnail?.let {
            val thumbBody = it.asRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("thumbnail", it.name, thumbBody)
        }
        api.uploadProfileMedia(profileType.name, filePart, thumbPart).toDomain()
    }

    override suspend fun deleteProfileMedia(id: Long): Result<Unit> = runCatching {
        val response = api.deleteProfileMedia(id)
        if (!response.isSuccessful) {
            throw retrofit2.HttpException(response)
        }
    }

    override suspend fun reorderProfileMedia(
        profileType: ProfileType,
        mediaIds: List<Long>
    ): Result<List<ProfileMediaItem>> = runCatching {
        api.reorderProfileMedia(ReorderProfileMediaRequestDto(profileType.name, mediaIds)).map { it.toDomain() }
    }

    override suspend fun setProfileMediaCover(id: Long): Result<List<ProfileMediaItem>> = runCatching {
        api.setProfileMediaCover(id).map { it.toDomain() }
    }
}
