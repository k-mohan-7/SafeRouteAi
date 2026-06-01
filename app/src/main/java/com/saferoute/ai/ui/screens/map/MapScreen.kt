package com.saferoute.ai.ui.screens.map

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.saferoute.ai.BuildConfig
import com.saferoute.ai.domain.model.Incident
import com.saferoute.ai.ui.components.MapBottomSheetChrome
import com.saferoute.ai.ui.components.MapDashboardSheetContent
import com.saferoute.ai.ui.components.OsmMapView
import com.saferoute.ai.ui.components.ProximityAlertBanner
import com.saferoute.ai.ui.components.createIncidentMarker
import com.saferoute.ai.ui.components.drawSegmentedPolyline
import com.saferoute.ai.ui.components.addRouteEndpointMarkers
import com.saferoute.ai.ui.screens.report.ReportIncidentSheet
import com.saferoute.ai.util.GeoUtils
import com.saferoute.ai.util.TimeUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateRoute: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onLocationUpdate: ((Double, Double) -> Unit)? = null,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var locationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var showEmergency by remember { mutableStateOf(false) }
    var dashboardTab by remember { mutableIntStateOf(0) }
    val uLat = state.userLat
    val uLng = state.userLng

    val sheetPeekHeight = 200.dp
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    LaunchedEffect(Unit) { viewModel.startPeriodicRefresh() }
    LaunchedEffect(state.userLat, state.userLng) {
        if (state.userLat != null && state.userLng != null) {
            onLocationUpdate?.invoke(state.userLat!!, state.userLng!!)
            viewModel.refreshNearbyIncidents()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Map rotation only while navigating
    DisposableEffect(state.navigationMode, state.rotationLocked, mapView) {
        val mv = mapView
        if (mv == null || !state.navigationMode || state.rotationLocked) {
            mv?.mapOrientation = 0f
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(SensorManager::class.java)
        var azimuth = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotation, orientation)
                val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = azimuth * 0.9f + deg * 0.1f
                mv.mapOrientation = -azimuth
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(
            listener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_UI
        )
        onDispose {
            sensorManager.unregisterListener(listener)
            mv.mapOrientation = 0f
        }
    }

    LaunchedEffect(state.incidents, state.activeRoute, mapView) {
        val mv = mapView ?: return@LaunchedEffect
        mv.overlays.removeAll { it is Marker }
        state.incidents.forEach { incident ->
            mv.overlays.add(createIncidentMarker(mv, incident) { viewModel.selectIncident(it) })
        }
        state.activeRoute?.let { route ->
            addRouteEndpointMarkers(mv, route.sourceLat, route.sourceLng, route.destLat, route.destLng)
        }
        mv.invalidate()
    }

    LaunchedEffect(state.activeRoute, state.alternativeRoutes, mapView, state.navigationMode) {
        val mv = mapView ?: return@LaunchedEffect
        mv.overlays.removeAll { it is org.osmdroid.views.overlay.Polyline }
        state.alternativeRoutes.forEach { alt ->
            val pts = alt.points.map { GeoPoint(it.lat, it.lng) }
            drawSegmentedPolyline(
                mv, pts, alt.segmentRiskScores,
                strokeWidth = 8f,
                fallbackRiskScore = alt.analysis.riskScore,
                dimmed = true
            )
        }
        state.activeRoute?.let { route ->
            val points = route.points.map { GeoPoint(it.lat, it.lng) }
            drawSegmentedPolyline(
                mv, points, route.segmentRiskScores,
                strokeWidth = if (state.navigationMode) 18f else 14f,
                fallbackRiskScore = route.analysis.riskScore
            )
            mv.post {
                if (state.navigationMode && uLat != null && uLng != null) {
                    mv.controller.animateTo(GeoPoint(uLat, uLng))
                    mv.controller.setZoom(18.0)
                    locationOverlay?.enableFollowLocation()
                } else if (!state.navigationMode) {
                    locationOverlay?.disableFollowLocation()
                    mv.zoomToBoundingBox(
                        org.osmdroid.util.BoundingBox.fromGeoPoints(points),
                        true, 120
                    )
                }
            }
        }
    }

    LaunchedEffect(state.navigationMode, uLat, uLng, mapView) {
        if (!state.navigationMode) {
            locationOverlay?.disableFollowLocation()
            return@LaunchedEffect
        }
        val mv = mapView ?: return@LaunchedEffect
        if (uLat != null && uLng != null) {
            mv.controller.animateTo(GeoPoint(uLat, uLng))
            locationOverlay?.enableFollowLocation()
        }
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        sheetSwipeEnabled = true,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        sheetContent = {
            Column(Modifier.fillMaxWidth()) {
                MapBottomSheetChrome(
                    locationName = state.userLocationName,
                    speedKmh = state.speedKmh,
                    navigationMode = state.navigationMode,
                    activeRoute = state.activeRoute,
                    alternativeRoutes = state.alternativeRoutes,
                    liveEtaLabel = viewModel.displayEta(),
                    onReportClick = { showReport = true },
                    onEmergencyClick = { showEmergency = true },
                    onStopNavigation = { viewModel.stopNavigation() },
                    onStartJourney = { viewModel.startNavigation() },
                    onSelectAlternative = { viewModel.selectRouteOption(it) }
                )
                MapDashboardSheetContent(
                    selectedTab = dashboardTab,
                    onTabSelected = { dashboardTab = it },
                    incidents = state.incidents,
                    myReports = state.myReports,
                    myRoutes = state.myRoutes,
                    isLoading = state.isLoadingDashboard,
                    userLat = uLat,
                    userLng = uLng,
                    locationName = state.userLocationName,
                    onIncidentClick = { viewModel.selectIncident(it) }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            OsmMapView(Modifier.fillMaxSize()) { mv ->
                mapView = mv
                if (locationOverlay == null) {
                    locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mv).apply {
                        enableMyLocation()
                    }
                    mv.overlays.add(locationOverlay)
                }
            }

            if (!state.navigationMode) {
                Row(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateRoute() },
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.elevatedCardElevation(6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.padding(4.dp))
                            Text(
                                "Search destination…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleRotationLock() }) {
                        Icon(Icons.Default.CompassCalibration, contentDescription = "Lock map north")
                    }
                    BadgedBox(badge = { if (state.unreadNotifications > 0) Badge() }) {
                        IconButton(onClick = onNavigateNotifications) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            }

            state.proximityAlert?.let { alert ->
                val dist = if (uLat != null && uLng != null) {
                    GeoUtils.formatDistance(
                        GeoUtils.distanceMeters(uLat, uLng, alert.latitude, alert.longitude)
                    )
                } else ""
                ProximityAlertBanner(
                    incident = alert,
                    distanceText = dist,
                    onDismiss = { viewModel.dismissProximityAlert() },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
                )
            }
        }
    }

    state.selectedIncident?.let { incident ->
        ModalBottomSheet(onDismissRequest = { viewModel.selectIncident(null) }) {
            IncidentDetailContent(incident, uLat, uLng)
        }
    }

    if (showReport) {
        ReportIncidentSheet(
            initialLat = uLat,
            initialLng = uLng,
            locationLabel = state.userLocationName,
            onDismiss = { showReport = false },
            onSuccess = {
                showReport = false
                viewModel.refreshNearbyIncidents()
                viewModel.loadDashboardData()
            }
        )
    }

    if (showEmergency) {
        AlertDialog(
            onDismissRequest = { showEmergency = false },
            title = { Text("Emergency Call") },
            text = {
                Column {
                    listOf(
                        "Police" to "100",
                        "Ambulance" to "108",
                        "Fire" to "101",
                        "Highway Helpline" to "1033"
                    ).forEach { (label, number) ->
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                            showEmergency = false
                        }) { Text("$label: $number") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEmergency = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun IncidentDetailContent(incident: Incident, userLat: Double?, userLng: Double?) {
    Column(Modifier.padding(24.dp)) {
        Text(incident.incidentType, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(incident.description.orEmpty())
        Text("Reporter: ${incident.reporter ?: "Anonymous"}")
        Text("Severity: ${"★".repeat(incident.severity)}")
        Text(TimeUtils.timeAgo(incident.createdAt))
        if (userLat != null && userLng != null) {
            Text(GeoUtils.formatDistance(GeoUtils.distanceMeters(userLat, userLng, incident.latitude, incident.longitude)))
        }
        incident.imagePath?.let {
            AsyncImage(
                model = BuildConfig.UPLOAD_BASE_URL + it,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
