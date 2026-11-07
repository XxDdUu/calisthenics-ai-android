package com.calisthenics.ai.ui.workoutsession.timer

import android.os.SystemClock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface TimeSource {
    fun elapsedRealtime(): Long
}

@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}

enum class TimerStatus {
    IDLE,
    COUNTDOWN,
    HOLDING,
    SET_COMPLETED,
    RESTING,
    REST_PAUSED,
    REST_COMPLETED
}

data class ExerciseTimerConfig(
    val countdownEnabled: Boolean = true,
    val countdownDurationSeconds: Int = 3,
    val restTimerEnabled: Boolean = true,
    val restDurationSeconds: Int = 90,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

data class ActiveTimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val exerciseId: Long = 0L,
    val setId: Long = 0L,
    val exerciseName: String = "",
    val setNumber: Int = 1,
    val targetHoldDurationSeconds: Int? = null,
    // Countdown state
    val countdownTotalSeconds: Int = 3,
    val countdownRemainingSeconds: Int = 3,
    // Hold state
    val holdElapsedMillis: Long = 0L,
    val finalHoldDurationSeconds: Double = 0.0,
    // Rest state
    val restTotalSeconds: Int = 90,
    val restRemainingSeconds: Int = 90,
    val nextExerciseName: String = "",
    val nextSetNumber: Int = 1
) {
    val isTimerActive: Boolean
        get() = status != TimerStatus.IDLE

    val isHoldingOrCountdown: Boolean
        get() = status == TimerStatus.COUNTDOWN ||
            status == TimerStatus.HOLDING ||
            status == TimerStatus.SET_COMPLETED

    val isResting: Boolean
        get() = status == TimerStatus.RESTING ||
            status == TimerStatus.REST_PAUSED ||
            status == TimerStatus.REST_COMPLETED

    val restProgress: Float
        get() = if (restTotalSeconds > 0) {
            ((restTotalSeconds - restRemainingSeconds).toFloat() / restTotalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 1f

    val formattedHoldDuration: String
        get() = String.format(Locale.US, "%.2fs", finalHoldDurationSeconds)

    val formattedHoldElapsed: String
        get() = formatElapsedHold(holdElapsedMillis)

    companion object {
        fun formatElapsedHold(millis: Long): String {
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val centiseconds = (millis % 1000) / 10
            return "%02d:%02d.%02d".format(Locale.US, minutes, seconds, centiseconds)
        }

        fun formatSeconds(seconds: Int): String {
            val s = seconds.coerceAtLeast(0)
            val minutes = s / 60
            val remainder = s % 60
            return "%02d:%02d".format(Locale.US, minutes, remainder)
        }
    }
}

