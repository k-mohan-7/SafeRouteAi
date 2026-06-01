package com.saferoute.ai.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun ReportMapPicker(
    initialLat: Double,
    initialLng: Double,
    onLocationPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerMarker by remember { mutableStateOf<Marker?>(null) }

    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        OsmMapView(
            modifier = Modifier.fillMaxSize(),
            initialZoom = 16.0,
            showBuiltInZoomControls = true
        ) { mapView ->
            val start = GeoPoint(initialLat, initialLng)
            mapView.controller.setCenter(start)
            mapView.overlays.removeAll { it is MapEventsOverlay || (it is Marker && it.title == "picker") }
            val events = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    p ?: return false
                    onLocationPicked(p.latitude, p.longitude)
                    pickerMarker?.let { mapView.overlays.remove(it) }
                    pickerMarker = Marker(mapView).apply {
                        position = p
                        title = "picker"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(pickerMarker)
                    mapView.invalidate()
                    return true
                }
            })
            mapView.overlays.add(0, events)
        }
    }
}
