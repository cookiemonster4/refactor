package com.elyonut.wow.analysis

import android.content.Context
import android.location.Location
import com.elyonut.wow.SingletonHolder
import com.elyonut.wow.VectorLayersManager
import com.elyonut.wow.adapter.LocationService
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.interfaces.ILocationService
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.Threat
import com.elyonut.wow.parser.MapboxParser
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.utilities.TempDB
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

class ThreatAnalyzer private constructor(context: Context) {
    private val logger: ILogger = TimberLogAdapter()
    private val topographyService = TopographyService
    private val vectorLayersManager = VectorLayersManager.getInstance(context)
    private val locationService: ILocationService = LocationService.getInstance(context)
    private val threatLayer = TempDB.getInstance(context).getThreatLayer().map { Threat(it) }
    var currentThreats: List<Threat> = listOf()

    companion object : SingletonHolder<ThreatAnalyzer, Context>(::ThreatAnalyzer)

    init {
        vectorLayersManager.addLayer(
            Constants.THREAT_LAYER_ID,
            Constants.THREAT_LAYER_NAME,
            threatLayer
        )
        vectorLayersManager.addLayer(
            Constants.ACTIVE_THREATS_LAYER_ID,
            Constants.ACTIVE_THREATS_LAYER_ID,
            currentThreats
        )
        locationService.subscribeToLocationChanges { locationChanged(it) }
    }

    private fun locationChanged(location: Location) {
        CoroutineScope(Dispatchers.Main).launch {
            currentThreats =
                withContext(Dispatchers.Default) {
                    calculateThreats(
                        LatLng(
                            location.latitude,
                            location.longitude,
                            location.altitude
                        )
                    )
                }
        }.invokeOnCompletion {
            vectorLayersManager.updateLayer(Constants.ACTIVE_THREATS_LAYER_ID, currentThreats)
        }
    }

    private fun calculateThreats(latLng: LatLng): List<Threat> {
        return filterWithLOSModelFeatures(latLng).map { threatFeature ->
            featureToThreat(threatFeature, latLng, true)
        }
    }

    // when in Los it means the building is a threat?
    // can we take it somewhere else so we won't need the map here?
    fun getBuildingsWithinLOS(
        currentLocation: LatLng,
        buildingAtLocation: Feature?
    ): List<FeatureModel> {
        val currentLocationCoordinate =
            Coordinate(currentLocation.latitude, currentLocation.longitude)
        val buildings =
            vectorLayersManager.getLayerById(Constants.BUILDINGS_LAYER_ID)

        return buildings?.filter {
            topographyService.isThreatBuilding(
                currentLocationCoordinate,
                MapboxParser.parseToMapboxFeature(it),
                buildingAtLocation
            )
        } ?: arrayListOf()
    }

    // Do we need this?
    fun calculateCoverage(
        currentLocation: LatLng,
        rangeMeters: Double,
        pointResolutionMeters: Double
    ): List<Coordinate> {
        val square = calculateCoverageSquare(currentLocation, rangeMeters, pointResolutionMeters)

        return filterWithLOSCoordinates(square, currentLocation)
    }

    fun calculateCoverageAlpha(
        currentLocation: LatLng,
        rangeMeters: Double,
        pointResolutionMeters: Double,
        heightMeters: Double
    ): List<Coordinate> {
        return filterWithLOSCoordinatesAlpha(
            currentLocation,
            rangeMeters,
            pointResolutionMeters,
            heightMeters,
            true
        )
    }

    fun calculateCoverageAlpha(
        pointResolutionMeters: Double,
        heightMeters: Double
    ) {
        threatLayer.forEach { threat ->
            val threatType = threat.enemyType
            if (!threatType.contains("mikush")) {

                val threatRangeMeters = KnowledgeBase.getRangeMeters(threatType)
                val buildingsAtZone = topographyService.getBuildingsAtZone(threat)

                logger.info("calculating $threatType, $threatRangeMeters meters, ${buildingsAtZone.size} buildings")
                buildingsAtZone.forEach { building ->

                    val corners = topographyService.getCoordinates(building.coordinates)
                    corners.forEach { bc ->
                        bc.heightMeters = building.properties["height"]!!.toDouble()
                    }

                    filterWithLOSFeatureModelAlpha(
                        building.properties["id"]!!,
                        corners,
                        threatRangeMeters,
                        pointResolutionMeters,
                        heightMeters,
                        true
                    )
                }
            }
        }
    }

