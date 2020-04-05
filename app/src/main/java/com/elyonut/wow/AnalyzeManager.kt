package com.elyonut.wow

import android.location.Location
import com.elyonut.wow.interfaces.IAnalyze
import com.elyonut.wow.model.RiskStatus
import com.elyonut.wow.utilities.Constants
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private const val MY_RISK_RADIUS = 0.3

class AnalyzeManager(private val layerManager: LayerManager) : IAnalyze {

    /**
     * calc the risk status according to my location.
     * return: the ids of the threat by their risk status
     */
    override fun calcRiskStatus(location: Location): Pair<RiskStatus, HashMap<RiskStatus, ArrayList<String>>> {
        val allFeatures = layerManager.getLayer(Constants.THREAT_LAYER_ID)
        var threatLocation: LatLng
        lateinit var riskStatus: RiskStatus
        val threatIdsByRiskStatus = HashMap<RiskStatus, ArrayList<String>>()
        var currentThreatId: String?

        threatIdsByRiskStatus[RiskStatus.LOW] = ArrayList()
        threatIdsByRiskStatus[RiskStatus.MEDIUM] = ArrayList()
        threatIdsByRiskStatus[RiskStatus.HIGH] = ArrayList()

        run {
            allFeatures?.forEach {
                riskStatus = RiskStatus.LOW
                val threatLat = it.properties?.get("latitude")
                val threatLng = it.properties?.get("longitude")
                currentThreatId = it.id

                if (threatLat != null && threatLng != null) {
                    threatLocation = LatLng(threatLat.asDouble, threatLng.asDouble)
                    val threatRiskRadius = it.properties?.get("radius").let { t -> t?.asDouble }
                    val userLocation = LatLng(location.latitude, location.longitude)
                    val distInKilometers = threatLocation.distanceTo(userLocation) / 1000

                    if (distInKilometers < (MY_RISK_RADIUS + threatRiskRadius!!)) {
                        riskStatus = RiskStatus.MEDIUM

                        if (distInKilometers < threatRiskRadius) {
                            riskStatus = RiskStatus.HIGH
                        }
                    }

                    threatIdsByRiskStatus[riskStatus]?.add(currentThreatId!!)
                }
            }
        }

        riskStatus = when {
            threatIdsByRiskStatus[RiskStatus.HIGH]!!.isNotEmpty() -> RiskStatus.HIGH
            threatIdsByRiskStatus[RiskStatus.MEDIUM]!!.isNotEmpty() -> RiskStatus.MEDIUM
            else -> RiskStatus.LOW
        }

        return Pair(riskStatus, threatIdsByRiskStatus)
    }
}