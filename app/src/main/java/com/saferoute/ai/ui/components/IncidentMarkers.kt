package com.saferoute.ai.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.saferoute.ai.domain.model.Incident
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.cos
import kotlin.math.sin

private fun incidentColorHex(type: String): String = when (type) {
    "Accident" -> "#EF4444"
    "Road Damage", "Pothole", "Fallen Tree", "Vehicle Breakdown" -> "#F59E0B"
    "Traffic Jam" -> "#EAB308"
    "Water Logging" -> "#3B82F6"
    "Construction Work" -> "#A855F7"
    else -> "#6B7280"
}

private fun incidentEmoji(type: String): String = when (type) {
    "Accident" -> "⚠️"
    "Road Damage" -> "🛣️"
    "Pothole" -> "○"
    "Fallen Tree" -> "🌲"
    "Vehicle Breakdown" -> "🚗"
    "Traffic Jam" -> "🚦"
    "Water Logging" -> "💧"
    "Construction Work" -> "🏗️"
    else -> "!"
}

/** Offset marker so it does not sit on top of another marker at the same location. */
fun offsetIncidentPosition(incident: Incident): GeoPoint {
    val angle = (incident.id % 360) * (Math.PI / 180)
    val meters = 20.0 + (incident.id % 4) * 6.0
    val dLat = (meters * cos(angle)) / 111_320.0
    val dLng = (meters * sin(angle)) / (111_320.0 * cos(Math.toRadians(incident.latitude)))
    return GeoPoint(incident.latitude + dLat, incident.longitude + dLng)
}

fun getIncidentPosition(incident: Incident, allIncidents: List<Incident>): GeoPoint {
    val hasCollision = allIncidents.any { other ->
        other.id != incident.id &&
        Math.abs(other.latitude - incident.latitude) < 0.00005 &&
        Math.abs(other.longitude - incident.longitude) < 0.00005
    }
    return if (hasCollision) {
        offsetIncidentPosition(incident)
    } else {
        GeoPoint(incident.latitude, incident.longitude)
    }
}

fun createIncidentMarkerDrawable(
    context: android.content.Context,
    colorHex: String,
    emoji: String
): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt()
    
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val colorInt = android.graphics.Color.parseColor(colorHex)
    
    // 1. Draw outer white border circle
    val borderPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, borderPaint)
    
    // 2. Draw inner colored circle
    val fillPaint = Paint().apply {
        color = colorInt
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (2 * density), fillPaint)
    
    // 3. Draw centered emoji
    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 17 * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    
    val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(emoji, size / 2f, (size / 2f) - yOffset, textPaint)
    
    return BitmapDrawable(context.resources, bitmap)
}

fun createIncidentMarker(
    mapView: MapView,
    incident: Incident,
    allIncidents: List<Incident>,
    onClick: (Incident) -> Unit
): Marker {
    val context = mapView.context
    val colorHex = incidentColorHex(incident.incidentType)
    val emoji = incidentEmoji(incident.incidentType)
    val drawable = createIncidentMarkerDrawable(context, colorHex, emoji)
    
    return Marker(mapView).apply {
        position = getIncidentPosition(incident, allIncidents)
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
