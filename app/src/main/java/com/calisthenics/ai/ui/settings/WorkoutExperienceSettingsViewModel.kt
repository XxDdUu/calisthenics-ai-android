package com.calisthenics.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.local.SetLoggingMode
import com.calisthenics.ai.data.local.WorkoutExperiencePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutExperienceSettingsViewModel @Inject constructor(
    private val preferences: WorkoutExperiencePreferences
) : ViewModel() {

    val bubbleEnabled: StateFlow<Boolean> = preferences.unfinishedWorkoutBubbleEnabled.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    val loggingMode: StateFlow<SetLoggingMode> = preferences.setLoggingMode.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SetLoggingMode.STEP_BY_STEP
    )

    fun setBubbleEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setUnfinishedWorkoutBubbleEnabled(enabled) }
    }

    fun setLoggingMode(mode: SetLoggingMode) {
        viewModelScope.launch { preferences.setSetLoggingMode(mode) }
    }
}
