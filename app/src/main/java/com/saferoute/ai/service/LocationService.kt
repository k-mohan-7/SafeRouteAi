package com.saferoute.ai.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.saferoute.ai.R
import com.saferoute.ai.SafeRouteApp
import com.saferoute.ai.data.repository.IncidentRepository
import com.saferoute.ai.data.repository.NotificationRepository
import com.saferoute.ai.util.GeoUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var incidentRepository: IncidentRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val alertedIds = HashSet<Int>()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, SafeRouteApp.CHANNEL_SERVICE)
                .setContentTitle("SafeRouteAI")
                .setContentText("SafeRouteAI is monitoring your route")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
        )
        requestLocationUpdates()
        startProximityMonitoring()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (_: SecurityException) {}
    }

    private fun onLocation(location: Location) {
        val speedKmh = location.speed * 3.6f
        LocationStateHolder.update(location, speedKmh)
        scope.launch {
            notificationRepository.updateLocation(location.latitude, location.longitude, speedKmh)
        }
    }

    private fun startProximityMonitoring() {
        scope.launch {
            while (isActive) {
                val loc = LocationStateHolder.location.value
                if (loc != null) {
                    incidentRepository.getNearbyIncidents(loc.latitude, loc.longitude, 500)
                        .onSuccess { incidents ->
                            incidents.forEach { incident ->
                                if (incident.id in alertedIds) return@forEach
                                val dist = GeoUtils.distanceMeters(
                                    loc.latitude, loc.longitude,
                                    incident.latitude, incident.longitude
                                )
                                if (dist <= 300) {
                                    alertedIds.add(incident.id)
                                    showProximityAlert(incident.incidentType, dist)
                                }
                            }
                        }
                }
                delay(30_000)
            }
        }
    }

    private fun showProximityAlert(type: String, distanceM: Double) {
        val notification = NotificationCompat.Builder(this, SafeRouteApp.CHANNEL_PROXIMITY)
            .setContentTitle("Nearby Incident")
            .setContentText("⚠️ $type reported ${distanceM.toInt()}m ahead!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        androidx.core.app.NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}

object LocationStateHolder {
    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    fun update(location: Location, speed: Float) {
        _location.value = location
        _speedKmh.value = speed
    }
}
