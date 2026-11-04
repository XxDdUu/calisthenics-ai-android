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
    @SerializedName("instructions") val instructions: String?
)
