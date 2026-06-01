package com.saferoute.ai.data.repository

import com.saferoute.ai.data.remote.ApiService
import com.saferoute.ai.data.remote.dto.MarkReadRequest
import com.saferoute.ai.data.remote.dto.UpdateLocationRequest
import com.saferoute.ai.domain.model.AppNotification
import com.saferoute.ai.domain.model.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        val response = api.getNotifications()
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Failed to load notifications" })
        }
        response.data.map { it.toDomain() }
    }

    suspend fun markAllRead(): Result<Unit> = runCatching {
        val response = api.markNotificationsRead(MarkReadRequest(true))
        if (!response.success) {
            error(response.message.ifBlank { "Failed to mark read" })
        }
    }

    suspend fun updateLocation(lat: Double, lng: Double, speedKmh: Float): Result<Unit> =
        runCatching {
            val response = api.updateLocation(
                UpdateLocationRequest(lat, lng, speedKmh)
            )
            if (!response.success) {
                error(response.message.ifBlank { "Location update failed" })
            }
        }
}
