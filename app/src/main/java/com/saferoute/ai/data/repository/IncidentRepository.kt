package com.saferoute.ai.data.repository

import android.content.Context
import android.net.Uri
import com.saferoute.ai.data.remote.ApiService
import com.saferoute.ai.data.remote.dto.ResolveIncidentRequest
import com.saferoute.ai.domain.model.Incident
import com.saferoute.ai.domain.model.toDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context
) {
    suspend fun getAllIncidents(limit: Int = 200): Result<List<Incident>> = runCatching {
        val response = api.getIncidents(limit)
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Failed to load incidents" })
        }
        response.data.map { it.toDomain() }
    }

    suspend fun getMyIncidents(): Result<List<Incident>> = runCatching {
        val response = api.getMyIncidents()
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Failed to load your reports" })
        }
        response.data.map { it.toDomain() }
    }

    suspend fun getNearbyIncidents(lat: Double, lng: Double, radius: Int): Result<List<Incident>> =
        runCatching {
            val response = api.getNearbyIncidents(lat, lng, radius)
            if (!response.success || response.data == null) {
                error(response.message.ifBlank { "Failed to load nearby incidents" })
            }
            response.data.map { it.toDomain() }
        }

    suspend fun reportIncident(
        type: String,
        description: String,
        lat: Double,
        lng: Double,
        severity: Int,
        imageUri: Uri?
    ): Result<Int> = runCatching {
        val imagePart = imageUri?.let { uri ->
            val file = uriToTempFile(uri)
            val body = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", file.name, body)
        }
        val response = api.reportIncident(
            type = type.toRequestBody("text/plain".toMediaTypeOrNull()),
            description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
            lat = lat.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            lng = lng.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            severity = severity.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            image = imagePart
        )
        if (!response.success || response.data == null) {
            error(response.message.ifBlank { "Failed to report incident" })
        }
        response.data.incidentId
    }

    suspend fun resolveIncident(incidentId: Int): Result<Unit> = runCatching {
        val response = api.resolveIncident(ResolveIncidentRequest(incidentId))
        if (!response.success) {
            error(response.message.ifBlank { "Failed to resolve incident" })
        }
    }

    private fun uriToTempFile(uri: Uri): File {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Cannot read image")
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out -> input.use { it.copyTo(out) } }
        return file
    }
}
