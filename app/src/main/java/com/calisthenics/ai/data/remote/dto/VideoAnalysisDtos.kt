package com.calisthenics.ai.data.remote.dto

import com.calisthenics.ai.domain.model.*
import com.google.gson.annotations.SerializedName

data class QuotaEntitlementDto(
    @SerializedName(value = "userId", alternate = ["user_id"]) val userId: Long? = null,
    @SerializedName(value = "membershipTier", alternate = ["membership_tier"]) val membershipTier: String? = "FREE",
    @SerializedName(value = "dailyLimit", alternate = ["daily_limit"]) val dailyLimit: Int? = null,
    @SerializedName(value = "usedToday", alternate = ["used_today"]) val usedToday: Int? = 0,
    @SerializedName(value = "remainingToday", alternate = ["remaining_today"]) val remainingToday: Int? = null,
    @SerializedName(value = "liveAnalysisUnlocked", alternate = ["live_analysis_unlocked"]) val liveAnalysisUnlocked: Boolean? = false,
    @SerializedName("timezone") val timezone: String? = "Asia/Ho_Chi_Minh",
    @SerializedName(value = "quotaDate", alternate = ["quota_date"]) val quotaDate: String? = null,
    @SerializedName(value = "resetsAt", alternate = ["resets_at"]) val resetsAt: String? = null,
    @SerializedName(value = "unlimited", alternate = ["is_unlimited", "isUnlimited"]) val unlimited: Boolean? = null,
    @SerializedName(value = "grantSource", alternate = ["grant_source"]) val grantSource: String? = null,
    @SerializedName(value = "adminGrantedPro", alternate = ["admin_granted_pro"]) val adminGrantedPro: Boolean? = null
) {
    fun toDomain(): QuotaEntitlement {
        val tier = membershipTier?.takeIf { it.isNotBlank() } ?: "FREE"
        val isAdminTier = tier.equals("ADMIN", ignoreCase = true)
        val isUnlimitedTier = isAdminTier || (unlimited == true) || (dailyLimit != null && dailyLimit < 0)
        val used = (usedToday ?: 0).coerceAtLeast(0)
        val limit = if (isUnlimitedTier) null else (dailyLimit ?: if (tier.equals("PRO", ignoreCase = true)) 10 else 1)
        val remaining = if (isUnlimitedTier) null else (remainingToday ?: (limit?.minus(used) ?: 0)).coerceAtLeast(0)
        return QuotaEntitlement(
            userId = userId ?: 0L,
            membershipTier = tier.uppercase(),
            dailyLimit = limit,
            usedToday = used,
            remainingToday = remaining,
            liveAnalysisUnlocked = if (isAdminTier) true else (liveAnalysisUnlocked ?: tier.equals("PRO", ignoreCase = true)),
            timezone = timezone?.takeIf { it.isNotBlank() } ?: "Asia/Ho_Chi_Minh",
            quotaDate = quotaDate,
            resetsAt = resetsAt,
            isUnlimited = isUnlimitedTier,
            grantSource = grantSource,
            adminGrantedPro = adminGrantedPro == true
        )
    }
}

data class UpdateMembershipRequestDto(
    @SerializedName("membershipTier") val membershipTier: String
)

