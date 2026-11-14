package com.wergogo.travelnote.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/**
 * Simple wrapper around FusedLocationProviderClient.
 * Returns null if no location permission is granted or location is unavailable.
 */
object LocationHelper {

    /** Request these together: Android 12+ ignores a request for precise location on its own. */
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        // Users may allow only approximate location; ask for the accuracy they granted.
        val priority = when {
            granted(Manifest.permission.ACCESS_FINE_LOCATION) -> Priority.PRIORITY_HIGH_ACCURACY
            granted(Manifest.permission.ACCESS_COARSE_LOCATION) -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            else -> return null
        }

        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = client.getCurrentLocation(priority, cts.token).await()
            location?.let { Pair(it.latitude, it.longitude) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