    private fun filterWithLOSModelFeatures(
        currentLocation: LatLng
    ): List<FeatureModel> {
        val currentLocationCoordinate =
            Coordinate(currentLocation.latitude, currentLocation.longitude)
        return threatLayer.parallelStream().filter { threat ->
            topographyService.isThreat(
                currentLocationCoordinate,
                threat
            )
        }.collect(
            Collectors.toList()
        )
    }

    private fun filterWithLOSCoordinates(
        coverageSquare: List<Coordinate>,
        currentLocation: LatLng
    ): List<Coordinate> {
        val currentLocationCoordinate =
            Coordinate(currentLocation.latitude, currentLocation.longitude)
        val currentLocationExploded =
            topographyService.explodeLocationCoordinate(currentLocationCoordinate)

        return coverageSquare.filter { coordinate ->
            topographyService.isLOS(
                coordinate,
                currentLocationExploded
            )
        }
    }

    private fun filterWithLOSCoordinatesAlpha(
        currentLocation: LatLng,
        rangeMeters: Double,
        pointResolutionMeters: Double,
        heightMeters: Double,
        fromCache: Boolean
    ): List<Coordinate> {
        // First, if requested, attempt to load from cache
        val featureIdAtLocation = topographyService.featureIdAtLocation(
            currentLocation.longitude,
            currentLocation.latitude
        )

        var visiblePoints: List<Coordinate>? = null
//        if (featureIdAtLocation != null && fromCache) { // we store only features in cache
//            visiblePoints = CoverageCacheManager.getCoverage(
//                featureIdAtLocation,
//                rangeMeters,
//                pointResolutionMeters,
//                heightMeters
//            )
//        }

        // Either fromCache was false or the object was not found, so
        // call forceMissionCoverage to create it
        if (visiblePoints == null || visiblePoints.isEmpty()) {
            val currentLocationCoord =
                Coordinate(currentLocation.latitude, currentLocation.longitude)
            val currentLocationExploded =
                topographyService.explodeLocationCoordinate(currentLocationCoord)
            visiblePoints = filterWithLOSCoordinatesAlpha(
                currentLocationExploded,
                rangeMeters,
                pointResolutionMeters,
                heightMeters
            )

//            if (featureIdAtLocation != null && fromCache) { // we store only features in cache
//                CoverageCacheManager.removeCoverage(
//                    featureIdAtLocation,
//                    heightMeters
//                ) //remove any existing points (lower resolution / range) on the same height
//                CoverageCacheManager.addCoverage(
//                    featureIdAtLocation,
//                    rangeMeters,
//                    pointResolutionMeters,
//                    heightMeters,
//                    visiblePoints
//                )
//            }
        }

        return visiblePoints
    }

    private fun filterWithLOSFeatureModelAlpha(
        featureIdAtLocation: String,
        explodedCoordinates: List<Coordinate>,
        rangeMeters: Double,
        pointResolutionMeters: Double,
        heightMeters: Double,
        fromCache: Boolean
    ) {
        // First, if requested, attempt to load from cache
        var visiblePoints: List<Coordinate>? = null
        if (fromCache) { // we store only features in cache
            visiblePoints = CoverageCacheManager.getCoverage(
                featureIdAtLocation,
                rangeMeters,
                pointResolutionMeters,
                heightMeters
            )
        }

        // Either fromCache was false or the object was not found, so
        // call forceMissionCoverage to create it
        if (visiblePoints == null || visiblePoints.isEmpty()) {
            visiblePoints = filterWithLOSCoordinatesAlpha(
                explodedCoordinates,
                rangeMeters,
                pointResolutionMeters,
                heightMeters
            )

            if (fromCache) { // we store only features in cache
                CoverageCacheManager.removeCoverage(
                    featureIdAtLocation,
                    heightMeters
                ) //remove any existing points (lower resolution / range) on the same height
                CoverageCacheManager.addCoverage(
                    featureIdAtLocation,
                    rangeMeters,
                    pointResolutionMeters,
                    heightMeters,
                    visiblePoints
                )
            }
        }
    }

    private fun filterWithLOSCoordinatesAlpha(
        explodedCoordinates: List<Coordinate>,
        rangeMeters: Double,
        pointResolutionMeters: Double,
        heightMeters: Double
    ): List<Coordinate> {

        if (heightMeters != Constants.DEFAULT_COVERAGE_HEIGHT_METERS) {
            explodedCoordinates.forEach { coordinate -> coordinate.heightMeters = heightMeters }
        }
        val visiblePoints = ArrayList<Coordinate>()
        explodedCoordinates.forEach { origin ->
            visiblePoints.addAll(
                calculateVisibleProjections(
                    origin,
                    rangeMeters,
                    pointResolutionMeters
                )
            )
        }

        //return visiblePoints
        return visiblePoints.parallelStream()
            .filter { coordinate -> !topographyService.isInsideBuilding(coordinate) }
            .collect(Collectors.toList()) // filter buildings
    }