data class RepBreakdownDto(
    @SerializedName(value = "repNumber", alternate = ["rep_number", "repIndex", "index"]) val repNumber: Int? = null,
    @SerializedName(value = "valid", alternate = ["isValid", "is_valid"]) val valid: Boolean? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName(value = "formScore", alternate = ["form_score", "score"]) val formScore: Double? = null,
    @SerializedName(value = "startTimestampMs", alternate = ["start_timestamp_ms", "startMs", "timestampMs"]) val startTimestampMs: Long? = null,
    @SerializedName(value = "endTimestampMs", alternate = ["end_timestamp_ms", "endMs"]) val endTimestampMs: Long? = null,
    @SerializedName(value = "durationMs", alternate = ["duration_ms"]) val durationMs: Long? = null,
    @SerializedName(value = "minAngleDeg", alternate = ["min_angle_deg", "minAngle", "minJointAngle", "minKneeAngle", "minElbowAngle"]) val minAngleDeg: Double? = null,
    @SerializedName(value = "romDeg", alternate = ["rom_deg", "rom", "rangeOfMotionDeg"]) val romDeg: Double? = null,
    @SerializedName(value = "depthClassification", alternate = ["depth_classification", "depth", "depthRating"]) val depthClassification: String? = null,
    @SerializedName(value = "issues", alternate = ["detectedErrors", "errors"]) val issues: List<String>? = null,
    @SerializedName("feedback") val feedback: String? = null
) {
    fun toDomain(defaultRepNumber: Int = 1): RepBreakdown {
        val resolvedValid = valid ?: status?.equals("VALID", ignoreCase = true) ?: false
        val resolvedStatus = status?.takeIf { it.isNotBlank() }?.uppercase()
            ?: if (resolvedValid) "VALID" else "INCOMPLETE"
        return RepBreakdown(
            repNumber = repNumber ?: defaultRepNumber,
            valid = resolvedValid,
            status = resolvedStatus,
            formScore = formScore,
            startTimestampMs = startTimestampMs ?: 0L,
            endTimestampMs = endTimestampMs,
            durationMs = durationMs,
            minAngleDeg = minAngleDeg,
            romDeg = romDeg,
            depthClassification = depthClassification,
            issues = issues ?: emptyList(),
            feedback = feedback
        )
    }
}

data class MovementMetricDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName(value = "value", alternate = ["measuredValue", "measured_value"]) val value: Double? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName(value = "targetRange", alternate = ["target_range", "target"]) val targetRange: String? = null,
    @SerializedName("score") val score: Double? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName(value = "reason", alternate = ["unavailableReason", "unavailable_reason", "message"]) val reason: String? = null
) {
    fun toDomain(key: String): MovementMetric {
        val prettifiedName = label?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: key.replace("_", " ").split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
        val resolvedStatus = status?.takeIf { it.isNotBlank() }?.uppercase()
            ?: if (value == null) "UNAVAILABLE" else "VALID"
        return MovementMetric(
            key = key,
            name = prettifiedName,
            value = value,
            unit = unit.orEmpty(),
            targetRange = targetRange,
            score = score,
            confidence = confidence,
            status = resolvedStatus,
            unavailableReason = reason
        )
    }
}

data class StructuredIssueDto(
    @SerializedName(value = "code", alternate = ["id"]) val code: String? = null,
    @SerializedName(value = "title", alternate = ["errorName", "name", "metric"]) val title: String? = null,
    @SerializedName(value = "message", alternate = ["description", "detail"]) val message: String? = null,
    @SerializedName("severity") val severity: String? = null,
    @SerializedName(value = "affectedReps", alternate = ["affected_reps", "repNumbers", "rep_numbers"]) val affectedReps: List<Int>? = null,
    @SerializedName(value = "recommendation", alternate = ["suggestedCorrection", "suggested_correction", "correction", "cue"]) val recommendation: String? = null,
    @SerializedName(value = "measuredValue", alternate = ["measured_value"]) val measuredValue: Double? = null,
    @SerializedName(value = "targetRange", alternate = ["target_range"]) val targetRange: String? = null,
    @SerializedName(value = "timestampsMs", alternate = ["timestamps_ms"]) val timestampsMs: List<Long>? = null
) {
    fun toDomain(): StructuredIssue {
        val resolvedTitle = title?.takeIf { it.isNotBlank() }
            ?: code?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() }
            ?: "Form Issue"
        return StructuredIssue(
            code = code.orEmpty(),
            title = resolvedTitle,
            message = message ?: resolvedTitle,
            severity = IssueSeverity.fromString(severity),
            affectedReps = affectedReps ?: emptyList(),
            recommendation = recommendation,
            measuredValue = measuredValue,
            targetRange = targetRange,
            timestampsMs = timestampsMs ?: emptyList()
        )
    }
}

