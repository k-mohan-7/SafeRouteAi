package com.saferoute.ai.domain.model

data class Incident(
    val id: Int,
    val userId: Int?,
    val reporter: String?,
    val incidentType: String,
    val description: String?,
    val imagePath: String?,
    val latitude: Double,
    val longitude: Double,
    val severity: Int,
    val status: String,
    val createdAt: String
)

data class RoutePoint(val lat: Double, val lng: Double)

data class RouteAnalysis(
    val riskScore: Int,
    val dangerLevel: String,
    val incidentTotal: Int,
    val hotspots: List<Hotspot>
)

data class Hotspot(val lat: Double, val lng: Double, val count: Int)

data class PlannedRoute(
    val points: List<RoutePoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val analysis: RouteAnalysis,
    val segmentRiskScores: List<Int>,
    val etaSeconds: Int,
    val sourceLat: Double,
    val sourceLng: Double,
    val destLat: Double,
    val destLng: Double,
    val destinationName: String? = null
)

data class UserProfile(
    val id: Int,
    val fullname: String,
    val email: String,
    val phone: String?,
    val reportCount: Int
)

data class AppNotification(
    val id: Int,
    val message: String,
    val isRead: Boolean,
    val createdAt: String,
    val incidentId: Int?,
    val incidentType: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class RouteHistoryItem(
    val id: Int,
    val sourceLat: Double,
    val sourceLng: Double,
    val destinationLat: Double,
    val destinationLng: Double,
    val destinationName: String?,
    val travelDate: String
)

data class LocationSuggestion(
    val name: String,
    val lat: Double,
    val lng: Double
)
