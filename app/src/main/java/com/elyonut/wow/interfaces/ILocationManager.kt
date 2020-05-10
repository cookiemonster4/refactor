package com.elyonut.wow.interfaces

import android.location.Location

interface ILocationManager {
    fun startLocationService()
    fun isGpsEnabled(): Boolean
    fun cleanLocation()
    fun getCurrentLocation() : Location?
    fun subscribe(locationChangedSubscriber: LocationChangedReceiver)
    fun unsubscribe(locationChangedSubscriber: LocationChangedReceiver)
}