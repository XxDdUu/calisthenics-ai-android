package com.calisthenics.ai.data.repository

import com.calisthenics.ai.data.mapper.toDomain
import com.calisthenics.ai.data.remote.api.CalisthenicsApi
import com.calisthenics.ai.data.remote.dto.ClientProfileRequestDto
import com.calisthenics.ai.data.remote.dto.CoachProfileRequestDto
import com.calisthenics.ai.domain.model.ClientProfile
import com.calisthenics.ai.domain.model.ClientTrainingOverview
import com.calisthenics.ai.domain.model.CoachProfile
import com.calisthenics.ai.domain.repository.ProfileRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val api: CalisthenicsApi
) : ProfileRepository {

    override suspend fun getMyCoachProfile(): Result<CoachProfile> = runCatching {
        api.getCoachProfileMe().toDomain()
    }

    override suspend fun updateMyCoachProfile(request: CoachProfileRequestDto): Result<CoachProfile> = runCatching {
        api.updateCoachProfileMe(request).toDomain()
    }

    override suspend fun getPublicCoachProfile(id: Long): Result<CoachProfile> = runCatching {
        api.getCoachProfile(id).toDomain()
    }

    override suspend fun getMyClientProfile(): Result<ClientProfile> = runCatching {
        api.getClientProfileMe().toDomain()
    }

    override suspend fun updateMyClientProfile(request: ClientProfileRequestDto): Result<ClientProfile> = runCatching {
        api.updateClientProfileMe(request).toDomain()
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
        val mediaType = "image/*".toMediaTypeOrNull()
        val requestBody = file.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        api.uploadFile(part).fileDownloadUri
    }
}
