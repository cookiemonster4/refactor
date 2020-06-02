package com.elyonut.wow.analysis

import android.os.AsyncTask
import android.widget.ProgressBar
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.viewModel.MapViewModel

class CalcThreatCoverageAllConstructionAsync(
    private val mapViewModel: MapViewModel,
    private val progressBar: ProgressBar

) : AsyncTask<ThreatCoverageData, Int, Unit>() {

    private val logger: ILogger = TimberLogAdapter()

    override fun doInBackground(vararg coverageData: ThreatCoverageData?) {
        val input = coverageData[0]
        if (input != null) {
            logger.info("calculating coverage for all enemies!")

            val allFeatures =
                mapViewModel.vectorLayersManager.getLayerById(Constants.THREAT_LAYER_ID)


            mapViewModel.threatAnalyzer.calculateCoverageAlpha(
                input.pointResolutionMeters,
                input.heightMeters
            )
        }
    }

    override fun onPostExecute(result: Unit?) {
        progressBar.visibility = android.view.View.GONE
        logger.info("coverage calculated!")
    }

}

class ThreatCoverageData(val pointResolutionMeters: Double, val heightMeters: Double)