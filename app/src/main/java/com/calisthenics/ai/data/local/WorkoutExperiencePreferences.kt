package com.calisthenics.ai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class SetLoggingMode {
    STEP_BY_STEP,
    SHOW_ALL
}

private val Context.workoutExperienceDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "workout_experience_settings")

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutExperiencePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager
) {
    private val legacyBubbleKey = booleanPreferencesKey("unfinished_workout_bubble_enabled")
    private val legacyLoggingModeKey = stringPreferencesKey("set_logging_mode")

    private fun bubbleKeyForUser(userId: Long?): Preferences.Key<Boolean> {
        val resolved = userId?.takeIf { it > 0L }
        return if (resolved != null) {
            booleanPreferencesKey("user_${resolved}_unfinished_workout_bubble_enabled")
        } else {
            legacyBubbleKey
        }
    }

    private fun loggingModeKeyForUser(userId: Long?): Preferences.Key<String> {
        val resolved = userId?.takeIf { it > 0L }
        return if (resolved != null) {
            stringPreferencesKey("user_${resolved}_set_logging_mode")
        } else {
            legacyLoggingModeKey
        }
    }

    val unfinishedWorkoutBubbleEnabled: Flow<Boolean> = tokenManager.sessionState.flatMapLatest { session ->
        val userId = session.activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        val key = bubbleKeyForUser(userId)
        context.workoutExperienceDataStore.data.map { preferences ->
            preferences[key] ?: preferences[legacyBubbleKey] ?: true
        }
    }

    val setLoggingMode: Flow<SetLoggingMode> = tokenManager.sessionState.flatMapLatest { session ->
        val userId = session.activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        val key = loggingModeKeyForUser(userId)
        context.workoutExperienceDataStore.data.map { preferences ->
            val raw = preferences[key] ?: preferences[legacyLoggingModeKey]
            runCatching { SetLoggingMode.valueOf(raw ?: SetLoggingMode.STEP_BY_STEP.name) }
                .getOrDefault(SetLoggingMode.STEP_BY_STEP)
        }
    }

    suspend fun setUnfinishedWorkoutBubbleEnabled(enabled: Boolean) {
        val userId = tokenManager.currentSessionSnapshot().activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        val key = bubbleKeyForUser(userId)
        context.workoutExperienceDataStore.edit { it[key] = enabled }
    }

    suspend fun setSetLoggingMode(mode: SetLoggingMode) {
        val userId = tokenManager.currentSessionSnapshot().activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        val key = loggingModeKeyForUser(userId)
        context.workoutExperienceDataStore.edit { it[key] = mode.name }
    }
}
