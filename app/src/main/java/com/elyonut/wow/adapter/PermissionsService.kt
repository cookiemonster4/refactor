package com.elyonut.wow.adapter

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.elyonut.wow.SingletonHolder
import com.elyonut.wow.interfaces.IPermissions
import com.mapbox.android.core.permissions.PermissionsManager

class PermissionsService private constructor(private var context: Context) : IPermissions {
    private var hasPermissions = false

    companion object : SingletonHolder<PermissionsService, Context>(::PermissionsService)

    override fun isLocationPermitted(): Boolean {
        if ((PermissionsManager.areLocationPermissionsGranted(context))
            && (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            hasPermissions = true
        }

        return hasPermissions
    }
}