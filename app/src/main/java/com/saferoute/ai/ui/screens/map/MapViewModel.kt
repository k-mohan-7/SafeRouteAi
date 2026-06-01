package com.saferoute.ai.ui.screens.map

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoute.ai.data.repository.IncidentRepository
import com.saferoute.ai.data.repository.NotificationRepository
import com.saferoute.ai.data.repository.RouteRepository
import com.saferoute.ai.domain.model.Incident
import com.saferoute.ai.domain.model.PlannedRoute
import com.saferoute.ai.domain.model.RiskCalculator
import com.saferoute.ai.domain.model.RouteHistoryItem
import com.saferoute.ai.util.GeoUtils
import com.saferoute.ai.util.GeocoderUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val incidents: List<Incident> = emptyList(),
    val myReports: List<Incident> = emptyList(),
    val myRoutes: List<RouteHistoryItem> = emptyList(),
    val selectedIncident: Incident? = null,
    val isLoading: Boolean = false,
    val isLoadingDashboard: Boolean = false,
    val error: String? = null,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val userLocationName: String = "My Location",
    val speedKmh: Float = 0f,
    val activeRoute: PlannedRoute? = null,
    val alternativeRoutes: List<PlannedRoute> = emptyList(),
    val selectedRouteIndex: Int = 0,
    val rotationLocked: Boolean = false,
    val navigationMode: Boolean = false,
    val unreadNotifications: Int = 0,
    val liveEtaSeconds: Int? = null,
    val proximityAlert: Incident? = null,
    val dismissedProximityId: Int? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidentRepository: IncidentRepository,
    private val notificationRepository: NotificationRepository,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var geocodeJob: Job? = null

    fun updateLocation(location: Location) {
        val prev = _uiState.value
        val speedKmh = location.speed * 3.6f
        var liveEta = prev.liveEtaSeconds
        prev.activeRoute?.let { route ->
            if (prev.navigationMode && route.durationSeconds > 0) {
                val progress = 1f - (location.speed / maxOf(1f, (route.distanceMeters / route.durationSeconds).toFloat()))
                liveEta = (route.etaSeconds * progress.coerceIn(0.1f, 1f)).toInt()
                    .coerceAtLeast(60)
            }
        }
        val proximity = if (prev.navigationMode) {
            findProximityAlert(location.latitude, location.longitude, prev.incidents, prev.dismissedProximityId)
        } else null

        _uiState.value = prev.copy(
            userLat = location.latitude,
            userLng = location.longitude,
            speedKmh = speedKmh,
            liveEtaSeconds = liveEta,
            proximityAlert = proximity
        )
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            val name = GeocoderUtil.reverseGeocode(context, location.latitude, location.longitude)
            if (name != null) {
                _uiState.value = _uiState.value.copy(userLocationName = name)
            }
        }
    }

    fun setSpeed(speedKmh: Float) {
        _uiState.value = _uiState.value.copy(speedKmh = speedKmh)
    }

    fun setActiveRoute(route: PlannedRoute?, alternatives: List<PlannedRoute> = emptyList()) {
        _uiState.value = _uiState.value.copy(
            activeRoute = route,
            alternativeRoutes = alternatives,
            selectedRouteIndex = 0,
            liveEtaSeconds = route?.etaSeconds
        )
    }

    fun selectRouteOption(index: Int) {
        val state = _uiState.value
        val all = listOfNotNull(state.activeRoute) + state.alternativeRoutes
        if (index !in all.indices) return
        val selected = all[index]
        val others = all.filterIndexed { i, _ -> i != index }
        _uiState.value = state.copy(
            activeRoute = selected,
            alternativeRoutes = others,
            liveEtaSeconds = selected.etaSeconds
        )
    }

    fun toggleRotationLock() {
        _uiState.value = _uiState.value.copy(rotationLocked = !_uiState.value.rotationLocked)
    }

    fun startNavigation() {
        val route = _uiState.value.activeRoute ?: return
        _uiState.value = _uiState.value.copy(
            navigationMode = true,
            liveEtaSeconds = route.etaSeconds
        )
    }

    fun stopNavigation() {
        _uiState.value = _uiState.value.copy(
            navigationMode = false,
            activeRoute = null,
            alternativeRoutes = emptyList(),
            liveEtaSeconds = null
        )
    }

    fun selectIncident(incident: Incident?) {
        _uiState.value = _uiState.value.copy(selectedIncident = incident)
    }

    fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            loadDashboardData()
            while (isActive) {
                refreshNearbyIncidents()
                refreshNotifications()
                if (_uiState.value.navigationMode) {
                    delay(30_000)
                } else {
                    delay(60_000)
                }
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDashboard = true)
            val reports = incidentRepository.getMyIncidents().getOrElse { emptyList() }
            val routes = routeRepository.getRouteHistory().getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(
                myReports = reports,
                myRoutes = routes,
                isLoadingDashboard = false,
                error = if (reports.isEmpty() && routes.isEmpty()) null else _uiState.value.error
            )
        }
    }

    fun refreshNearbyIncidents() {
        val lat = _uiState.value.userLat ?: return
        val lng = _uiState.value.userLng ?: return
        viewModelScope.launch {
            incidentRepository.getNearbyIncidents(lat, lng, 5000)
                .onSuccess { incidents ->
                    _uiState.value = _uiState.value.copy(incidents = incidents, isLoading = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = it.message ?: "Failed to load incidents")
                }
        }
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            notificationRepository.getNotifications()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(
                        unreadNotifications = list.count { !it.isRead }
                    )
                }
        }
    }

    fun displayEta(): String {
        val state = _uiState.value
        val seconds = state.liveEtaSeconds ?: state.activeRoute?.etaSeconds ?: return "—"
        return RiskCalculator.formatEta(seconds)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissProximityAlert() {
        val id = _uiState.value.proximityAlert?.id
        _uiState.value = _uiState.value.copy(dismissedProximityId = id, proximityAlert = null)
    }

    private fun findProximityAlert(
        lat: Double,
        lng: Double,
        incidents: List<Incident>,
        dismissedId: Int?
    ): Incident? {
        return incidents
            .filter { it.id != dismissedId }
            .map { it to GeoUtils.distanceMeters(lat, lng, it.latitude, it.longitude) }
            .filter { (_, d) -> d in 1.0..800.0 }
            .minByOrNull { it.second }
            ?.first
    }
}
