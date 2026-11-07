package com.calisthenics.ai.ui.bodymetrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisthenics.ai.data.remote.dto.BodyMeasurementDto
import com.calisthenics.ai.data.remote.dto.CurrentBodyMetricsDto
import com.calisthenics.ai.data.remote.dto.ProgressChartDataPointDto
import com.calisthenics.ai.data.remote.dto.RecordBodyMeasurementRequestDto
import com.calisthenics.ai.domain.repository.BodyMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class BodyMetricType(val displayName: String, val unit: String) {
    WEIGHT("Weight", "kg"),
    BODY_FAT("Body Fat", "%"),
    WAIST("Waist", "cm"),
    CHEST("Chest", "cm"),
    ARM("Arm", "cm"),
    THIGH("Thigh", "cm")
}

data class BodyMetricsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val currentMetrics: CurrentBodyMetricsDto? = null,
    val history: List<BodyMeasurementDto> = emptyList(),
    val selectedMetricType: BodyMetricType = BodyMetricType.WEIGHT,
    val selectedTimeFilter: String = "All",
    val isAddRecordOpen: Boolean = false,
    val isEditHeightOpen: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
) {
    val currentHeightCm: Double?
        get() = currentMetrics?.heightCm

    val latestWeightKg: Double?
        get() = currentMetrics?.weightKg

    val chartDataPoints: List<ProgressChartDataPointDto>
        get() {
            val sorted = history.sortedBy { it.measuredAt }
            val now = LocalDate.now()
            val cutoff = when (selectedTimeFilter) {
                "7d" -> now.minusDays(7)
                "30d" -> now.minusDays(30)
                "90d" -> now.minusDays(90)
                "6m" -> now.minusDays(180)
                "1y" -> now.minusDays(365)
                else -> null
            }

            val filtered = if (cutoff != null) {
                sorted.filter {
                    try {
                        LocalDate.parse(it.measuredAt.take(10)) >= cutoff
                    } catch (_: Exception) {
                        true
                    }
                }
            } else {
                sorted
            }

            return filtered.mapNotNull { m ->
                val v = when (selectedMetricType) {
                    BodyMetricType.WEIGHT -> m.weightKg
                    BodyMetricType.BODY_FAT -> m.bodyFatPercentage
                    BodyMetricType.WAIST -> m.waistCm
                    BodyMetricType.CHEST -> m.chestCm
                    BodyMetricType.ARM -> m.armCm
                    BodyMetricType.THIGH -> m.thighCm
                }
                v?.let {
                    ProgressChartDataPointDto(
                        date = m.measuredAt.take(10),
                        value = it
                    )
                }
            }
        }
}

@HiltViewModel
class BodyMetricsViewModel @Inject constructor(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMetricsUiState())
    val uiState: StateFlow<BodyMetricsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val currentRes = bodyMetricsRepository.getCurrentBodyMetrics()
            val historyRes = bodyMetricsRepository.getBodyMetricsHistory(page = 0, size = 100, sort = "desc")

            if (currentRes.isSuccess && historyRes.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentMetrics = currentRes.getOrNull(),
                        history = historyRes.getOrNull()?.content.orEmpty()
                    )
                }
            } else {
                val errorMsg = currentRes.exceptionOrNull()?.message
                    ?: historyRes.exceptionOrNull()?.message
                    ?: "Failed to load body metrics"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            }
        }
    }

    fun selectMetricType(type: BodyMetricType) {
        _uiState.update { it.copy(selectedMetricType = type) }
    }

    fun selectTimeFilter(filter: String) {
        _uiState.update { it.copy(selectedTimeFilter = filter) }
    }

    fun openAddRecordDialog() {
        _uiState.update { it.copy(isAddRecordOpen = true, error = null) }
    }

    fun closeAddRecordDialog() {
        _uiState.update { it.copy(isAddRecordOpen = false) }
    }

    fun openEditHeightDialog() {
        _uiState.update { it.copy(isEditHeightOpen = true, error = null) }
    }

    fun closeEditHeightDialog() {
        _uiState.update { it.copy(isEditHeightOpen = false) }
    }

    fun recordMeasurement(
        weightKgStr: String,
        bodyFatStr: String?,
        waistStr: String?,
        chestStr: String?,
        armStr: String?,
        thighStr: String?,
        dateStr: String?
    ) {
        val weight = weightKgStr.toDoubleOrNull()
        if (weight == null || weight < 20.0 || weight > 500.0) {
            _uiState.update { it.copy(error = "Weight must be between 20.0 and 500.0 kg") }
            return
        }

        val bodyFat = bodyFatStr?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (bodyFatStr != null && bodyFatStr.isNotBlank() && (bodyFat == null || bodyFat < 0.0 || bodyFat > 100.0)) {
            _uiState.update { it.copy(error = "Body fat percentage must be between 0.0 and 100.0%") }
            return
        }

        val waist = waistStr?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (waistStr != null && waistStr.isNotBlank() && (waist == null || waist < 10.0 || waist > 300.0)) {
            _uiState.update { it.copy(error = "Waist circumference must be between 10.0 and 300.0 cm") }
            return
        }

        val chest = chestStr?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (chestStr != null && chestStr.isNotBlank() && (chest == null || chest < 10.0 || chest > 300.0)) {
            _uiState.update { it.copy(error = "Chest circumference must be between 10.0 and 300.0 cm") }
            return
        }

        val arm = armStr?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (armStr != null && armStr.isNotBlank() && (arm == null || arm < 10.0 || arm > 300.0)) {
            _uiState.update { it.copy(error = "Arm circumference must be between 10.0 and 300.0 cm") }
            return
        }

        val thigh = thighStr?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        if (thighStr != null && thighStr.isNotBlank() && (thigh == null || thigh < 10.0 || thigh > 300.0)) {
            _uiState.update { it.copy(error = "Thigh circumference must be between 10.0 and 300.0 cm") }
            return
        }

        val isoTimestamp = if (!dateStr.isNullOrBlank()) {
            try {
                val parsedDate = LocalDate.parse(dateStr)
                if (parsedDate.isAfter(LocalDate.now())) {
                    _uiState.update { it.copy(error = "Measurement date cannot be in the future") }
                    return
                }
                parsedDate.atStartOfDay(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Invalid date format. Use YYYY-MM-DD") }
                return
            }
        } else {
            ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        val request = RecordBodyMeasurementRequestDto(
            weightKg = weight,
            bodyFatPercentage = bodyFat,
            waistCm = waist,
            chestCm = chest,
            armCm = arm,
            thighCm = thigh,
            measuredAt = isoTimestamp
        )

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            bodyMetricsRepository.recordBodyMeasurement(request).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isAddRecordOpen = false,
                            successMessage = "Body measurement recorded successfully!"
                        )
                    }
                    loadData()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = err.message ?: "Failed to record measurement"
                        )
                    }
                }
            )
        }
    }

    fun updateHeight(heightCmStr: String) {
        val height = heightCmStr.toDoubleOrNull()
        if (height == null || height < 50.0 || height > 300.0) {
            _uiState.update { it.copy(error = "Height must be between 50.0 and 300.0 cm") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            bodyMetricsRepository.updateHeight(height).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditHeightOpen = false,
                            currentMetrics = updated,
                            successMessage = "Height updated successfully!"
                        )
                    }
                    loadData()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = err.message ?: "Failed to update height"
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

