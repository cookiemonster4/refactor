package com.elyonut.wow.analysis

import android.os.AsyncTask
import com.elyonut.wow.App
import com.elyonut.wow.analysis.dal.CoverageReaderDbHelper
import com.elyonut.wow.model.Coordinate
import com.mapbox.mapboxsdk.geometry.LatLng


object CoverageCacheManager {

    private var dbHelper: CoverageReaderDbHelper =
        CoverageReaderDbHelper(App.instance!!.baseContext)

    fun addCoverage(
        featureId: String,
        radius: Double,
        resolution: Double,
        heightMeters: Double,
        coverageCoords: List<Coordinate>
    ) {
        dbHelper.insert(featureId, radius, resolution, heightMeters, coverageCoords)
    }

    fun getCoverage(
        featureId: String,
        radius: Double,
        resolution: Double,
        heightMeters: Double
    ): List<Coordinate> {
        /*return GetCoverageAsync().execute(
            ThreatCoverageQueryData(
                featureId,
                radius,
                resolution,
                heightMeters
            )
        ).get()*/
        return dbHelper.getCoverageCoords(featureId, radius, resolution, heightMeters)
    }

    fun removeCoverage(featureId: String, heightMeters: Double) {
        dbHelper.remove(featureId, heightMeters)
    }

    private class GetCoverageAsync : AsyncTask<ThreatCoverageQueryData, Int, List<Coordinate>>() {
        override fun doInBackground(vararg coverageData: ThreatCoverageQueryData): List<Coordinate> {
            val input = coverageData[0]
            return dbHelper.getCoverageCoords(
                input.featureId,
                input.rangeMeters,
                input.pointResolutionMeters,
                input.heightMeters
            )
        }

    }

}

class ThreatCoverageQueryData(
    val featureId: String,
    val rangeMeters: Double,
    val pointResolutionMeters: Double,
    val heightMeters: Double
) {
}