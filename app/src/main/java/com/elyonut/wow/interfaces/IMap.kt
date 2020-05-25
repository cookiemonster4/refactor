package com.elyonut.wow.interfaces

import android.graphics.Color
import com.elyonut.wow.model.FeatureModel
import java.util.*


interface IMap {
    fun addLayer(layerId: String)

    fun removeLayer(layerId: String)

    fun colorFilter(layerId: String, colorsList: Dictionary<Int, Color>)

    fun initOfflineMap()
}