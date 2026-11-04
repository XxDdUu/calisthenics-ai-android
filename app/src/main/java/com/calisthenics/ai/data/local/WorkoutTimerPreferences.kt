package com.calisthenics.ai.data.local

import android.content.Context
import com.calisthenics.ai.ui.workoutsession.timer.ExerciseTimerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WorkoutTimerPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    open fun getTimerConfig(exerciseName: String, fallbackRestSeconds: Int? = null): ExerciseTimerConfig {
        val key = normalizeKey(exerciseName)
        val defaultRest = fallbackRestSeconds?.takeIf { it > 0 } ?: 90

        val countdownEnabled = preferences.getBoolean(KEY_COUNTDOWN_ENABLED_PREFIX + key, true)
        val countdownDuration = preferences.getInt(KEY_COUNTDOWN_DURATION_PREFIX + key, 3)
        val restEnabled = preferences.getBoolean(KEY_REST_ENABLED_PREFIX + key, true)
        val restDuration = preferences.getInt(KEY_REST_DURATION_PREFIX + key, defaultRest)
        val soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true)
        val vibrationEnabled = preferences.getBoolean(KEY_VIBRATION_ENABLED, true)

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
        preferences.edit()
            .putBoolean(KEY_COUNTDOWN_ENABLED_PREFIX + key, config.countdownEnabled)
            .putInt(KEY_COUNTDOWN_DURATION_PREFIX + key, config.countdownDurationSeconds)
            .putBoolean(KEY_REST_ENABLED_PREFIX + key, config.restTimerEnabled)
            .putInt(KEY_REST_DURATION_PREFIX + key, config.restDurationSeconds)
            .putBoolean(KEY_SOUND_ENABLED, config.soundEnabled)
            .putBoolean(KEY_VIBRATION_ENABLED, config.vibrationEnabled)
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