    // Needs to be without Model in the name when we delete the other function
    fun featureToThreat(
        feature: FeatureModel,
        currentLocation: LatLng,
        isLos: Boolean
    ): Threat {
        val threat = Threat(feature)
        enrichThreat(threat, currentLocation, isLos)
        return threat
    }

    // Where should this be? here or in Threat class? needs to be rewritten without mapbox
    private fun enrichThreat(threat: Threat, currentLocation: LatLng, isLOS: Boolean) {
        threat.azimuth = bearingToAzimuth(
            TurfMeasurement.bearing( // how to get without mapbox?
                Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
                Point.fromLngLat(threat.longitude, threat.latitude)
            )
        )

        threat.distanceMeters =
            currentLocation.distanceTo(LatLng(threat.latitude, threat.longitude))
        threat.isLos = isLOS
    }

    private fun bearingToAzimuth(bearing: Double): Double {
        var angle = bearing % 360
        if (angle < 0) {
            angle += 360; }
        return angle
    }

    private fun calculateCoverageSquare(
        currentLocation: LatLng,
        rangeMeters: Double,
        pointResolutionMeters: Double
    ): List<Coordinate> {

        val top = topographyService.destinationPoint(
            currentLocation.latitude,
            currentLocation.longitude,
            rangeMeters,
            0.0
        )
        val right = topographyService.destinationPoint(
            currentLocation.latitude,
            currentLocation.longitude,
            rangeMeters,
            90.0
        )
        val bottom = topographyService.destinationPoint(
            currentLocation.latitude,
            currentLocation.longitude,
            rangeMeters,
            180.0
        )
        val left = topographyService.destinationPoint(
            currentLocation.latitude,
            currentLocation.longitude,
            rangeMeters,
            270.0
        )

        val topLeft = Coordinate(top.latitude, left.longitude)
        val topRight = Coordinate(top.latitude, right.longitude)
        val bottomRight = Coordinate(bottom.latitude, right.longitude)
        val bottomLeft = Coordinate(bottom.latitude, left.longitude)

        val square =
            getGridPoints(topLeft, topRight, bottomRight, bottomLeft, pointResolutionMeters)
        val currentLocationCoord = Coordinate(currentLocation.latitude, currentLocation.longitude)
        val filteredByDistance = square.parallelStream().filter { coordinate ->
            topographyService.distanceMeters(
                currentLocationCoord,
                coordinate
            ) <= rangeMeters
        }.collect(Collectors.toList())

        return filteredByDistance
    }

    // TODO There are some problems here
    private fun getGridPoints(
        topLeft: Coordinate,
        topRight: Coordinate,
        bottomRight: Coordinate,
        bottomLeft: Coordinate,
        pointResolutionMeters: Double
    ): ArrayList<Coordinate> {
        val ans = ArrayList<Coordinate>()

        val leftBearing =
            topographyService.bearing(bottomLeft, topLeft)
        val rightBearing =
            topographyService.bearing(bottomRight, topRight)
        var runningLeft = bottomLeft
        var runningRight = bottomRight
        do {
            val coordinates = topographyService.calcRoutePointsLinear(
                runningLeft,
                runningRight,
                false,
                pointResolutionMeters
            )
            ans.addAll(
                coordinates.parallelStream()
                    .filter { coordinate -> !topographyService.isInsideBuilding(coordinate) }
                    .collect(Collectors.toList())
            )
            runningLeft = topographyService.destinationPoint(
                runningLeft.latitude,
                runningLeft.longitude,
                pointResolutionMeters,
                leftBearing
            )
            runningRight = topographyService.destinationPoint(
                runningRight.latitude,
                runningRight.longitude,
                pointResolutionMeters,
                rightBearing
            )
        } while (runningLeft.latitude < topLeft.latitude && runningRight.latitude < topRight.latitude)

        return ans
    }

    private fun calculateVisibleProjections(
        center: Coordinate,
        rangeMeters: Double,
        pointResolutionMeters: Double
    ): List<Coordinate> {
        val visibleProjections: ArrayList<Coordinate> = ArrayList()
        for (bearing in 0..359 step 2) {
            val outer: Coordinate = topographyService.destinationPoint(
                center.latitude,
                center.longitude,
                rangeMeters,
                bearing.toDouble()
            )
            val projection =
                topographyService.calcRoutePointsLinear(center, outer, false, pointResolutionMeters)
            visibleProjections.addAll(topographyService.getVisiblePoints(center, projection))
        }
        return visibleProjections
    }
}