package com.saferoute.ai.data.remote

import com.saferoute.ai.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("login.php")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponseDto>

    @POST("register.php")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<RegisterResponseDto>

    @POST("logout.php")
    suspend fun logout(): ApiResponse<Any?>

    @GET("getProfile.php")
    suspend fun getProfile(): ApiResponse<ProfileDto>

    @GET("getIncidents.php")
    suspend fun getIncidents(@Query("limit") limit: Int = 200): ApiResponse<List<IncidentDto>>

    @GET("getIncidents.php")
    suspend fun getMyIncidents(@Query("mine") mine: Int = 1): ApiResponse<List<IncidentDto>>

    @GET("getNearbyIncidents.php")
    suspend fun getNearbyIncidents(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 2000
    ): ApiResponse<List<IncidentDto>>

    @Multipart
    @POST("reportIncident.php")
    suspend fun reportIncident(
        @Part("incident_type") type: RequestBody,
        @Part("description") description: RequestBody,
        @Part("latitude") lat: RequestBody,
        @Part("longitude") lng: RequestBody,
        @Part("severity") severity: RequestBody,
        @Part image: MultipartBody.Part?
    ): ApiResponse<ReportedIncidentDto>

    @GET("calculateRisk.php")
    suspend fun calculateRisk(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 1000
    ): ApiResponse<RiskDto>

    @POST("routeAnalysis.php")
    suspend fun analyzeRoute(@Body body: RouteAnalysisRequest): ApiResponse<RouteAnalysisDto>

    @POST("saveRoute.php")
    suspend fun saveRoute(@Body body: SaveRouteRequest): ApiResponse<SaveRouteResponseDto>

    @GET("searchLocation.php")
    suspend fun searchLocation(@Query("q") query: String): ApiResponse<List<LocationSearchDto>>

    @POST("updateLocation.php")
    suspend fun updateLocation(@Body body: UpdateLocationRequest): ApiResponse<Any?>

    @GET("getNotifications.php")
    suspend fun getNotifications(): ApiResponse<List<NotificationDto>>

    @POST("getNotifications.php")
    suspend fun markNotificationsRead(@Body body: MarkReadRequest): ApiResponse<Any?>

    @POST("resolveIncident.php")
    suspend fun resolveIncident(@Body body: ResolveIncidentRequest): ApiResponse<Any?>

    @GET("getRouteHistory.php")
    suspend fun getRouteHistory(): ApiResponse<List<RouteHistoryDto>>
}

interface OsrmService {
    @GET("route/v1/driving/{coords}")
    suspend fun getRoute(
        @Path("coords") coords: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("alternatives") alternatives: Boolean = true
    ): OsrmRouteResponse
}