data class CameraQualityDto(
    @SerializedName(value = "overallQuality", alternate = ["overall_quality", "quality"]) val overallQuality: String? = null,
    @SerializedName(value = "viewAngle", alternate = ["view_angle", "detectedView", "detected_view"]) val viewAngle: String? = null,
    @SerializedName(value = "fullBodyVisibleRatio", alternate = ["full_body_visible_ratio", "visibilityRatio"]) val fullBodyVisibleRatio: Double? = null,
    @SerializedName(value = "averageConfidence", alternate = ["average_confidence", "avgConfidence", "poseConfidence"]) val averageConfidence: Double? = null,
    @SerializedName("warnings") val warnings: List<String>? = null,
    @SerializedName(value = "sideViewDetected", alternate = ["side_view_detected"]) val sideViewDetected: Boolean? = null
) {
    fun toDomain(): CameraQuality {
        return CameraQuality(
            overallQuality = overallQuality?.takeIf { it.isNotBlank() }?.uppercase() ?: "UNKNOWN",
            viewAngle = viewAngle?.takeIf { it.isNotBlank() } ?: "UNKNOWN",
            fullBodyVisibleRatio = fullBodyVisibleRatio,
            averageConfidence = averageConfidence,
            warnings = warnings ?: emptyList(),
            sideViewDetected = sideViewDetected
        )
    }
}

data class VideoAnalysisResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("exerciseId") val exerciseId: Long? = null,
    @SerializedName("exerciseName") val exerciseName: String? = null,
    @SerializedName("status") val status: String? = null, // PENDING, PROCESSING, COMPLETED, FAILED
    @SerializedName("overallScore") val overallScore: Double? = null,
    @SerializedName("formAccuracy") val formAccuracy: Double? = null,
    @SerializedName("stabilityScore") val stabilityScore: Double? = null,
    @SerializedName("alignmentScore") val alignmentScore: Double? = null,
    @SerializedName("durationMs") val durationMs: Long? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("fps") val fps: Double? = null,
    @SerializedName("originalVideoUrl") val originalVideoUrl: String? = null,
    @SerializedName("annotatedVideoUrl") val annotatedVideoUrl: String? = null,
    @SerializedName("keypointsUrl") val keypointsUrl: String? = null,
    @SerializedName("metrics") val metrics: Map<String, Any?>? = null,
    @SerializedName("feedback") val feedback: List<String>? = null,
    @SerializedName("errorMessage") val errorMessage: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName(value = "schemaVersion", alternate = ["schema_version"]) val schemaVersion: String? = null,
    @SerializedName(value = "exerciseCode", alternate = ["exercise_code"]) val exerciseCode: String? = null,
    @SerializedName(value = "repCount", alternate = ["rep_count"]) val repCount: Int? = null,
    @SerializedName(value = "validReps", alternate = ["valid_reps"]) val validReps: Int? = null,
    @SerializedName(value = "incompleteReps", alternate = ["incomplete_reps"]) val incompleteReps: Int? = null,
    @SerializedName("reps") val reps: List<RepBreakdownDto>? = null,
    @SerializedName(value = "movementMetrics", alternate = ["movement_metrics"]) val movementMetrics: Map<String, MovementMetricDto>? = null,
    @SerializedName("issues") val issues: List<StructuredIssueDto>? = null,
    @SerializedName("recommendations") val recommendations: List<String>? = null,
    @SerializedName(value = "cameraQuality", alternate = ["camera_quality"]) val cameraQuality: CameraQualityDto? = null,
    @SerializedName("quota") val quota: QuotaEntitlementDto? = null
) {
    fun toDomain(): AnalysisResult {
        val meta = if ((durationMs ?: 0L) > 0 || (width ?: 0) > 0) {
            VideoMetadata(
                durationMs = durationMs ?: 0L,
                width = width ?: 0,
                height = height ?: 0,
                originalFps = fps ?: 30.0,
                totalFrames = if (fps != null && durationMs != null) ((durationMs / 1000.0) * fps).toInt() else 0,
                analyzedFrames = (metrics?.get("analyzedFrames") as? Number)?.toInt() ?: 0,
                samplingFps = (metrics?.get("samplingFps") as? Number)?.toDouble() ?: 10.0,
                fileSizeBytes = 0L
            )
        } else null

        val compScores = ComponentScores(
            bodyAlignment = alignmentScore,
            elbowExtension = (metrics?.get("elbow_angle") as? Number)?.toDouble(),
            shoulderPosition = (metrics?.get("shoulder_angle") as? Number)?.toDouble(),
            hipExtension = (metrics?.get("hip_angle") as? Number)?.toDouble(),
            stability = stabilityScore,
            overall = overallScore
        )

        val mappedReps = reps?.mapIndexed { index, rep ->
            rep.toDomain(defaultRepNumber = index + 1)
        } ?: emptyList()
        val resolvedRepCount = repCount ?: mappedReps.size
        val resolvedValidReps = validReps ?: mappedReps.count { it.valid }
        val resolvedIncompleteReps = incompleteReps ?: (resolvedRepCount - resolvedValidReps).coerceAtLeast(0)
        val resolvedExerciseName = exerciseName?.takeIf { it.isNotBlank() }
            ?: exerciseCode?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() }
            ?: "Exercise"

        return AnalysisResult(
            id = id,
            userId = userId,
            exerciseId = exerciseId,
            exerciseName = resolvedExerciseName,
            mediaUrl = annotatedVideoUrl ?: originalVideoUrl ?: "",
            mediaType = "VIDEO",
            overallScore = overallScore,
            holdDurationSeconds = durationMs?.let { it / 1000.0 },
            formFeedbackList = feedback ?: emptyList(),
            elbowAngle = (metrics?.get("elbow_angle") as? Number)?.toDouble(),
            bodyLineAngle = (metrics?.get("body_line_angle") as? Number)?.toDouble(),
            shoulderLeanAngle = (metrics?.get("shoulder_angle") as? Number)?.toDouble(),
            rawKeypointsJson = null,
            createdAt = createdAt ?: "",
            videoMetadata = meta,
            componentScores = compScores,
            stabilityMetrics = stabilityScore?.let {
                StabilityMetrics(
                    averageBodyLineDeviation = 0.0,
                    maxBodyLineDeviation = 0.0,
                    shoulderPositionVariation = 0.0,
                    hipPositionVariation = 0.0,
                    averagePoseConfidence = cameraQuality?.averageConfidence ?: 0.9,
                    stabilityScore = it
                )
            },
            timeline = emptyList(),
            originalVideoUrl = originalVideoUrl,
            annotatedVideoUrl = annotatedVideoUrl,
            status = status ?: "COMPLETED",
            keypointsUrl = keypointsUrl,
            schemaVersion = schemaVersion,
            exerciseCode = exerciseCode,
            formAccuracy = formAccuracy,
            stabilityScore = stabilityScore,
            alignmentScore = alignmentScore,
            repCount = resolvedRepCount,
            validReps = resolvedValidReps,
            incompleteReps = resolvedIncompleteReps,
            reps = mappedReps,
            movementMetrics = movementMetrics?.mapValues { (key, dto) -> dto.toDomain(key) } ?: emptyMap(),
            issues = issues?.map { it.toDomain() } ?: emptyList(),
            recommendations = recommendations ?: emptyList(),
            cameraQuality = cameraQuality?.toDomain(),
            quota = quota?.toDomain(),
            errorMessage = errorMessage,
            updatedAt = updatedAt
        )
    }
}

