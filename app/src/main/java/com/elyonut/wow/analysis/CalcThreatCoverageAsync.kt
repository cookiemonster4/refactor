package com.elyonut.wow.analysis

import android.os.AsyncTask
import android.widget.ProgressBar
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.viewModel.MapViewModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class CalcThreatCoverageAsync(
    private val mapViewModel: MapViewModel,
    private val progressBar: ProgressBar

    ) : AsyncTask<ThreatCoverageData, Int, List<Coordinate>>() {

    private val logger: ILogger = TimberLogAdapter()

    override fun doInBackground(vararg coverageData: ThreatCoverageData?): List<Coordinate> {
        val input = coverageData[0]
        if (input != null) {
            logger.info("calculating coverage!")

            return mapViewModel.threatAnalyzer.calculateCoverageAlpha(input.currentLocation, input.rangeMeters, input.pointResolutionMeters, input.heightMeters)
        }

        return ArrayList()
    }

    override fun onPostExecute(result: List<Coordinate>) {
        val threatCoverageSource: GeoJsonSource? = mapViewModel.threatAnalyzer.mapboxMap.style?.getSourceAs(
            Constants.THREAT_COVERAGE_SOURCE_ID)

        val features = result.map { c-> Feature.fromGeometry(Point.fromLngLat(c.longitude, c.latitude)) }

        threatCoverageSource?.setGeoJson(FeatureCollection.fromFeatures(features))


        mapViewModel.setLayerVisibility(
            Constants.THREAT_COVERAGE_LAYER_ID,
            PropertyFactory.visibility(Property.VISIBLE)
        )

        progressBar.visibility = android.view.View.GONE
        logger.info("coverage calculated!")
    }

}

class ThreatCoverageData(
    val currentLocation: LatLng,
    val rangeMeters: Double,
    val pointResolutionMeters: Double,
    val heightMeters: Double
) {
}
