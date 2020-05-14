package com.elyonut.wow.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.elyonut.wow.R
import com.elyonut.wow.interfaces.ILocationService
import com.elyonut.wow.interfaces.ILogger
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.lang.ref.WeakReference

// Const values
private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

class LocationService(
    private var context: Context,
    var map: MapboxMap
) : ILocationService {
    private val logger: ILogger = TimberLogAdapter()
    private var lastUpdatedLocation = Location("")
    private var locationComponent = map.locationComponent
    private var locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationEngine: LocationEngine =
        LocationEngineProvider.getBestLocationEngine(context)
    val locationChangedSubscribers = mutableListOf<(Location) -> Unit>()
    private var callback = LocationUpdatesCallback(this)

    init { // Temp until we make it a singleton ot use Dagger
        logger.initLogger()
    }

    override fun getCurrentLocation() = lastUpdatedLocation

    override fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun startLocationService() {
        val myLocationComponentOptions = LocationComponentOptions.builder(context)
            .trackingGesturesManagement(true)
            .accuracyColor(ContextCompat.getColor(context, R.color.myLocationColor))
            .build()

        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(context, map.style!!)
                .locationComponentOptions(myLocationComponentOptions).build()

        locationComponent.apply {
            activateLocationComponent(locationComponentActivationOptions)
            isLocationComponentEnabled = true
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }

        initLocationEngine(context)
        logger.info("location engine initialized")
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine(context: Context) {
        val request: LocationEngineRequest = LocationEngineRequest.Builder(
            DEFAULT_INTERVAL_IN_MILLISECONDS
        )
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()

        locationEngine.requestLocationUpdates(request, callback, context.mainLooper)
        locationEngine.getLastLocation(callback)
    }

    override fun subscribeToLocationChanges(locationChangedSubscriber: (Location) -> Unit) {
        locationChangedSubscribers.add(locationChangedSubscriber)
    }

    override fun unsubscribeFromLocationChanges(locationChangedSubscriber: (Location) -> Unit) {
        locationChangedSubscribers.remove(locationChangedSubscriber)
    }

    override fun cleanLocationService() {
        locationEngine.removeLocationUpdates(callback)
        locationChangedSubscribers.clear()
    }

    private class LocationUpdatesCallback(locationService: LocationService) :
        LocationEngineCallback<LocationEngineResult> {
        private var locationServiceWeakReference: WeakReference<LocationService> =
            WeakReference(locationService)
        val logger = locationServiceWeakReference.get()?.logger

        override fun onSuccess(result: LocationEngineResult?) {
            val location: Location = result?.lastLocation ?: return
            val lastUpdatedLocation = locationServiceWeakReference.get()?.lastUpdatedLocation

            // don't recalculate if staying in the same location
            if (lastUpdatedLocation?.longitude == location.longitude &&
                lastUpdatedLocation.latitude == location.latitude
            ) {
                return
            }

            logger?.info("Location changed!")
            locationServiceWeakReference.get()?.lastUpdatedLocation = location
            locationServiceWeakReference.get()?.locationChangedSubscribers?.forEach {
                it(location)
            }
            locationServiceWeakReference.get()?.locationComponent?.forceLocationUpdate(location)
        }

        override fun onFailure(exception: java.lang.Exception) {
            val locationComponent = locationServiceWeakReference.get()?.locationComponent
            if (locationComponent != null) {
                logger?.error(exception.message + exception.stackTrace)
            }
        }
    }
}