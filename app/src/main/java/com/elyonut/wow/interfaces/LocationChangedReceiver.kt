package com.elyonut.wow.interfaces

import android.location.Location

interface LocationChangedReceiver {
    fun onLocationChanged(newLocation: Location)
}