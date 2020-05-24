package com.elyonut.wow.analysis

import com.elyonut.wow.model.Coordinate
import com.mapbox.mapboxsdk.geometry.LatLng

class CalcThreatCoverageAsync1(var threatAnalyzer: ThreatAnalyzer) {

    suspend fun calculateCoverageAlpha(currentLocation: LatLng,
                  rangeMeters: Double,
                  pointResolutionMeters: Double,
                  heightMeters: Double): List<Coordinate> {
        
         return threatAnalyzer.filterWithLOSCoordinatesAlpha(
            currentLocation,
            rangeMeters,
            pointResolutionMeters,
            heightMeters,
            true
        )
    }
}