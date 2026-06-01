package com.saferoute.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saferoute.ai.domain.model.Incident
import com.saferoute.ai.domain.model.PlannedRoute
import com.saferoute.ai.domain.model.RiskCalculator

/** Speedometer, FABs, and route actions — lives inside the bottom sheet so they move with drag. */
@Composable
fun MapBottomSheetChrome(
    locationName: String,
    speedKmh: Float,
    navigationMode: Boolean,
    activeRoute: PlannedRoute?,
    alternativeRoutes: List<PlannedRoute>,
    onReportClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onStopNavigation: () -> Unit,
    onStartJourney: () -> Unit,
    onSelectAlternative: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            locationName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeedometerWidget(speedKmh = speedKmh)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (activeRoute != null && !navigationMode) {
                        Button(
                            onClick = onStartJourney,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Start Journey")
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = onReportClick,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Report")
                    }
                    Spacer(Modifier.height(8.dp))
                    if (navigationMode) {
                        FloatingActionButton(
                            onClick = onStopNavigation,
                            containerColor = MaterialTheme.colorScheme.error
                        ) {
                            Text("Stop", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        FloatingActionButton(
                            onClick = onEmergencyClick,
                            containerColor = Color(0xFFDC2626)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Emergency", tint = Color.White)
                        }
                    }
                }
            }
        }

        if (!navigationMode) {
            activeRoute?.let { route ->
                Spacer(Modifier.height(10.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    RiskCalculator.formatDistance(route.distanceMeters),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("ETA ${RiskCalculator.formatEta(route.etaSeconds)}")
                                Text(
                                    "Risk ${route.analysis.riskScore}% — ${RiskCalculator.riskLabel(route.analysis.riskScore)}",
                                    color = RiskCalculator.riskColor(route.analysis.riskScore)
                                )
                            }
                            RiskBadge(route.analysis.riskScore)
                        }
                        if (alternativeRoutes.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Choose a route (lower risk is safer):",
                                style = MaterialTheme.typography.labelMedium
                            )
                            alternativeRoutes.forEachIndexed { i, alt ->
                                TextButton(onClick = { onSelectAlternative(i + 1) }) {
                                    Text(
                                        "Alt ${i + 1}: ${RiskCalculator.formatDistance(alt.distanceMeters)} · " +
                                            "${RiskCalculator.formatEta(alt.etaSeconds)} · Risk ${alt.analysis.riskScore}%"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProximityAlertBanner(
    incident: Incident,
    distanceText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.92f))
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("⚠️ ${incident.incidentType} ahead", color = Color.White, fontWeight = FontWeight.Bold)
                Text("$distanceText — tap map marker for details", color = Color.White.copy(alpha = 0.9f))
            }
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color.White)
            }
        }
    }
}
