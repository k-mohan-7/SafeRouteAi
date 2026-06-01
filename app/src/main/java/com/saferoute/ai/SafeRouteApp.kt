package com.saferoute.ai

import android.app.Application
import android.app.Notification
import android.media.RingtoneManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class SafeRouteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        val basePath = java.io.File(filesDir, "osmdroid").apply { mkdirs() }
        val tileCache = java.io.File(basePath, "tile").apply { mkdirs() }
        Configuration.getInstance().apply {
            osmdroidBasePath = basePath
            osmdroidTileCache = tileCache
            userAgentValue = BuildConfig.APPLICATION_ID
        }
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_SERVICE,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName("Location Tracking")
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                CHANNEL_PROXIMITY,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName("Nearby Incident Alerts")
                .setVibrationEnabled(true)
                .setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    Notification.AUDIO_ATTRIBUTES_DEFAULT
                )
                .build()
        )
    }

    companion object {
        const val CHANNEL_SERVICE = "saferoute_service"
        const val CHANNEL_PROXIMITY = "saferoute_proximity_alerts"
    }
}