data class VideoAnalysisKeypointsDto(
    @SerializedName("analysisId") val analysisId: Long,
    @SerializedName("format") val format: String? = "COCO_17",
    @SerializedName("coordinateSystem") val coordinateSystem: String? = "NORMALIZED",
    @SerializedName("totalFrames") val totalFrames: Int = 0,
    @SerializedName("frames") val frames: List<VideoKeypointFrameDto>? = null
) {
    fun toDomain(): VideoKeypointsDataModel {
        return VideoKeypointsDataModel(
            analysisId = analysisId,
            format = format ?: "COCO_17",
            coordinateSystem = coordinateSystem ?: "NORMALIZED",
            totalFrames = totalFrames,
            frames = frames?.map { it.toDomain() } ?: emptyList()
        )
    }
}

data class VideoKeypointFrameDto(
    @SerializedName("frameIndex") val frameIndex: Int,
    @SerializedName("timestampMs") val timestampMs: Long,
    @SerializedName("keypoints") val keypoints: List<VideoKeypointItemDto>? = null
) {
    fun toDomain(): KeypointFrameModel {
        return KeypointFrameModel(
            frameIndex = frameIndex,
            timestampMs = timestampMs,
            keypoints = keypoints?.map { it.toDomain() } ?: emptyList()
        )
    }
}

data class VideoKeypointItemDto(
    @SerializedName("name") val name: String,
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("confidence") val confidence: Float
) {
    fun toDomain(): KeypointItemModel {
        return KeypointItemModel(
            name = name,
            x = x,
            y = y,
            confidence = confidence
        )
    }
}
