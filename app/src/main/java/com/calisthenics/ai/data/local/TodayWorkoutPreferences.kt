package com.calisthenics.ai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calisthenics.ai.domain.model.WorkoutSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.todayWorkoutDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "today_workout_settings")

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class TodayWorkoutPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager
) {
    private val legacyPriorityKey = stringPreferencesKey("today_workout_priority")

    private fun keyForUser(userId: Long?): Preferences.Key<String> {
        val resolved = userId?.takeIf { it > 0L }
        return if (resolved != null) {
            stringPreferencesKey("user_${resolved}_today_workout_priority")
        } else {
            legacyPriorityKey
        }
    }

    val priority: Flow<WorkoutSource> = tokenManager.sessionState.flatMapLatest { session ->
        val userId = session.activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        val key = keyForUser(userId)
        context.todayWorkoutDataStore.data.map { preferences ->
            val raw = if (userId != null) {
                preferences[key]
            } else {
                preferences[legacyPriorityKey]
            }
            runCatching { WorkoutSource.valueOf(raw ?: WorkoutSource.COACH.name) }
                .getOrDefault(WorkoutSource.COACH)
        }
    }

    suspend fun setPriority(source: WorkoutSource) {
        require(source == WorkoutSource.COACH || source == WorkoutSource.PERSONAL)
        val userId = tokenManager.currentSessionSnapshot().activeUserId?.takeIf { it > 0L }
            ?: tokenManager.getUserId()?.takeIf { it > 0L }
        val key = keyForUser(userId)
        context.todayWorkoutDataStore.edit { it[key] = source.name }
    }
}
