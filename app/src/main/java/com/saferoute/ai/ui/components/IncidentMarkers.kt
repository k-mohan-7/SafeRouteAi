package com.saferoute.ai.ui.components

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.saferoute.ai.R
import com.saferoute.ai.domain.model.Incident
import com.saferoute.ai.domain.model.RiskCalculator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.cos
import kotlin.math.sin

private fun incidentIconRes(type: String): Int = when (type) {
    "Accident" -> R.drawable.ic_incident_accident
    "Tree Fall" -> R.drawable.ic_incident_tree
    "Waterlogging" -> R.drawable.ic_incident_water
    "Flood" -> R.drawable.ic_incident_flood
    "Traffic Jam" -> R.drawable.ic_incident_traffic
    "Road Block", "Construction", "Pothole", "Animal on Road" -> R.drawable.ic_incident_other
    else -> R.drawable.ic_incident_other
}

/** Offset marker so it does not sit on top of the user location dot. */
fun offsetIncidentPosition(incident: Incident): GeoPoint {
    val angle = (incident.id % 360) * (Math.PI / 180)
    val meters = 25.0 + (incident.id % 4) * 8.0
    val dLat = (meters * cos(angle)) / 111_320.0
    val dLng = (meters * sin(angle)) / (111_320.0 * cos(Math.toRadians(incident.latitude)))
    return GeoPoint(incident.latitude + dLat, incident.longitude + dLng)
}

fun createIncidentMarker(
    mapView: MapView,
    incident: Incident,
    onClick: (Incident) -> Unit
): Marker {
    val context = mapView.context
    val drawable: Drawable? = ContextCompat.getDrawable(context, incidentIconRes(incident.incidentType))
        ?.mutate()?.also {
            DrawableCompat.setTint(
                it,
                riskScoreToAndroidColor(incident.severity * 20)
            )
        }
    return Marker(mapView).apply {
        position = offsetIncidentPosition(incident)
        title = incident.incidentType
        icon = drawable
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        relatedObject = incident
        setOnMarkerClickListener { m, _ ->
            onClick(m.relatedObject as Incident)
            true
        }
    }
}
