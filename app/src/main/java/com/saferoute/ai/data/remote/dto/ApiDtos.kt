package com.saferoute.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String
)

data class LoginResponseDto(
    @SerializedName("user_id") val userId: Int,
    val fullname: String
)

data class RegisterResponseDto(
    @SerializedName("user_id") val userId: Int
)

data class IncidentDto(
    val id: Int,
    @SerializedName("user_id") val userId: Int?,
    val reporter: String?,
    @SerializedName("incident_type") val incidentType: String,
    val description: String?,
    @SerializedName("image_path") val imagePath: String?,
    val latitude: Double,
    val longitude: Double,
    val severity: Int,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

data class ReportedIncidentDto(
    @SerializedName("incident_id") val incidentId: Int,
    @SerializedName("image_path") val imagePath: String?
)

data class RiskDto(
    @SerializedName("risk_score") val riskScore: Int,
    @SerializedName("danger_level") val dangerLevel: String,
    val color: String?,
    @SerializedName("incident_count") val incidentCount: Int?,
    @SerializedName("avg_severity") val avgSeverity: Double?,
    @SerializedName("radius_m") val radiusM: Int?
)

data class RouteAnalysisRequest(
    val points: List<List<Double>>
)

data class HotspotDto(
    val lat: Double,
    val lng: Double,
    val count: Int
)

data class RouteAnalysisDto(
    @SerializedName("risk_score") val riskScore: Int,
    @SerializedName("danger_level") val dangerLevel: String,
    @SerializedName("incident_total") val incidentTotal: Int?,
    @SerializedName("avg_severity") val avgSeverity: Double?,
    val hotspots: List<HotspotDto>?,
    val sampled: Int?
)

data class SaveRouteResponseDto(
    @SerializedName("route_id") val routeId: Int
)

data class LocationSearchDto(
    val name: String,
    val lat: Double?,
    val lng: Double?,
    val type: String?
)

data class ProfileDto(
    val id: Int,
    val fullname: String,
    val email: String,
    val phone: String?,
    @SerializedName("profile_photo") val profilePhoto: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("report_count") val reportCount: Int?
)

data class NotificationDto(
    val id: Int,
    val message: String,
    @SerializedName("is_read") val isRead: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("incident_id") val incidentId: Int?,
    @SerializedName("incident_type") val incidentType: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class RouteHistoryDto(
    val id: Int,
    @SerializedName("source_lat") val sourceLat: Double,
    @SerializedName("source_lng") val sourceLng: Double,
    @SerializedName("destination_lat") val destinationLat: Double,
    @SerializedName("destination_lng") val destinationLng: Double,
    @SerializedName("destination_name") val destinationName: String?,
    @SerializedName("travel_date") val travelDate: String
)

data class UpdateLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val speed: Float
)

data class MarkReadRequest(
    @SerializedName("mark_read") val markRead: Boolean = true
)

data class ResolveIncidentRequest(
    @SerializedName("incident_id") val incidentId: Int
)

data class SaveRouteRequest(
    @SerializedName("source_lat") val sourceLat: Double,
    @SerializedName("source_lng") val sourceLng: Double,
    @SerializedName("destination_lat") val destinationLat: Double,
    @SerializedName("destination_lng") val destinationLng: Double,
    @SerializedName("destination_name") val destinationName: String?
)

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val fullname: String,
    val email: String,
    val phone: String?,
    val password: String,
    @SerializedName("confirm_password") val confirmPassword: String
)

// OSRM response DTOs
data class OsrmRouteResponse(
    val routes: List<OsrmRoute>?
)

data class OsrmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: OsrmGeometry?
)

data class OsrmGeometry(
    val coordinates: List<List<Double>>?,
    val type: String?
)
