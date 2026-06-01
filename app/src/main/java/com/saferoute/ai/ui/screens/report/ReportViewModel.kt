package com.saferoute.ai.ui.screens.report

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoute.ai.data.repository.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val incidentType: String = "Accident",
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "Current GPS location",
    val useCustomPin: Boolean = false,
    val showMapPicker: Boolean = false,
    val severity: Int = 3,
    val imageUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    val incidentTypes = listOf(
        "Accident", "Road Block", "Waterlogging", "Tree Fall", "Construction",
        "Pothole", "Flood", "Traffic Jam", "Animal on Road", "Other"
    )

    fun updateType(type: String) {
        _uiState.value = _uiState.value.copy(incidentType = type)
    }

    fun updateDescription(text: String) {
        if (text.length <= 1000) _uiState.value = _uiState.value.copy(description = text)
    }

    fun setGpsLocation(lat: Double, lng: Double, label: String) {
        if (!_uiState.value.useCustomPin) {
            _uiState.value = _uiState.value.copy(
                latitude = lat,
                longitude = lng,
                locationLabel = label
            )
        }
    }

    fun updateCustomLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(
            latitude = lat,
            longitude = lng,
            locationLabel = "Custom pin (%.5f, %.5f)".format(lat, lng),
            useCustomPin = true
        )
    }

    fun useCurrentGps(lat: Double, lng: Double, label: String) {
        _uiState.value = _uiState.value.copy(
            latitude = lat,
            longitude = lng,
            locationLabel = label,
            useCustomPin = false,
            showMapPicker = false
        )
    }

    fun toggleMapPicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showMapPicker = show)
    }

    fun updateSeverity(severity: Int) {
        _uiState.value = _uiState.value.copy(severity = severity)
    }

    fun setImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(imageUri = uri)
    }

    fun submit() {
        val state = _uiState.value
        val lat = state.latitude ?: return
        val lng = state.longitude ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)
            incidentRepository.reportIncident(
                state.incidentType, state.description, lat, lng, state.severity, state.imageUri
            )
                .onSuccess { _uiState.value = _uiState.value.copy(isSubmitting = false, success = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isSubmitting = false, error = it.message) }
        }
    }

    fun reset() {
        _uiState.value = ReportUiState()
    }
}
