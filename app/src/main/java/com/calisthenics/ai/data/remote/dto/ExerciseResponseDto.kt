package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExerciseResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("code") val code: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("difficulty") val difficulty: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("targetMuscles") val targetMuscles: String?,
    @SerializedName("instructions") val instructions: String?,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("mediaItems") val mediaItems: List<ExerciseMediaResponseDto>? = null
)

data class ExerciseMediaResponseDto(
    @SerializedName("type") val type: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerializedName("displayOrder") val displayOrder: Int? = null
)
