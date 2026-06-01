package com.saferoute.ai.domain.model

import androidx.compose.ui.graphics.Color
import com.saferoute.ai.data.remote.dto.IncidentDto
import com.saferoute.ai.data.remote.dto.NotificationDto
import com.saferoute.ai.data.remote.dto.ProfileDto
import com.saferoute.ai.data.remote.dto.RouteAnalysisDto
import com.saferoute.ai.data.remote.dto.RouteHistoryDto
import com.saferoute.ai.data.remote.dto.LocationSearchDto

fun IncidentDto.toDomain() = Incident(
    id = id,
    userId = userId,
    reporter = reporter,
    incidentType = incidentType,
    description = description,
    imagePath = imagePath,
    latitude = latitude,
    longitude = longitude,
    severity = severity,
    status = status,
    createdAt = createdAt
)

fun RouteAnalysisDto.toDomain() = RouteAnalysis(
    riskScore = riskScore,
    dangerLevel = dangerLevel,
    incidentTotal = incidentTotal ?: 0,
    hotspots = hotspots?.map { Hotspot(it.lat, it.lng, it.count) } ?: emptyList()
)

fun ProfileDto.toDomain() = UserProfile(
    id = id,
    fullname = fullname,
    email = email,
    phone = phone,
    reportCount = reportCount ?: 0
)

fun NotificationDto.toDomain() = AppNotification(
    id = id,
    message = message,
    isRead = isRead == 1,
    createdAt = createdAt,
    incidentId = incidentId,
    incidentType = incidentType,
    latitude = latitude,
    longitude = longitude
)

fun RouteHistoryDto.toDomain() = RouteHistoryItem(
    id = id,
    sourceLat = sourceLat,
    sourceLng = sourceLng,
    destinationLat = destinationLat,
    destinationLng = destinationLng,
    destinationName = destinationName,
    travelDate = travelDate
)

fun LocationSearchDto.toDomain(): LocationSuggestion? {
    val lat = lat ?: return null
    val lng = lng ?: return null
    return LocationSuggestion(name = name, lat = lat, lng = lng)
}

object RiskCalculator {

    val incidentDelays = mapOf(
        "Accident" to 600,
        "Tree Fall" to 480,
        "Flood" to 900,
        "Road Block" to 360,
        "Waterlogging" to 240,
        "Construction" to 120,
        "Traffic Jam" to 180,
        "Pothole" to 60,
        "Animal on Road" to 90,
        "Other" to 60
    )

    fun congestionMultiplier(riskScore: Int): Double = when {
        riskScore <= 30 -> 1.0
        riskScore <= 60 -> 1.25
        riskScore <= 80 -> 1.55
        else -> 2.0
    }

    fun computeEta(
        baseDurationSeconds: Double,
        riskScore: Int,
        hotspotIncidentTypes: List<String> = emptyList()
    ): Int {
        val multiplier = congestionMultiplier(riskScore)
        val incidentDelay = hotspotIncidentTypes.sumOf { type ->
            incidentDelays[type] ?: incidentDelays["Other"]!!
        }
        return ((baseDurationSeconds * multiplier) + incidentDelay).toInt()
    }

    fun formatEta(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h == 0) "$m min" else "${h}h ${m}m"
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000)
        } else {
            "${meters.toInt()} m"
        }
    }

    fun riskColor(score: Int): Color = when {
        score <= 30 -> Color(0xFF22C55E)
        score <= 60 -> Color(0xFFEAB308)
        score <= 80 -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    fun riskColorFromLevel(level: String): Color = when (level.lowercase()) {
        "safe" -> Color(0xFF22C55E)
        "moderate" -> Color(0xFFEAB308)
        "high" -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    fun riskLabel(score: Int): String = when {
        score <= 30 -> "Safe"
        score <= 60 -> "Moderate"
        score <= 80 -> "High Risk"
        else -> "Danger"
    }

    fun severityColor(severity: Int): Color = when (severity) {
        in 1..2 -> Color(0xFF22C55E)
        3 -> Color(0xFFEAB308)
        4 -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }
}
