package com.sdevprem.runtrack.shared.data.tracking.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.sdevprem.runtrack.shared.domain.tracking.location.LocationTrackingManager
import com.sdevprem.runtrack.shared.domain.tracking.model.LocationInfo
import com.sdevprem.runtrack.shared.domain.tracking.model.LocationTrackingInfo

@SuppressLint("MissingPermission")
class DefaultLocationTrackingManager(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val context: Context,
    private val locationRequest: LocationRequest
) : LocationTrackingManager {

    private var locationCallback: LocationTrackingManager.LocationCallback? = null
    private val gLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            locationCallback?.onLocationUpdate(
                p0.locations.mapNotNull {
                    it?.let {
                        LocationTrackingInfo(
                            locationInfo = LocationInfo(it.latitude, it.longitude),
                            speedInMS = it.speed
                        )
                    }
                }
            )
        }
    }
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private val platformLocationListener = LocationListener { location ->
        locationCallback?.onLocationUpdate(listOf(location.toTrackingInfo()))
    }
    private var usingPlatformLocationFallback = false

    override fun setCallback(locationCallback: LocationTrackingManager.LocationCallback) {
        if (/*context.hasLocationPermission()*/ true) { //todo: add permission check
            this.locationCallback = locationCallback
            val startedWithFused = runCatching {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    gLocationCallback,
                    Looper.getMainLooper()
                )
            }.isSuccess
            if (!startedWithFused) {
                startPlatformLocationUpdates()
            }
        }
    }

    override fun removeCallback() {
        this.locationCallback = null
        fusedLocationProviderClient.removeLocationUpdates(gLocationCallback)
        if (usingPlatformLocationFallback) {
            runCatching { locationManager.removeUpdates(platformLocationListener) }
            usingPlatformLocationFallback = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun startPlatformLocationUpdates() {
        usingPlatformLocationFallback = true
        val minTimeMs = locationRequest.intervalMillis
        runCatching {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                0f,
                platformLocationListener,
                Looper.getMainLooper()
            )
        }
        runCatching {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTimeMs,
                0f,
                platformLocationListener,
                Looper.getMainLooper()
            )
        }
    }

    private fun Location.toTrackingInfo(): LocationTrackingInfo {
        return LocationTrackingInfo(
            locationInfo = LocationInfo(latitude, longitude),
            speedInMS = speed
        )
    }
}