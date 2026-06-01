package com.saferoute.ai.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoute.ai.data.repository.IncidentRepository
import com.saferoute.ai.domain.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsUiState(
    val myReports: List<Incident> = emptyList(),
    val allReports: List<Incident> = emptyList(),
    val filter: String = "All",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadMyReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            incidentRepository.getMyIncidents()
                .onSuccess { _uiState.value = _uiState.value.copy(myReports = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoading = false) }
        }
    }

    fun loadAllReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            incidentRepository.getAllIncidents()
                .onSuccess { _uiState.value = _uiState.value.copy(allReports = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoading = false) }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun filteredReports(): List<Incident> {
        val all = _uiState.value.allReports
        val filter = _uiState.value.filter
        return if (filter == "All") all else all.filter { it.incidentType == filter }
    }

    fun resolveIncident(id: Int) {
        viewModelScope.launch {
            incidentRepository.resolveIncident(id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(message = "Incident resolved")
                    loadMyReports()
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}
