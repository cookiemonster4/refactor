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
import com.mapbox.mapboxsdk.geometry.LatLng

class CalcThreatStatusAsync(
    private val mapViewModel: MapViewModel
) : AsyncTask<LatLng, Int, RiskData>() {

    private val logger: ILogger = TimberLogAdapter()

    override fun doInBackground(vararg locations: LatLng): RiskData {
        val latLng = locations[0]
        var riskStatus = RiskStatus.LOW
        var threatFeaturesConstruction: List<FeatureModel> = ArrayList()
        logger.info("location changed, calculating threats!")
        threatFeaturesConstruction =
            mapViewModel.threatAnalyzer.getThreatFeaturesConstruction(latLng)

        if (threatFeaturesConstruction.isNotEmpty()) {
            riskStatus = RiskStatus.HIGH
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
            mapViewModel.threats.postValue(modelThreatList) // maybe we should check if the list is the same...
        }
    }
}