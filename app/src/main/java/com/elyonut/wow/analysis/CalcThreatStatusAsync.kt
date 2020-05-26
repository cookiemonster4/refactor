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
        val threatLayerFeatures =
            mapViewModel.mapVectorLayersManager.getLayerById(Constants.THREAT_LAYER_ID)
        var riskStatus = RiskStatus.LOW
        var threatFeaturesConstruction: List<FeatureModel> = ArrayList()
        if (threatLayerFeatures != null) {
            logger.info("location changed, calculating threats!")
            threatFeaturesConstruction =
                mapViewModel.threatAnalyzer.getThreatFeaturesConstruction(
                    latLng,
                    threatLayerFeatures
                )

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
                val modelThreat = mapViewModel.threatAnalyzer.featureModelToThreat(
                    threat,
                    result.currentLocation,
                    true
                )
                modelThreatList.add(modelThreat)
            }
            val selectedBuildingSource: GeoJsonSource? = if (isManualSelection) {
                mapViewModel.map.style?.getSourceAs(Constants.SELECTED_BUILDING_SOURCE_ID)
            } else {
                mapViewModel.map.style?.getSourceAs(Constants.ACTIVE_THREATS_SOURCE_ID)
            }
            selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(features))

            if (!isManualSelection) {
                mapViewModel.threats.postValue(modelThreatList) // maybe we should check if the list is the same...
            }
        }
    }
}