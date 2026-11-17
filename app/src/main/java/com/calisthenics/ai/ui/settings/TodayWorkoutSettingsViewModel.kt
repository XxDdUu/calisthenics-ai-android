package com.calisthenics.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.TodayWorkoutPreferences
import com.calisthenics.ai.domain.model.WorkoutSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodayWorkoutSettingsViewModel @Inject constructor(
    private val preferences: TodayWorkoutPreferences
) : ViewModel() {
    val priority: StateFlow<WorkoutSource> = preferences.priority.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        WorkoutSource.COACH
    )

    fun setPriority(source: WorkoutSource) {
        viewModelScope.launch { preferences.setPriority(source) }
    }
}
