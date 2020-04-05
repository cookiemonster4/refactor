package com.elyonut.wow.adapter

import android.graphics.Color
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.IMap
import com.elyonut.wow.LayerManager
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

class MapAdapter(var layerManager: LayerManager) : IMap {

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

    override fun createThreatRadiusSource(): ArrayList<FeatureModel> {
        val circleLayerFeatureList = ArrayList<FeatureModel>()
        val allFeatures = (layerManager.getLayer(Constants.THREAT_LAYER_ID))
        allFeatures?.forEach {
            val circlePolygonArea = createCirclePolygonArea(it)
            val properties = createThreatProperties(it)

            if (circlePolygonArea != null) {
                val feature = MapboxParser.parseToFeatureModel(
                    Feature.fromGeometry(
                        Polygon.fromOuterInner(
                            LineString.fromLngLats(TurfMeta.coordAll(circlePolygonArea, false))
                        ), properties
                    )
                )

                it.geometry = feature.geometry
                circleLayerFeatureList.add(it)
            }
        }

        return circleLayerFeatureList
    }

    private fun createCirclePolygonArea(feature: FeatureModel): Polygon? {
        var circlePolygon: Polygon? = null
        val currentLatitude = feature.properties?.get("latitude")
        val currentLongitude = feature.properties?.get("longitude")

        if ((currentLatitude != null) && (currentLongitude != null)) {
            val featureRiskRadius = feature.properties?.get("radius").let { t -> t?.asDouble }
            val currPoint = Point.fromLngLat(currentLongitude.asDouble, currentLatitude.asDouble)
            circlePolygon = TurfTransformation.circle(currPoint, featureRiskRadius!!,
                circleSteps,
                circleUnit
            )
        }

        return circlePolygon
    }

    private fun createThreatProperties(feature: FeatureModel): JsonObject {
        val featureThreatLevel = feature.properties?.get("risk")
        val properties = JsonObject()

        if (featureThreatLevel != null) {
            properties.add(Constants.THREAT_PROPERTY, featureThreatLevel)
        }

        return properties
    }
}