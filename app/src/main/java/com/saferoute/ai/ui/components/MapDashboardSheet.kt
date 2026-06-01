package com.saferoute.ai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saferoute.ai.domain.model.Incident
import com.saferoute.ai.domain.model.RouteHistoryItem
import com.saferoute.ai.util.GeoUtils
import com.saferoute.ai.util.TimeUtils

/**
 * Scrollable body for the map dashboard — used inside [androidx.compose.material3.BottomSheetScaffold].
 */
@Composable
fun MapDashboardSheetContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    incidents: List<Incident>,
    myReports: List<Incident>,
    myRoutes: List<RouteHistoryItem>,
    isLoading: Boolean,
    userLat: Double?,
    userLng: Double?,
    locationName: String,
    onIncidentClick: (Incident) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }, text = { Text("Nearby") })
            Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }, text = { Text("My Reports") })
            Tab(selected = selectedTab == 2, onClick = { onTabSelected(2) }, text = { Text("My Routes") })
        }
        if (isLoading) {
            CircularProgressIndicator(
                Modifier
                    .padding(24.dp)
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        if (incidents.isEmpty()) EmptyState("No nearby incidents")
                        else incidents.forEach { incident ->
                            val dist = if (userLat != null && userLng != null) {
                                GeoUtils.formatDistance(
                                    GeoUtils.distanceMeters(
                                        userLat, userLng, incident.latitude, incident.longitude
                                    )
                                )
                            } else null
                            IncidentCard(
                                incident = incident,
                                distanceText = dist,
                                onClick = { onIncidentClick(incident) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    1 -> {
                        if (myReports.isEmpty()) EmptyState("No reports yet — tap + to report")
                        else myReports.forEach { incident ->
                            IncidentCard(
                                incident = incident,
                                onClick = { onIncidentClick(incident) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    2 -> {
                        if (myRoutes.isEmpty()) EmptyState("No saved routes yet")
                        else myRoutes.forEach { route ->
                            IncidentCard(
                                incident = Incident(
                                    id = route.id,
                                    userId = null,
                                    reporter = null,
                                    incidentType = "Saved Route",
                                    description = route.destinationName ?: "Route to destination",
                                    imagePath = null,
                                    latitude = route.destinationLat,
                                    longitude = route.destinationLng,
                                    severity = 2,
                                    status = "active",
                                    createdAt = route.travelDate
                                ),
                                distanceText = TimeUtils.timeAgo(route.travelDate),
                                onClick = null,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
