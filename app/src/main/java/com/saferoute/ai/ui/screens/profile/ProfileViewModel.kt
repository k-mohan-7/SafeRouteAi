package com.saferoute.ai.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoute.ai.data.repository.AuthRepository
import com.saferoute.ai.data.repository.IncidentRepository
import com.saferoute.ai.data.repository.RouteRepository
import com.saferoute.ai.domain.model.RouteHistoryItem
import com.saferoute.ai.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val reportCount: Int = 0,
    val routeHistory: List<RouteHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val loggedOut: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val incidentRepository: IncidentRepository,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepository.getProfile()
                .onSuccess { profile ->
                    val reports = incidentRepository.getMyIncidents().getOrNull()?.size ?: 0
                    val history = routeRepository.getRouteHistory().getOrNull() ?: emptyList()
                    _uiState.value = ProfileUiState(
                        profile = profile,
                        reportCount = reports,
                        routeHistory = history,
                        isLoading = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(loggedOut = true)
        }
    }
}
