package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient? by lazy {
        try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: Throwable) {
            android.util.Log.e("LocationHelper", "Failed to retrieve FusedLocationProviderClient", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse && !hasFine) {
            return null
        }

        // 1. Prioritize safe, lightweight, native LocationManager (Last Known Location)
        // This is extremely safe and does not trigger complex GMS Play Services binders.
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            if (locationManager != null) {
                // Try GPS_PROVIDER first for accuracy, then NETWORK_PROVIDER, then PASSIVE_PROVIDER
                val providers = listOf(
                    android.location.LocationManager.GPS_PROVIDER,
                    android.location.LocationManager.NETWORK_PROVIDER,
                    android.location.LocationManager.PASSIVE_PROVIDER
                )
                for (provider in providers) {
                    try {
                        if (locationManager.isProviderEnabled(provider)) {
                            val loc = locationManager.getLastKnownLocation(provider)
                            if (loc != null) {
                                android.util.Log.d("LocationHelper", "Retrieved last known location via native provider: $provider")
                                return loc
                            }
                        }
                    } catch (e: Throwable) {
                        // Suppress individual provider errors
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("LocationHelper", "Native LocationManager check failed", e)
        }

        // 2. Fallback to GMS FusedLocationProviderClient lastLocation (Passive lookups are low overhead)
        fusedLocationClient?.let { client ->
            try {
                val loc = client.lastLocation.await()
                if (loc != null) {
                    android.util.Log.d("LocationHelper", "Retrieved last known location via GMS client")
                    return loc
                }
            } catch (e: Throwable) {
                android.util.Log.w("LocationHelper", "lastLocation from fused client failed", e)
            }
        }

        // 3. Last fallback: Try native LocationManager live check or GMS getCurrentLocation
        // We do this with extreme caution to prevent any thread-blocking or unhandled Play Services loops.
        fusedLocationClient?.let { client ->
            try {
                val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                if (loc != null) {
                    android.util.Log.d("LocationHelper", "Retrieved current location via GMS client")
                    return loc
                }
            } catch (e: Throwable) {
                android.util.Log.w("LocationHelper", "getCurrentLocation from fused client failed", e)
            }
        }

        return null
    }
}
