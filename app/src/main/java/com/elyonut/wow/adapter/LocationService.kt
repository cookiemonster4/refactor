package com.elyonut.wow.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.elyonut.wow.SingletonHolder
import com.elyonut.wow.interfaces.ILocationService
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.utilities.Constants
import com.mapbox.android.core.location.*
import java.lang.ref.WeakReference

// Const values
private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

class LocationService private constructor(private var context: Context) : ILocationService {
    private val logger: ILogger = TimberLogAdapter()
    private var lastUpdatedLocation = Location("")
    private val locationChangedSubscribers = mutableListOf<(Location) -> Unit>()
    private var callback = LocationUpdatesCallback(this)
    private var locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationEngine: LocationEngine =
        LocationEngineProvider.getBestLocationEngine(context)

    init {
        logger.initLogger() // Temp until we make it a singleton ot use Dagger
    }

    companion object : SingletonHolder<LocationService, Context>(::LocationService)

    override fun getCurrentLocation() = lastUpdatedLocation

    override fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun startLocationService() {
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
            if (location.distanceTo(lastUpdatedLocation) <= Constants.MAX_DISTANCE_TO_CURRENT_LOCATION) {
                return
            }

            logger?.info(
                "Location changed! New location is: latitude- " +
                        location.latitude + " longitude- " + location.longitude
            )
            locationServiceWeakReference.get()?.lastUpdatedLocation = location
            locationServiceWeakReference.get()?.locationChangedSubscribers?.forEach {
                it(location)
            }
        }

        override fun onFailure(exception: java.lang.Exception) {
            logger?.error(exception.message + exception.stackTrace)
        }
    }
}