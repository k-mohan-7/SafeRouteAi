package com.saferoute.ai.ui.components

import androidx.core.content.ContextCompat
import com.saferoute.ai.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

fun addRouteEndpointMarkers(
    mapView: MapView,
    sourceLat: Double,
    sourceLng: Double,
    destLat: Double,
    destLng: Double
) {
    val ctx = mapView.context
    val start = Marker(mapView).apply {
        position = GeoPoint(sourceLat, sourceLng)
        title = "Start"
        icon = ContextCompat.getDrawable(ctx, R.drawable.ic_marker_start)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
    val end = Marker(mapView).apply {
        position = GeoPoint(destLat, destLng)
        title = "Destination"
        icon = ContextCompat.getDrawable(ctx, R.drawable.ic_marker_end)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }
    mapView.overlays.add(start)
    mapView.overlays.add(end)
}
