package com.saferoute.ai.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TimeUtils {
    private val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun timeAgo(dateString: String): String {
        return try {
            val date = parser.parse(dateString) ?: return dateString
            val diffMs = System.currentTimeMillis() - date.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            when {
                minutes < 1 -> "just now"
                minutes < 60 -> "$minutes min ago"
                minutes < 1440 -> "${minutes / 60} hours ago"
                minutes < 2880 -> "yesterday"
                else -> "${minutes / 1440} days ago"
            }
        } catch (_: Exception) {
            dateString
        }
    }
}

object GeoUtils {
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) String.format("%.1f km", meters / 1000)
        else "${meters.toInt()} m"
    }
}
