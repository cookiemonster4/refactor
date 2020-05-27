package com.elyonut.wow.analysis

import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.Threat
import com.mapbox.mapboxsdk.geometry.LatLng

class Calculations(var threatAnalyzer: ThreatAnalyzer) {

    fun calculateCoverageAlpha(
        currentLocation: LatLng,
        rangeMeters: Double,
        pointResolutionMeters: Double,
        heightMeters: Double
    ): List<Coordinate> {
        return threatAnalyzer.filterWithLOSCoordinatesAlpha(
            currentLocation,
            rangeMeters,
            pointResolutionMeters,
            heightMeters,
            true
        )
    }

    fun calcThreatStatus(latLng: LatLng): List<Threat> {
        return threatAnalyzer.filterWithLOSModelFeatures(latLng).map { threatFeature ->
            threatAnalyzer.featureModelToThreat(
                threatFeature,
                latLng,
                true
            )
        }
    }
}