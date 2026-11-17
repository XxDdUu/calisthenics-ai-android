package com.calisthenics.ai.data.local

import android.content.Context
import com.calisthenics.ai.ui.workoutsession.timer.ExerciseTimerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WorkoutTimerPreferences @Inject constructor(
    @ApplicationContext context: Context,
    private val tokenManager: TokenManager
) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun userPrefix(): String {
        val userId = tokenManager.currentSessionSnapshot().activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        return if (userId != null) "user_${userId}_" else ""
    }

    open fun getTimerConfig(exerciseName: String, fallbackRestSeconds: Int? = null): ExerciseTimerConfig {
        val key = normalizeKey(exerciseName)
        val prefix = userPrefix()
        val defaultRest = fallbackRestSeconds?.takeIf { it > 0 } ?: 90

        val countdownEnabled = preferences.getBoolean(prefix + KEY_COUNTDOWN_ENABLED_PREFIX + key, true)
        val countdownDuration = preferences.getInt(prefix + KEY_COUNTDOWN_DURATION_PREFIX + key, 3)
        val restEnabled = preferences.getBoolean(prefix + KEY_REST_ENABLED_PREFIX + key, true)
        val restDuration = preferences.getInt(prefix + KEY_REST_DURATION_PREFIX + key, defaultRest)
        val soundEnabled = preferences.getBoolean(prefix + KEY_SOUND_ENABLED, true)
        val vibrationEnabled = preferences.getBoolean(prefix + KEY_VIBRATION_ENABLED, true)

        return ExerciseTimerConfig(
            countdownEnabled = countdownEnabled,
            countdownDurationSeconds = countdownDuration,
            restTimerEnabled = restEnabled,
            restDurationSeconds = restDuration,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled
        )
    }

    open fun saveTimerConfig(exerciseName: String, config: ExerciseTimerConfig) {
        val key = normalizeKey(exerciseName)
        val prefix = userPrefix()
        preferences.edit()
            .putBoolean(prefix + KEY_COUNTDOWN_ENABLED_PREFIX + key, config.countdownEnabled)
            .putInt(prefix + KEY_COUNTDOWN_DURATION_PREFIX + key, config.countdownDurationSeconds)
            .putBoolean(prefix + KEY_REST_ENABLED_PREFIX + key, config.restTimerEnabled)
            .putInt(prefix + KEY_REST_DURATION_PREFIX + key, config.restDurationSeconds)
            .putBoolean(prefix + KEY_SOUND_ENABLED, config.soundEnabled)
            .putBoolean(prefix + KEY_VIBRATION_ENABLED, config.vibrationEnabled)
            .apply()
    }

    private fun normalizeKey(name: String): String {
        return name.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_")
    }

    companion object {
        private const val PREF_NAME = "workout_timer_preferences"
        private const val KEY_COUNTDOWN_ENABLED_PREFIX = "countdown_enabled_"
        private const val KEY_COUNTDOWN_DURATION_PREFIX = "countdown_duration_"
        private const val KEY_REST_ENABLED_PREFIX = "rest_enabled_"
        private const val KEY_REST_DURATION_PREFIX = "rest_duration_"
        private const val KEY_SOUND_ENABLED = "timer_sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "timer_vibration_enabled"
    }
}

