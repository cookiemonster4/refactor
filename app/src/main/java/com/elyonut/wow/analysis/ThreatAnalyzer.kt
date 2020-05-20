package com.elyonut.wow.analysis

import android.content.Context
import android.graphics.RectF
import android.os.Build
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.model.*
import com.elyonut.wow.parser.MapboxParser
import com.elyonut.wow.utilities.TempDB
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.turf.TurfMeasurement
import java.util.stream.Collectors

class ThreatAnalyzer(
    var context: Context,
    var mapboxMap: MapboxMap,
    private var topographyService: TopographyService
) {
    private val tempDB = TempDB.getInstance(context)
    var layers: List<LayerModel>
    var threatLayer: List<FeatureModel>

    init {
        layers = tempDB.getLayers()
        threatLayer = tempDB.getThreatLayer()
    }

    private val logger: ILogger = TimberLogAdapter()

    // when in Los it means the building is a threat?
    // can we take it somewhere else so we won't need the map here?
    fun getThreatFeaturesBuildings(currentLocation: LatLng, boundingBox: RectF): List<Feature> {
        val features = mutableListOf<Feature>()
        layers.find { it.id == Constants.BUILDINGS_LAYER_ID }?.features?.forEach {
            features.add(
                MapboxParser.parseToMapboxFeature(it)
            )
        }
        return filterWithLOS(features, currentLocation)
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
        rangeMeters: Double,
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

    private fun filterWithLOS(
        buildingFeatureCollection: List<Feature>,
        currentLocation: LatLng
    ): List<Feature> {
        val currentLocationCoord = Coordinate(currentLocation.latitude, currentLocation.longitude)
        return buildingFeatureCollection.filter {
            topographyService.isThreatBuilding(
                currentLocationCoord,
                it
            )
        }
    }

    private fun filterWithLOSModelFeatures(
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

/*        return coverageSquare.parallelStream().filter{coord -> topographyService.isLOS(coord, currentLocationExploded)}.collect(
            Collectors.toList())*/
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
            explodedCoordinates.forEach { bc -> bc.heightMeters = heightMeters }
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            visiblePoints.parallelStream()
                .filter { coordinate -> !topographyService.isInsideBuilding(coordinate) }
                .collect(Collectors.toList())
        } else {
            TODO("VERSION.SDK_INT < N")
        } // filter buildings
    }

    fun featureToThreat(
        feature: Feature,
        currentLocation: LatLng,
        isLos: Boolean
    ): Threat {
        val threat = Threat()
        val height = feature.getNumberProperty("height").toDouble()
        threat.level = getThreatLevel(height)

        val geometryCoordinates = topographyService.getGeometryCoordinates(feature.geometry()!!)
        val featureLatitude = geometryCoordinates[0].latitude
        val featureLongitude = geometryCoordinates[0].longitude
        val featureLocation = LatLng(featureLatitude, featureLongitude)
        val threatFeatureProperties = feature.properties()

        threat.feature = feature
        threat.location =
            GeoLocation(LocationType.Polygon, geometryCoordinates as ArrayList<Coordinate>)
        threat.distanceMeters = currentLocation.distanceTo(featureLocation)
        threat.azimuth = bearingToAzimuth(
            TurfMeasurement.bearing( // bearing - a person's way of standing or moving. בעברית- כוון או יחס
                Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
                Point.fromLngLat(featureLongitude, featureLatitude)
            )
        )
        threat.creator = "ישראל ישראלי"
        threat.description = "תיאור"
        threat.id = threatFeatureProperties?.get("id")?.asString ?: ""
        threat.name = threatFeatureProperties?.get("namestr")?.asString ?: ""
        threat.isLos = isLos
        threat.type = threatFeatureProperties?.get("type")?.asString ?: ""
        threat.height = height
        threat.latitude = threatFeatureProperties?.get("latitude")?.asDouble ?: 0.0
        threat.longitude = threatFeatureProperties?.get("longitude")?.asDouble ?: 0.0
        threat.eAmount = threatFeatureProperties?.get("eAmount")?.asString ?: ""
        threat.knowledgeType = threatFeatureProperties?.get("knowledgeType")?.asString ?: ""
        threat.range = threatFeatureProperties?.get("range")?.asDouble ?: 0.0

        return threat
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

    private fun getFeaturesFromMapbox(
        mapboxMap: MapboxMap,
        layerId: String,
        boundingBox: RectF
    ): List<Feature> {
        val features = mapboxMap.queryRenderedFeatures(boundingBox, layerId)
        return features

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