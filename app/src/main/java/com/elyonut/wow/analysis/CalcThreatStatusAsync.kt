package com.elyonut.wow.analysis

import android.os.AsyncTask
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.model.RiskData
import com.elyonut.wow.model.RiskStatus
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.Threat
import com.elyonut.wow.parser.MapboxParser
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfMeasurement

class CalcThreatStatusAsync(
    private val mapViewModel: MapViewModel,
    private val isManualSelection: Boolean
) : AsyncTask<LatLng, Int, RiskData>() {

    private val logger: ILogger = TimberLogAdapter()

    override fun doInBackground(vararg locations: LatLng): RiskData {
        val latLng = locations[0]
        val allFeatures = mapViewModel.layerManager.getLayer(Constants.THREAT_LAYER_ID)
        var riskStatus = RiskStatus.LOW
        var threatFeaturesConstruction: List<FeatureModel> = ArrayList()
        if (allFeatures != null) {
            logger.info("location changed, calculating threats!")
            threatFeaturesConstruction =
                mapViewModel.threatAnalyzer.getThreatFeaturesConstruction(latLng, allFeatures)

            if (threatFeaturesConstruction.isNotEmpty()) {
                riskStatus = RiskStatus.HIGH
            }
        }

        return RiskData(latLng, riskStatus, threatFeaturesConstruction)
    }

    override fun onPostExecute(result: RiskData?) {
        logger.info("threats calculated!")
        if (result != null
        ) {
            val threatList = result.threatList
            val features = ArrayList<Feature>()
            val modelThreatList = ArrayList<Threat>()
            for (threat in threatList) {
                // create feature for drawing
                val feature = MapboxParser.parseToMapboxFeature(threat)
                features.add(feature)

                //create threat model for UI list
                val modelThreat = Threat()
                modelThreat.feature = feature
                modelThreat.isLos = true
                modelThreat.name = threat.properties!!["namestr"].asString
                modelThreat.type = threat.properties!!["type"].asString
                modelThreat.level = KnowledgeBase.getThreatLevel(modelThreat.type)
                val height = threat.properties!!["height"]
                if(height != null) {
                    modelThreat.height = height.asDouble
                }

                val coordinates = (feature.geometry() as Polygon).coordinates()
                val featureLatitude = coordinates[0][0].latitude()
                val featureLongitude = coordinates[0][0].longitude()
                val featureLocation = LatLng(featureLatitude, featureLongitude)

                modelThreat.distanceMeters = result.currentLocation.distanceTo(featureLocation)
                modelThreat.azimuth = bearingToAzimuth(
                    TurfMeasurement.bearing(
                        Point.fromLngLat(result.currentLocation.longitude, result.currentLocation.latitude),
                        Point.fromLngLat(featureLongitude, featureLatitude)))
                modelThreatList.add(modelThreat)

            }
            val selectedBuildingSource: GeoJsonSource? = if (isManualSelection){
                mapViewModel.threatAnalyzer.mapboxMap.style?.getSourceAs(Constants.SELECTED_BUILDING_SOURCE_ID)
            } else{
                mapViewModel.threatAnalyzer.mapboxMap.style?.getSourceAs(Constants.ACTIVE_THREATS_SOURCE_ID)
            }
            selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))
            if(!isManualSelection){
                mapViewModel.threats.postValue(modelThreatList)
                mapViewModel.riskStatus.postValue(result.riskStatus)
            }
        }
    }

    private fun bearingToAzimuth(bearing: Double): Double {
        var angle = bearing % 360
        if (angle < 0) { angle += 360; }
        return angle
    }
}