package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProfileMediaDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("profileType") val profileType: String,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("url") val url: String,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("displayOrder") val displayOrder: Int = 0,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class ReorderProfileMediaRequestDto(
    @SerializedName("profileType") val profileType: String,
    @SerializedName("mediaIds") val mediaIds: List<Long>
)
