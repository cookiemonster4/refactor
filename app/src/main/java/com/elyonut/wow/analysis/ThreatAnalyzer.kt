package com.elyonut.wow.analysis

import android.content.Context
import com.elyonut.wow.SingletonHolder
import com.elyonut.wow.VectorLayersManager
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.model.*
import com.elyonut.wow.parser.MapboxParser
import com.elyonut.wow.utilities.TempDB
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.turf.TurfMeasurement
import java.util.stream.Collectors

class ThreatAnalyzer private constructor(
    context: Context
) {
    private var topographyService = TopographyService
    private val vectorLayersManager = VectorLayersManager.getInstance(context)
    var threatLayer: List<FeatureModel> = TempDB.getInstance(context).getThreatLayer()

    private val logger: ILogger = TimberLogAdapter()

    companion object : SingletonHolder<ThreatAnalyzer, Context>(::ThreatAnalyzer)

    // when in Los it means the building is a threat?
    // can we take it somewhere else so we won't need the map here?
    fun getBuildingsWithinLOS(
        currentLocation: LatLng,
        buildingAtLocation: Feature?
    ): List<Feature> {
        val buildings = vectorLayersManager.getLayerById(Constants.BUILDINGS_LAYER_ID)?.map {
            MapboxParser.parseToMapboxFeature(it)
        }

        val currentLocationCoord = Coordinate(currentLocation.latitude, currentLocation.longitude)
        return buildings?.filter {
            topographyService.isThreatBuilding(
                currentLocationCoord,
                it,
                buildingAtLocation
            )
        } ?: arrayListOf()
    }

    // returns a threatList?
    fun getThreatFeaturesConstruction(
        currentLocation: LatLng,
        featureModels: List<FeatureModel>
    ): List<FeatureModel> {
        return filterWithLOSModelFeatures(featureModels, currentLocation)
    }

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
        featureModels: List<FeatureModel>,
        pointResolutionMeters: Double,
        heightMeters: Double
    ) {
        featureModels.forEach { featureModel ->
            val threatType = featureModel.properties?.get("type")?.asString
            if (threatType != null && !threatType.contains("mikush")) {

                val threatRangeMeters = KnowledgeBase.getRangeMeters(threatType)
                val buildingsAtZone = topographyService.getBuildingsAtZone(featureModel)

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

    fun filterWithLOSModelFeatures(
        buildingFeatureCollection: List<FeatureModel>,
        currentLocation: LatLng
    ): List<FeatureModel> {
        val currentLocationCoord = Coordinate(currentLocation.latitude, currentLocation.longitude)
        return buildingFeatureCollection.parallelStream().filter { featureModel ->
            topographyService.isThreat(
                currentLocationCoord,
                featureModel
            )
        }.collect(
            Collectors.toList()
        )
    }

    private fun filterWithLOSCoordinates(
        coverageSquare: List<Coordinate>,
        currentLocation: LatLng
    ): List<Coordinate> {
        val currentLocationCoord =
            Coordinate(currentLocation.latitude, currentLocation.longitude)
        val currentLocationExploded =
            topographyService.explodeLocationCoordinate(currentLocationCoord)

        return coverageSquare.filter { coord ->
            topographyService.isLOS(
                coord,
                currentLocationExploded
            )
        }
    }

    fun filterWithLOSCoordinatesAlpha(
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
        if (featureIdAtLocation != null && fromCache) { // we store only features in cache
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

            if (featureIdAtLocation != null && fromCache) { // we store only features in cache
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
    fun featureModelToThreat(featureModel: FeatureModel, currentLocation: LatLng, isLos: Boolean): Threat {
        val threat = Threat(featureModel)
        enrichThreat(threat, currentLocation, isLos)
        return threat
    }

    private fun enrichThreat(threat: Threat, currentLocation: LatLng, isLOS: Boolean) {
        threat.azimuth = bearingToAzimuth(
            TurfMeasurement.bearing( // how to get without mapbox
                Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
                Point.fromLngLat(threat.longitude, threat.latitude)
            )
        )

        threat.distanceMeters =
            currentLocation.distanceTo(LatLng(threat.latitude, threat.longitude))
        threat.isLos = isLOS
    }

    private fun bearingToAzimuth(bearing: Double): Double {
        var angle = bearing % 360;
        if (angle < 0) {
            angle += 360; }
        return angle
    }

    private fun getThreatLevel(height: Double): ThreatLevel = when {
        height < 3 -> ThreatLevel.None
        height < 10 -> ThreatLevel.Low
        height < 100 -> ThreatLevel.Medium
        else -> ThreatLevel.High
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
            /*val bestAlpha = topographyService.getProjectionBestAlpha(center, projection)
            visibleProjections.addAll(projection.filter { current -> topographyService.isPointVisible(center, current, bestAlpha) })*/
        }
        return visibleProjections
    }


}