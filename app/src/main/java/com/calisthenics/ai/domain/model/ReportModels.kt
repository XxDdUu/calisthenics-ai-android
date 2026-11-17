package com.calisthenics.ai.domain.model

const val MAX_REPORT_DESCRIPTION_LENGTH = 2000
const val MAX_REPORT_EVIDENCE = 5

enum class ReportReason {
    HARASSMENT,
    INAPPROPRIATE_BEHAVIOR,
    SPAM_OR_SCAM,
    ABUSIVE_LANGUAGE,
    FAKE_ACCOUNT,
    INAPPROPRIATE_PROFILE_CONTENT,
    FRAUD,
    OTHER;

    companion object {
        fun fromApi(value: String?): ReportReason? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

enum class UserReportStatus {
    PENDING, UNDER_REVIEW, RESOLVED, DISMISSED, UNKNOWN;

    companion object {
        fun fromApi(value: String?): UserReportStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class UserReportActionType {
    STATUS_CHANGE, NOTE, MODERATION_ACTION, UNKNOWN;

    companion object {
        fun fromApi(value: String?): UserReportActionType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

enum class ModerationAction {
    WARNING, TEMPORARY_SUSPENSION, PERMANENT_BAN;

    companion object {
        fun fromApi(value: String?): ModerationAction? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

data class UserReportDraft(
    val reason: ReportReason? = null,
    val description: String = "",
    val evidenceUrls: List<String> = emptyList()
) {
    val isValid: Boolean
        get() = reason != null &&
            description.length <= MAX_REPORT_DESCRIPTION_LENGTH &&
            evidenceUrls.size <= MAX_REPORT_EVIDENCE
}

data class UserReportReceipt(val id: Long, val status: UserReportStatus, val createdAt: String?)

data class UserReportSummary(
    val total: Long,
    val pending: Long,
    val underReview: Long,
    val resolved: Long,
    val dismissed: Long
)

data class ReportUserRef(val id: Long?, val fullName: String, val email: String?)

data class PreviousUserReport(
    val id: Long,
    val reason: ReportReason?,
    val status: UserReportStatus,
    val createdAt: String?
)

data class UserReportAction(
    val id: Long,
    val type: UserReportActionType,
    val fromStatus: UserReportStatus?,
    val toStatus: UserReportStatus?,
    val moderationAction: ModerationAction?,
    val suspendedUntil: String?,
    val note: String?,
    val adminId: Long?,
    val adminName: String?,
    val createdAt: String?
)

data class AdminUserReport(
    val id: Long,
    val reason: ReportReason?,
    val status: UserReportStatus,
    val reporter: ReportUserRef,
    val reportedUser: ReportUserRef,
    val createdAt: String?,
    val updatedAt: String?,
    val description: String? = null,
    val evidenceUrls: List<String> = emptyList(),
    val handledByAdminId: Long? = null,
    val reviewedAt: String? = null,
    val resolvedAt: String? = null,
    val previousReports: List<PreviousUserReport> = emptyList(),
    val actions: List<UserReportAction> = emptyList()
)
