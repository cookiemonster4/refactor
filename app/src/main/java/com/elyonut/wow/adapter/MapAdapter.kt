package com.elyonut.wow.adapter

import android.graphics.Color
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.IMap
import com.elyonut.wow.VectorLayersManager
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.parser.MapboxParser
import com.google.gson.JsonObject
import com.mapbox.geojson.*
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeta
import com.mapbox.turf.TurfTransformation
import java.util.*
import kotlin.collections.ArrayList

private const val circleUnit = TurfConstants.UNIT_KILOMETERS
private const val circleSteps = 180

class MapAdapter(var vectorLayersManager: VectorLayersManager) : IMap {

    override fun addLayer(layerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeLayer(layerId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun colorFilter(layerId: String, colorsList: Dictionary<Int, Color>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun initOfflineMap() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}