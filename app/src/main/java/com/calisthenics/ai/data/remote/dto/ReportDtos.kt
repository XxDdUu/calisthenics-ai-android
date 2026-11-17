package com.calisthenics.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateUserReportRequestDto(
    @SerializedName("reason") val reason: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("evidenceUrls") val evidenceUrls: List<String>? = null
)

data class UserReportCreatedDto(
    @SerializedName("id") val id: Long,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class UserReportSummaryDto(
    @SerializedName("total") val total: Long? = null,
    @SerializedName("pending") val pending: Long? = null,
    @SerializedName("underReview") val underReview: Long? = null,
    @SerializedName("resolved") val resolved: Long? = null,
    @SerializedName("dismissed") val dismissed: Long? = null
)

data class ReportUserRefDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("email") val email: String? = null
)

data class AdminUserReportDto(
    @SerializedName("id") val id: Long,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("reporter") val reporter: ReportUserRefDto? = null,
    @SerializedName("reportedUser") val reportedUser: ReportUserRefDto? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    // detail-only fields
    @SerializedName("description") val description: String? = null,
    @SerializedName("evidenceUrls") val evidenceUrls: List<String>? = null,
    @SerializedName("handledByAdminId") val handledByAdminId: Long? = null,
    @SerializedName("reviewedAt") val reviewedAt: String? = null,
    @SerializedName("resolvedAt") val resolvedAt: String? = null,
    @SerializedName("previousReports") val previousReports: List<PreviousUserReportDto>? = null,
    @SerializedName("actions") val actions: List<UserReportActionDto>? = null
)

data class PreviousUserReportDto(
    @SerializedName("id") val id: Long,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class UserReportActionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String? = null,
    @SerializedName("fromStatus") val fromStatus: String? = null,
    @SerializedName("toStatus") val toStatus: String? = null,
    @SerializedName("moderationAction") val moderationAction: String? = null,
    @SerializedName("suspendedUntil") val suspendedUntil: String? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("adminId") val adminId: Long? = null,
    @SerializedName("adminName") val adminName: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

data class UpdateReportStatusRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("note") val note: String? = null
)

data class AddReportNoteRequestDto(
    @SerializedName("note") val note: String
)

data class ReportActionRequestDto(
    @SerializedName("action") val action: String,
    @SerializedName("suspendedUntil") val suspendedUntil: String? = null,
    @SerializedName("note") val note: String? = null
)
