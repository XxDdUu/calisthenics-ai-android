package com.calisthenics.ai.ui.liveanalysis

import com.calisthenics.ai.domain.model.DetectedFormError
import com.calisthenics.ai.domain.model.ErrorSeverity
import com.calisthenics.ai.domain.model.ExerciseFormMetric
import com.calisthenics.ai.domain.model.LiveAnalysisResult
import com.calisthenics.ai.domain.model.LiveFeedback
import com.calisthenics.ai.domain.model.LiveRepRecord
import com.calisthenics.ai.domain.model.LiveSessionRecord
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Stateful per-session biomechanical accumulator for Live Analysis.
 * Collects per-frame confidence, exercise-specific scoring metrics,
 * rep timings, and deduplicated form errors without fabricating missing data.
 */
class LiveSessionSummaryAccumulator(
    private var exerciseCode: String,
    private var sessionStartTimeMs: Long = 0L
) {
    private data class PendingError(
        val code: String,
        val errorName: String,
        val severity: ErrorSeverity,
        val timestampOffsetMs: Long,
        val suggestedCorrection: String
    )

    private data class AggregatedErrorState(
        val code: String,
        val errorName: String,
        var occurrences: Int = 0,
        var severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        val timestampsMs: MutableList<Long> = mutableListOf(),
        val repNumbers: MutableSet<Int> = linkedSetOf(),
        var suggestedCorrection: String? = null
    )

    private val validFrameScores = mutableListOf<Double>()
    private val frameConfidences = mutableListOf<Double>()
    private val metricSamples = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

    private val completedReps = mutableListOf<LiveRepRecord>()
    private var lastCountedRep: Int = 0
    private var currentRepStartMs: Long = sessionStartTimeMs
    private val currentRepFrameScores = mutableListOf<Double>()
    private val currentRepErrors = linkedMapOf<String, PendingError>()

    private val aggregatedErrors = linkedMapOf<String, AggregatedErrorState>()
    private val holdErrorCooldownMs = linkedMapOf<String, Long>()
    private var peakHoldDurationSec: Double = 0.0

    fun reset(exerciseCode: String, startTimeMs: Long) {
        this.exerciseCode = exerciseCode.uppercase(Locale.US)
        this.sessionStartTimeMs = startTimeMs
        this.currentRepStartMs = startTimeMs
        this.lastCountedRep = 0
        validFrameScores.clear()
        frameConfidences.clear()
        metricSamples.clear()
        completedReps.clear()
        currentRepFrameScores.clear()
        currentRepErrors.clear()
        aggregatedErrors.clear()
        holdErrorCooldownMs.clear()
        peakHoldDurationSec = 0.0
    }

    fun recordFrame(analysis: LiveAnalysisResult, nowMs: Long, isHoldExercise: Boolean): List<LiveRepRecord> {
        val offsetMs = if (sessionStartTimeMs > 0L) (nowMs - sessionStartTimeMs).coerceAtLeast(0L) else 0L

        analysis.holdDurationSec?.let { hold ->
            if (hold > peakHoldDurationSec) {
                peakHoldDurationSec = hold
            }
        }

        val frameConfidence = analysis.scoreConfidence ?: analysis.confidence
        if (frameConfidence != null && frameConfidence > 0.0 && analysis.poseDetected) {
            frameConfidences.add(frameConfidence.coerceIn(0.0, 1.0))
        }

        if (analysis.status.equals("VALID", ignoreCase = true) || analysis.poseDetected) {
            val score = analysis.score ?: analysis.currentScore
            if (score != null && score > 0.0 && !analysis.status.equals("INCOMPLETE_POSE", ignoreCase = true)) {
                validFrameScores.add(score.coerceIn(0.0, 100.0))
                currentRepFrameScores.add(score.coerceIn(0.0, 100.0))
            }
        }

        // Accumulate real scoring metrics only (never accumulate INSUFFICIENT_DATA / UNAVAILABLE / low-confidence placeholders)
        for (m in analysis.scoringMetrics) {
            val statusUpper = m.status.uppercase(Locale.US)
            if (statusUpper != "INSUFFICIENT_DATA" &&
                statusUpper != "UNAVAILABLE" &&
                m.confidence >= 0.25
            ) {
                val list = metricSamples.getOrPut(m.name.lowercase(Locale.US)) { mutableListOf() }
                list.add(m.value to m.score.coerceIn(0.0, 100.0))
            }
        }

        // Track biomechanical form errors
        for (fb in analysis.feedback) {
            if (!isBiomechanicalFormIssue(fb)) continue
            val code = fb.code.uppercase(Locale.US).ifBlank { normalizeCodeFromMessage(fb.message) }
            val friendlyName = formatErrorName(code, fb.message)
            val severity = ErrorSeverity.fromString(fb.severity)
            val correction = resolveSuggestedCorrection(code, fb.message)

            if (isHoldExercise) {
                // For isometric holds, debounce continuous error reports to 5-second windows
                val lastReportedAt = holdErrorCooldownMs[code] ?: -10_000L
                if (offsetMs - lastReportedAt >= 5000L) {
                    holdErrorCooldownMs[code] = offsetMs
                    recordAggregatedError(
                        code = code,
                        errorName = friendlyName,
                        severity = severity,
                        timestampOffsetMs = offsetMs,
                        repNumber = null,
                        correction = correction
                    )
                }
            } else {
                // For dynamic repetition exercises, deduplicate within the current repetition
                val existingInRep = currentRepErrors[code]
                if (existingInRep == null) {
                    currentRepErrors[code] = PendingError(
                        code = code,
                        errorName = friendlyName,
                        severity = severity,
                        timestampOffsetMs = offsetMs,
                        suggestedCorrection = correction
                    )
                } else if (severityRank(severity) > severityRank(existingInRep.severity)) {
                    currentRepErrors[code] = existingInRep.copy(severity = severity)
                }
            }
        }

        // Process newly completed repetitions
        val newlyCompletedReps = mutableListOf<LiveRepRecord>()
        val currentReps = analysis.repCount ?: 0
        if (currentReps > lastCountedRep) {
            val repStart = if (currentRepStartMs > 0L) currentRepStartMs else nowMs
            val repDurationMs = (nowMs - repStart).coerceAtLeast(250L)
            val repOffsetMs = if (sessionStartTimeMs > 0L) (repStart - sessionStartTimeMs).coerceAtLeast(0L) else 0L
            val computedRepScore = currentRepFrameScores.average()
                .takeIf { !it.isNaN() }
                ?: analysis.score

            val repErrorLabels = mutableListOf<String>()
            for (i in (lastCountedRep + 1)..currentReps) {
                if (i == currentReps) {
                    for (err in currentRepErrors.values) {
                        recordAggregatedError(
                            code = err.code,
                            errorName = err.errorName,
                            severity = err.severity,
                            timestampOffsetMs = err.timestampOffsetMs,
                            repNumber = i,
                            correction = err.suggestedCorrection
                        )
                        repErrorLabels.add(err.errorName)
                    }
                }

                val isValid = (computedRepScore ?: 0.0) >= 70.0 &&
                    !analysis.depth.equals("SHALLOW", ignoreCase = true)
                val completionStatus = when {
                    isValid -> "SUCCESS"
                    analysis.depth.equals("SHALLOW", ignoreCase = true) -> "PARTIAL"
                    else -> "FAILED"
                }

                val repFeedbackText = when {
                    repErrorLabels.isNotEmpty() -> repErrorLabels.joinToString("; ")
                    else -> analysis.feedback.firstOrNull()?.message
                }

                val repRecord = LiveRepRecord(
                    repNumber = i,
                    score = computedRepScore?.let { round1(it) },
                    minKneeAngle = analysis.minKneeAngle,
                    minJointAngle = analysis.minElbowAngle ?: analysis.minKneeAngle,
                    minElbowAngle = analysis.minElbowAngle,
                    depth = analysis.depth,
                    completionStatus = completionStatus,
                    durationMs = repDurationMs,
                    timestampOffsetMs = repOffsetMs,
                    isValid = isValid,
                    feedback = repFeedbackText,
                    detectedErrors = repErrorLabels.toList()
                )
                completedReps.add(repRecord)
                newlyCompletedReps.add(repRecord)
            }

            lastCountedRep = currentReps
            currentRepStartMs = nowMs
            currentRepFrameScores.clear()
            currentRepErrors.clear()
        }

        return newlyCompletedReps
    }

    fun getTotalRepCount(): Int = lastCountedRep.coerceAtLeast(completedReps.size)

    fun buildSessionSnapshot(
        sessionId: String,
        durationSeconds: Double,
        isHoldExercise: Boolean
    ): LiveSessionRecord {
        // Flush any uncompleted-rep errors on stop if user aborted mid-rep so errors aren't silently lost
        if (!isHoldExercise && currentRepErrors.isNotEmpty() && completedReps.isEmpty()) {
            for (err in currentRepErrors.values) {
                recordAggregatedError(
                    code = err.code,
                    errorName = err.errorName,
                    severity = err.severity,
                    timestampOffsetMs = err.timestampOffsetMs,
                    repNumber = null,
                    correction = err.suggestedCorrection
                )
            }
            currentRepErrors.clear()
        }

        val repsSnapshot = completedReps.toList()
        val total = getTotalRepCount()
        val repScores = repsSnapshot.mapNotNull { it.score }

        val avgScore = when {
            repScores.isNotEmpty() -> round1(repScores.average())
            isHoldExercise && validFrameScores.isNotEmpty() -> round1(validFrameScores.average())
            total == 0 && validFrameScores.isNotEmpty() -> round1(validFrameScores.average())
            else -> null
        }

        val overallScore = when {
            validFrameScores.isNotEmpty() && isHoldExercise -> round1(validFrameScores.average())
            repScores.isNotEmpty() -> round1(repScores.average())
            validFrameScores.isNotEmpty() -> round1(validFrameScores.average())
            else -> null
        }

        val peakScore = (repScores.maxOrNull() ?: validFrameScores.maxOrNull())?.let { round1(it) }
        val lowestScore = (repScores.minOrNull() ?: validFrameScores.minOrNull())?.let { round1(it) }

        val confidence = frameConfidences.average().takeIf { !it.isNaN() }?.let { round2(it) }

        val kneeAngles = repsSnapshot.mapNotNull { it.minKneeAngle }
        val avgKneeAngle = kneeAngles.average().takeIf { !it.isNaN() }?.let { round1(it) }
        val deepestKneeAngle = kneeAngles.minOrNull()?.let { round1(it) }

        val jointAngles = repsSnapshot.mapNotNull { it.minJointAngle ?: it.minElbowAngle ?: it.minKneeAngle }
        val avgJointAngle = jointAngles.average().takeIf { !it.isNaN() }?.let { round1(it) }
        val deepestJointAngle = jointAngles.minOrNull()?.let { round1(it) }

        val correct = repsSnapshot.count { it.isValid }
        val incorrect = repsSnapshot.size - correct

        val exerciseMetrics = buildExerciseMetrics(repsSnapshot, isHoldExercise)
        val errors = aggregatedErrors.values.map {
            DetectedFormError(
                code = it.code,
                errorName = it.errorName,
                occurrences = it.occurrences,
                severity = it.severity,
                timestampsMs = it.timestampsMs.toList(),
                repNumbers = it.repNumbers.toList(),
                suggestedCorrection = it.suggestedCorrection
            )
        }.sortedWith(
            compareByDescending<DetectedFormError> { severityRank(it.severity) }
                .thenByDescending { it.occurrences }
        )

        return LiveSessionRecord(
            id = sessionId,
            exerciseType = exerciseCode,
            status = "COMPLETED",
            totalReps = total,
            correctReps = correct,
            incorrectReps = incorrect,
            overallScore = overallScore,
            averageScore = avgScore,
            peakScore = peakScore,
            lowestScore = lowestScore,
            analysisConfidence = confidence,
            peakHoldDurationSeconds = peakHoldDurationSec.takeIf { it > 0.0 }?.let { round1(it) },
            averageKneeAngle = avgKneeAngle,
            deepestKneeAngle = deepestKneeAngle,
            averageJointAngle = avgJointAngle,
            deepestJointAngle = deepestJointAngle,
            durationSeconds = round1(durationSeconds),
            exerciseMetrics = exerciseMetrics,
            detectedErrors = errors,
            aiSummaryStatus = "PENDING",
            reps = repsSnapshot
        )
    }

    private fun buildExerciseMetrics(
        reps: List<LiveRepRecord>,
        isHoldExercise: Boolean
    ): List<ExerciseFormMetric> {
        val code = exerciseCode.uppercase(Locale.US)
        return when {
            code == "PUSH_UP" || code == "PIKE_PUSH_UP" -> listOf(
                metricFromSample(
                    key = "body_alignment",
                    aliases = listOf("body_line"),
                    label = "Body Alignment",
                    unit = "°",
                    unavailableReason = "Full plank line (shoulder-hip-ankle) not visible"
                ),
                metricFromSample(
                    key = "elbow_positioning",
                    aliases = listOf("elbow_movement", "elbow_symmetry"),
                    label = "Elbow Positioning",
                    unit = "°",
                    unavailableReason = "Bilateral elbow tracking not clearly visible"
                ),
                metricFromSample(
                    key = "range_of_motion",
                    aliases = listOf("elbow_angle", "rom"),
                    label = "Range of Motion",
                    unit = "°",
                    unavailableReason = "Elbow flexion depth could not be captured"
                ),
                computeTempoConsistency(reps),
                computeRepConsistency(reps)
            )

            code == "SQUAT" -> listOf(
                metricFromSample(
                    key = "squat_depth",
                    aliases = listOf("knee_angle", "depth"),
                    label = "Squat Depth",
                    unit = "°",
                    unavailableReason = "Hip-knee-ankle landmarks not visible"
                ),
                metricFromSample(
                    key = "knee_alignment",
                    aliases = listOf("knee_symmetry", "knee_valgus"),
                    label = "Knee Alignment",
                    unit = "°",
                    unavailableReason = "Knee alignment landmarks unavailable"
                ),
                metricFromSample(
                    key = "hip_positioning",
                    aliases = listOf("hip_angle", "hip_position"),
                    label = "Hip Positioning",
                    unit = "°",
                    unavailableReason = "Shoulder-hip-knee alignment not visible"
                ),
                metricFromSample(
                    key = "torso_angle",
                    aliases = listOf("torso_position", "trunk_lean"),
                    label = "Torso Angle",
                    unit = "°",
                    unavailableReason = "Torso inclination could not be measured"
                ),
                metricFromSample(
                    key = "balance",
                    aliases = listOf("center_of_mass", "sway"),
                    label = "Balance",
                    unit = "%",
                    unavailableReason = "Foot/ankle balance tracking unavailable"
                )
            )

            code.contains("PLANCHE") -> listOf(
                metricFromSample(
                    key = "shoulder_lean",
                    aliases = listOf("shoulder_angle"),
                    label = "Shoulder Lean",
                    unit = "°",
                    unavailableReason = "Shoulder-wrist lean angle not visible"
                ),
                metricFromSample(
                    key = "hip_alignment",
                    aliases = listOf("hip_elevation", "hip_angle"),
                    label = "Hip Alignment",
                    unit = "°",
                    unavailableReason = "Hip alignment relative to torso not visible"
                ),
                metricFromSample(
                    key = "elbow_lockout",
                    aliases = listOf("arm_lockout", "elbow_extension"),
                    label = "Elbow Lockout",
                    unit = "°",
                    unavailableReason = "Arm lockout landmarks not visible"
                ),
                metricFromSample(
                    key = "scapular_positioning",
                    aliases = listOf("scapular_protraction"),
                    label = "Scapular Positioning",
                    unit = "",
                    unavailableReason = "Unavailable without scapular depth landmarks"
                ),
                metricFromSample(
                    key = "body_horizontal_alignment",
                    aliases = listOf("body_line", "horizontal_tilt"),
                    label = "Body Horizontal Alignment",
                    unit = "°",
                    unavailableReason = "Horizontal body line not visible"
                )
            )

            code.contains("FRONT_LEVER") -> listOf(
                metricFromSample(
                    key = "body_horizontal_alignment",
                    aliases = listOf("horizontal_tilt"),
                    label = "Body Horizontal Alignment",
                    unit = "°",
                    unavailableReason = "Horizontal body angle not visible"
                ),
                metricFromSample(
                    key = "hip_position",
                    aliases = listOf("body_line", "hip_alignment"),
                    label = "Hip Position",
                    unit = "°",
                    unavailableReason = "Hip-shoulder-ankle line not visible"
                ),
                metricFromSample(
                    key = "shoulder_position",
                    aliases = listOf("shoulder_angle"),
                    label = "Shoulder Position",
                    unit = "°",
                    unavailableReason = "Shoulder landmarks not clearly visible"
                ),
                metricFromSample(
                    key = "elbow_lockout",
                    aliases = listOf("elbow_extension", "arm_lockout"),
                    label = "Elbow Lockout",
                    unit = "°",
                    unavailableReason = "Elbow extension landmarks not visible"
                ),
                computeHoldConsistency(isHoldExercise)
            )

            else -> {
                // Other supported exercises: use whatever real valid metrics were captured without mixing
                metricSamples.entries.map { (name, samples) ->
                    val avgVal = samples.map { it.first }.average()
                    val avgScore = samples.map { it.second }.average()
                    ExerciseFormMetric(
                        name = name,
                        label = formatLabel(name),
                        value = round1(avgVal),
                        unit = "°",
                        score = round1(avgScore),
                        confidence = 0.85,
                        status = "VALID"
                    )
                }
            }
        }
    }

    private fun metricFromSample(
        key: String,
        aliases: List<String>,
        label: String,
        unit: String,
        unavailableReason: String
    ): ExerciseFormMetric {
        val samples = metricSamples[key]
            ?: aliases.firstNotNullOfOrNull { metricSamples[it] }

        if (samples.isNullOrEmpty()) {
            return ExerciseFormMetric(
                name = key,
                label = label,
                value = null,
                unit = unit,
                score = null,
                confidence = 0.0,
                status = "UNAVAILABLE",
                unavailableReason = unavailableReason
            )
        }

        val avgVal = samples.map { it.first }.average()
        val avgScore = samples.map { it.second }.average()
        val meanConf = frameConfidences.average().takeIf { !it.isNaN() } ?: 0.85
        return ExerciseFormMetric(
            name = key,
            label = label,
            value = round1(avgVal),
            unit = unit,
            score = round1(avgScore),
            confidence = round2(meanConf),
            status = "VALID"
        )
    }

    private fun computeTempoConsistency(reps: List<LiveRepRecord>): ExerciseFormMetric {
        val durations = reps.mapNotNull { it.durationMs }.filter { it >= 200L }.map { it / 1000.0 }
        if (durations.size < 2) {
            return ExerciseFormMetric(
                name = "tempo_consistency",
                label = "Tempo Consistency",
                value = null,
                unit = "s/rep",
                score = null,
                confidence = 0.0,
                status = "UNAVAILABLE",
                unavailableReason = "Requires at least 2 completed repetitions"
            )
        }
        val mean = durations.average()
        val variance = durations.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance)
        val cov = if (mean > 0.2) std / mean else 0.0
        val score = (100.0 - cov * 120.0).coerceIn(20.0, 100.0)
        return ExerciseFormMetric(
            name = "tempo_consistency",
            label = "Tempo Consistency",
            value = round1(mean),
            unit = "s/rep",
            score = round1(score),
            confidence = 0.90,
            status = "VALID"
        )
    }

    private fun computeRepConsistency(reps: List<LiveRepRecord>): ExerciseFormMetric {
        val scores = reps.mapNotNull { it.score }
        if (scores.size < 2) {
            return ExerciseFormMetric(
                name = "rep_consistency",
                label = "Rep Consistency",
                value = null,
                unit = "pts",
                score = null,
                confidence = 0.0,
                status = "UNAVAILABLE",
                unavailableReason = "Requires at least 2 completed repetitions"
            )
        }
        val mean = scores.average()
        val variance = scores.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance)
        val score = (100.0 - std * 3.0).coerceIn(20.0, 100.0)
        return ExerciseFormMetric(
            name = "rep_consistency",
            label = "Rep Consistency",
            value = round1(std),
            unit = "σ",
            score = round1(score),
            confidence = 0.92,
            status = "VALID"
        )
    }

    private fun computeHoldConsistency(isHoldExercise: Boolean): ExerciseFormMetric {
        val samples = metricSamples["hold_consistency"] ?: metricSamples["stability"]
        if (!samples.isNullOrEmpty()) {
            return ExerciseFormMetric(
                name = "hold_consistency",
                label = "Hold Consistency",
                value = round1(samples.map { it.first }.average()),
                unit = "%",
                score = round1(samples.map { it.second }.average()),
                confidence = 0.85,
                status = "VALID"
            )
        }
        if (!isHoldExercise || validFrameScores.size < 3) {
            return ExerciseFormMetric(
                name = "hold_consistency",
                label = "Hold Consistency",
                value = null,
                unit = "%",
                score = null,
                confidence = 0.0,
                status = "UNAVAILABLE",
                unavailableReason = "Requires at least 3 valid hold frames"
            )
        }
        val mean = validFrameScores.average()
        val variance = validFrameScores.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance)
        val consistencyScore = (100.0 - std * 3.0).coerceIn(20.0, 100.0)
        return ExerciseFormMetric(
            name = "hold_consistency",
            label = "Hold Consistency",
            value = round1(consistencyScore),
            unit = "%",
            score = round1(consistencyScore),
            confidence = 0.88,
            status = "VALID"
        )
    }

    private fun recordAggregatedError(
        code: String,
        errorName: String,
        severity: ErrorSeverity,
        timestampOffsetMs: Long,
        repNumber: Int?,
        correction: String
    ) {
        val state = aggregatedErrors.getOrPut(code) {
            AggregatedErrorState(
                code = code,
                errorName = errorName,
                severity = severity,
                suggestedCorrection = correction
            )
        }
        state.occurrences += 1
        if (severityRank(severity) > severityRank(state.severity)) {
            state.severity = severity
        }
        state.timestampsMs.add(timestampOffsetMs)
        if (repNumber != null) {
            state.repNumbers.add(repNumber)
        }
        if (state.suggestedCorrection.isNullOrBlank()) {
            state.suggestedCorrection = correction
        }
    }

    private fun isBiomechanicalFormIssue(fb: LiveFeedback): Boolean {
        val code = fb.code.uppercase(Locale.US)
        if (code in IGNORED_FEEDBACK_CODES) return false
        if (fb.severity.equals("INFO", ignoreCase = true) && code.startsWith("GOOD")) return false
        if (fb.message.contains("Great", ignoreCase = true) && fb.severity.equals("INFO", ignoreCase = true)) return false
        return fb.message.isNotBlank()
    }

    private fun formatErrorName(code: String, rawMessage: String): String {
        return KNOWN_ERROR_NAMES[code] ?: formatLabel(code.ifBlank { rawMessage.take(30) })
    }

    private fun resolveSuggestedCorrection(code: String, rawMessage: String): String {
        return KNOWN_CORRECTIONS[code] ?: rawMessage
    }

    private fun normalizeCodeFromMessage(message: String): String {
        return message.uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
            .take(32)
    }

    private fun formatLabel(raw: String): String {
        return raw.replace("_", " ").lowercase(Locale.US)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }
    }

    private fun severityRank(s: ErrorSeverity): Int = when (s) {
        ErrorSeverity.HIGH -> 3
        ErrorSeverity.MEDIUM -> 2
        ErrorSeverity.LOW -> 1
    }

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0
    private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

    private companion object {
        val IGNORED_FEEDBACK_CODES = setOf(
            "GOOD_FORM",
            "GOOD_DEPTH",
            "GOOD_HOLD",
            "INCOMPLETE_POSE",
            "LOW_CONFIDENCE",
            "NO_PERSON_DETECTED",
            "INVALID_INPUT"
        )

        val KNOWN_ERROR_NAMES = mapOf(
            "HIP_SAG" to "Hip Sagging",
            "HIP_PIKE" to "Hip Piking",
            "PIKED_HIPS" to "Hip Piking",
            "ELBOW_FLARE" to "Elbow Flare",
            "INSUFFICIENT_DEPTH" to "Incomplete ROM",
            "SHALLOW_DEPTH" to "Incomplete Squat Depth",
            "MODERATE_DEPTH" to "Moderate Squat Depth",
            "INCOMPLETE_LOCKOUT" to "Incomplete Lockout",
            "UNEVEN_PRESS" to "Asymmetric Press",
            "KNEE_VALGUS" to "Knee Valgus (Inward Collapse)",
            "FORWARD_LEAN" to "Excessive Forward Lean",
            "BENT_ARMS" to "Elbow Bending",
            "INSUFFICIENT_LEAN" to "Insufficient Shoulder Lean",
            "BODY_SAG" to "Lower Back Arch / Hip Sag"
        )

        val KNOWN_CORRECTIONS = mapOf(
            "HIP_SAG" to "Squeeze your glutes and brace your core with a slight posterior pelvic tilt.",
            "HIP_PIKE" to "Lower your hips into a straight line from shoulders to ankles.",
            "PIKED_HIPS" to "Tuck tailbone under and straighten your body line.",
            "ELBOW_FLARE" to "Tuck elbows closer to your torso (~45° angle) during descent.",
            "INSUFFICIENT_DEPTH" to "Lower your chest closer to the floor until elbows bend to ~90°.",
            "SHALLOW_DEPTH" to "Sink hips lower until thighs are parallel to or below your knees.",
            "MODERATE_DEPTH" to "Aim slightly lower at the bottom to consistently hit parallel depth.",
            "INCOMPLETE_LOCKOUT" to "Fully extend your elbows / knees at the top of every repetition.",
            "UNEVEN_PRESS" to "Distribute weight evenly and drive symmetrically through both arms.",
            "KNEE_VALGUS" to "Push knees outward in line with your second and third toes.",
            "FORWARD_LEAN" to "Keep chest upright and engage core to avoid collapsing forward.",
            "BENT_ARMS" to "Lock your elbows completely and depress/protract your shoulders.",
            "INSUFFICIENT_LEAN" to "Shift shoulders further forward over your fingertips.",
            "BODY_SAG" to "Pull belly button toward spine and squeeze glutes to eliminate lumbar arch."
        )
    }
}
