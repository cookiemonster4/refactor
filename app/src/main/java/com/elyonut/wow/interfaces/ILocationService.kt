package com.elyonut.wow.interfaces

import android.location.Location

interface ILocationService {
    fun startLocationService()
    fun isGpsEnabled(): Boolean
    fun cleanLocationService()
    fun getCurrentLocation(): Location
    fun subscribeToLocationChanges(locationChangedSubscriber: (Location) -> Unit)
    fun unsubscribeFromLocationChanges(locationChangedSubscriber: (Location) -> Unit)
}