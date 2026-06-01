package com.saferoute.ai.data.repository

import com.saferoute.ai.data.remote.ApiService
import com.saferoute.ai.data.remote.OsrmService
import com.saferoute.ai.data.remote.dto.RouteAnalysisRequest
import com.saferoute.ai.data.remote.dto.SaveRouteRequest
import com.saferoute.ai.domain.model.LocationSuggestion
import com.saferoute.ai.domain.model.PlannedRoute
import com.saferoute.ai.domain.model.RiskCalculator
import com.saferoute.ai.domain.model.RoutePoint
import com.saferoute.ai.domain.model.RouteHistoryItem
import com.saferoute.ai.domain.model.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val api: ApiService,
    private val osrm: OsrmService
) {
    suspend fun searchLocation(query: String): Result<List<LocationSuggestion>> = runCatching {
        val response = api.searchLocation(query)
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Search failed" })
        }
        response.data.mapNotNull { it.toDomain() }
    }

    suspend fun planRoute(
        sourceLat: Double,
        sourceLng: Double,
        destLat: Double,
        destLng: Double,
        destinationName: String?
    ): Result<List<PlannedRoute>> = runCatching {
        val coords = "$sourceLng,$sourceLat;$destLng,$destLat"
        val osrmResponse = osrm.getRoute(coords)
        val routes = osrmResponse.routes ?: error("No route found")

        val plannedRoutes = mutableListOf<PlannedRoute>()
        for (route in routes.take(3)) {
            buildPlannedRoute(
                route.geometry?.coordinates?.map { RoutePoint(lat = it[1], lng = it[0]) },
                route.distance,
                route.duration,
                sourceLat, sourceLng, destLat, destLng, destinationName
            )?.let { plannedRoutes.add(it) }
        }

        if (plannedRoutes.isEmpty()) error("No valid routes found")

        runCatching {
            api.saveRoute(
                SaveRouteRequest(sourceLat, sourceLng, destLat, destLng, destinationName)
            )
        }

        plannedRoutes.sortedWith(compareBy({ it.analysis.riskScore }, { it.distanceMeters }))
    }

    private suspend fun buildPlannedRoute(
        coordinates: List<RoutePoint>?,
        distance: Double,
        duration: Double,
        sourceLat: Double,
        sourceLng: Double,
        destLat: Double,
        destLng: Double,
        destinationName: String?
    ): PlannedRoute? {
        val points = coordinates ?: return null
        if (points.size < 2) return null
        val pointPairs = points.map { listOf(it.lat, it.lng) }
        val analysisResponse = api.analyzeRoute(RouteAnalysisRequest(pointPairs))
        if (!analysisResponse.success || analysisResponse.data == null) return null
        val analysis = analysisResponse.data.toDomain()
        val segmentScores = computeSegmentRiskScores(points)
        val eta = RiskCalculator.computeEta(duration, analysis.riskScore)
        return PlannedRoute(
            points = points,
            distanceMeters = distance,
            durationSeconds = duration,
            analysis = analysis,
            segmentRiskScores = segmentScores,
            etaSeconds = eta,
            sourceLat = sourceLat,
            sourceLng = sourceLng,
            destLat = destLat,
            destLng = destLng,
            destinationName = destinationName
        )
    }

    suspend fun calculateRiskAt(lat: Double, lng: Double, radius: Int = 1000): Result<Int> =
        runCatching {
            val response = api.calculateRisk(lat, lng, radius)
            if (!response.success || response.data == null) {
                error(response.message.ifBlank { "Risk calculation failed" })
            }
            response.data.riskScore
        }

    suspend fun getRouteHistory(): Result<List<RouteHistoryItem>> = runCatching {
        val response = api.getRouteHistory()
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Failed to load route history" })
        }
        response.data.map { it.toDomain() }
    }

    private suspend fun computeSegmentRiskScores(points: List<RoutePoint>): List<Int> {
        if (points.size < 2) return emptyList()
        val segmentCount = minOf(10, points.size - 1)
        val step = maxOf(1, (points.size - 1) / segmentCount)
        val scores = mutableListOf<Int>()
        var i = 0
        while (i < points.size - 1) {
            val midIdx = minOf(i + step / 2, points.size - 1)
            val mid = points[midIdx]
            val score = calculateRiskAt(mid.lat, mid.lng).getOrDefault(0)
            scores.add(score)
            i += step
        }
        while (scores.size < points.size - 1) {
            scores.add(scores.lastOrNull() ?: 0)
        }
        return scores.take(points.size - 1)
    }
}
