package com.saferoute.ai.util

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GeocoderUtil {
    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale.getDefault())
                val addr = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() ?: return@runCatching null
                listOfNotNull(addr.subLocality, addr.locality, addr.adminArea)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .ifBlank { addr.getAddressLine(0) }
            }.getOrNull()
        }
}
