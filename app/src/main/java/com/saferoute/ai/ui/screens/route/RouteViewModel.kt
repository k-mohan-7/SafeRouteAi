package com.saferoute.ai.ui.screens.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoute.ai.data.repository.RouteRepository
import com.saferoute.ai.domain.model.LocationSuggestion
import com.saferoute.ai.domain.model.PlannedRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteUiState(
    val sourceLat: Double? = null,
    val sourceLng: Double? = null,
    val destinationQuery: String = "",
    val suggestions: List<LocationSuggestion> = emptyList(),
    val selectedDestination: LocationSuggestion? = null,
    val plannedRoutes: List<PlannedRoute> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun setSource(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(sourceLat = lat, sourceLng = lng)
    }

    fun onDestinationQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            destinationQuery = query,
            selectedDestination = null,
            plannedRoutes = emptyList(),
            error = null
        )
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(150)
            _uiState.value = _uiState.value.copy(isSearching = true)
            routeRepository.searchLocation(query)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(suggestions = it.take(8), isSearching = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSearching = false, error = it.message)
                }
        }
    }

    fun searchOnEnter() {
        val query = _uiState.value.destinationQuery.trim()
        if (query.length < 2) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            routeRepository.searchLocation(query)
                .onSuccess { results ->
                    if (results.isNotEmpty()) {
                        selectSuggestion(results.first())
                        getRoute()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            error = "No places found for \"$query\""
                        )
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSearching = false, error = it.message)
                }
        }
    }

    fun selectSuggestion(suggestion: LocationSuggestion) {
        _uiState.value = _uiState.value.copy(
            selectedDestination = suggestion,
            destinationQuery = suggestion.name,
            suggestions = emptyList(),
            isSearching = false
        )
    }

    fun getRoute() {
        val state = _uiState.value
        val dest = state.selectedDestination ?: return
        val srcLat = state.sourceLat ?: return
        val srcLng = state.sourceLng ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            routeRepository.planRoute(srcLat, srcLng, dest.lat, dest.lng, dest.name)
                .onSuccess { routes ->
                    val sorted = routes.sortedWith(compareBy({ it.analysis.riskScore }, { it.distanceMeters }))
                    _uiState.value = _uiState.value.copy(
                        plannedRoutes = sorted,
                        selectedRouteIndex = 0,
                        isLoading = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                }
        }
    }

    fun selectRoute(index: Int) {
        _uiState.value = _uiState.value.copy(selectedRouteIndex = index)
    }

    fun getSelectedRoute(): PlannedRoute? =
        _uiState.value.plannedRoutes.getOrNull(_uiState.value.selectedRouteIndex)

    fun getAlternatives(): List<PlannedRoute> {
        val routes = _uiState.value.plannedRoutes
        val idx = _uiState.value.selectedRouteIndex
        if (routes.isEmpty()) return emptyList()
        return routes.filterIndexed { i, _ -> i != idx }
    }
}
