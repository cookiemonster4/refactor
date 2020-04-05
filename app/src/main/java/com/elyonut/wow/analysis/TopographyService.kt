package com.elyonut.wow.analysis

import android.util.ArrayMap
import com.elyonut.wow.App
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.analysis.quadtree.Envelope
import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.PolygonModel
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.io.InputStream
import kotlin.math.*


class TopographyService {

    private var mapboxMap: MapboxMap
    private val LOS_HEIGHT_METERS = 1.5
    private val vectorIndex: VectorEnvelopeIndex = VectorEnvelopeIndex()

    var explodedMap: ArrayMap<String, List<Coordinate>> = ArrayMap()

    constructor(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        val stream: InputStream = App.resourses.assets.open("tlv-buildings.geojson")
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        val jsonObj = String(buffer, charset("UTF-8"))

        this.vectorIndex.loadBuildingFromGeoJson(jsonObj)
    }

    fun isThreatBuilding(currentLocation: Coordinate, feature: Feature): Boolean {
        val threatCoordinates = getGeometryCoordinates(feature.geometry()!!)
        val threatType = feature.getProperty("type")?.asString
        if (threatType != null && threatType.contains("mikush")) {
            return isMikushInRange(currentLocation, threatCoordinates)
        }

        val threatRangeMeters = KnowledgeBase.getRangeMeters(threatType)

        var inRange = false
        for (coord in threatCoordinates) {
            val distance = distanceMeters(currentLocation, coord)
            if (distance <= threatRangeMeters) {
                inRange = true
                break
            }
        }

        if (inRange) {
            val threatHeight = feature.getNumberProperty("height").toDouble()
            return isLOS(currentLocation, threatCoordinates, threatHeight)
        }


        return false
    }

    fun isLOS(
        currentLocation: Coordinate,
        threatCoordinates: List<Coordinate>,
        threatHeight: Double
    ): Boolean {

        var locationCoordinates = listOf(currentLocation)

        val buildingAtLocation =
            getBuildingAtLocation(LatLng(currentLocation.latitude, currentLocation.longitude))
        if (buildingAtLocation != null) {
            locationCoordinates = getCoordinatesForAnalysis(buildingAtLocation.geometry()!!)
            val locationHeight = buildingAtLocation.getNumberProperty("height").toDouble()
            locationCoordinates.forEach { bc -> bc.heightMeters = locationHeight }
        }

        val buildingCoordinates = explodeCornerCoordinates(threatCoordinates)
        buildingCoordinates.forEach { bc -> bc.heightMeters = threatHeight }

        return isLOS(locationCoordinates, buildingCoordinates)

    }

    fun isThreat(currentLocation: Coordinate, featureModel: FeatureModel): Boolean {

        val threatType = featureModel.properties?.get("type")?.asString
        if (threatType != null && threatType.contains("mikush")) {
            val coordinates = getCoordinates(featureModel.geometry)
            return isMikushInRange(currentLocation, coordinates)
        }

        val threatRangeMeters = KnowledgeBase.getRangeMeters(threatType)

        val threatCoordinates = getCoordinates(featureModel.geometry)

        var inRange = false
        for (coord in threatCoordinates) {
            val distance = distanceMeters(currentLocation, coord)
            if (distance <= threatRangeMeters) {
                inRange = true
                break
            }
        }

        if (inRange) {
            val threatHeight = featureModel.properties?.get("height")!!.asDouble
            val threatCoordinatesExploded =
                getExplodedFromCache(featureModel.id!!, threatCoordinates, threatHeight)

            return isLOSLocalIndex(currentLocation, threatCoordinatesExploded)
        }

        return false
    }

    fun explodeLocationCoordinate(currentLocation: Coordinate): List<Coordinate> {
        var locationCoordinates = listOf(currentLocation)

        val buildingAtLocation =
            vectorIndex.getVectorQuad(currentLocation.longitude, currentLocation.latitude)
        if (buildingAtLocation != null) {
            // locationCoordinates = getCoordinatesForAnalysis(buildingAtLocation.polygon)
            locationCoordinates = getGeometryCoordinates(buildingAtLocation.polygon)
            val locationHeight = buildingAtLocation.properties["height"]!!.toDouble()
            locationCoordinates.forEach { bc -> bc.heightMeters = locationHeight }
        }
        return locationCoordinates
    }

    fun explodeFeatureCoordinate(featureModel: FeatureModel): List<Coordinate> {

        val featureCoords = getCoordinates(featureModel.geometry)
        val threatHeight = featureModel.properties?.get("height")!!.asDouble
        return getExplodedFromCache(featureModel.id!!, featureCoords, threatHeight)
    }

    fun getBuildingsAtZone(featureModel: FeatureModel): List<VectorEnvelope>{
        val featureCoords = getCoordinates(featureModel.geometry)

        val multipleVectors =
            vectorIndex.getMultipleVectors(getEnvelope(featureCoords), getPolygon(featureCoords))

        return if (multipleVectors != null && multipleVectors.size > 0) {
            multipleVectors
        } else {
            ArrayList()
        }
    }

    fun featureIdAtLocation(longitude: Double, latitude: Double): String? {
        val buildingAtLocation = vectorIndex.getVectorQuad(longitude, latitude)
        if (buildingAtLocation != null) {
            return buildingAtLocation.properties["id"]
        }
        return null
    }

    fun isInsideBuilding(location: Coordinate): Boolean {
        val buildingAtLocation = vectorIndex.getVectorQuad(location.longitude, location.latitude)
        if (buildingAtLocation != null) {
            return true
        }
        return false
    }


    private fun getExplodedFromCache(
        featureId: String,
        threatCoordinates: List<Coordinate>,
        threatHeight: Double
    ): List<Coordinate> {
        //if buildings inside the threat zone: return the building coords
        // else return the threat zone corners
        var explodedThreatCoordinates: List<Coordinate>? = explodedMap[featureId]
        if (explodedThreatCoordinates == null) {
            explodedThreatCoordinates = getCoordinatesFromZone(threatCoordinates, threatHeight)
            explodedMap[featureId] = explodedThreatCoordinates
        }
        return explodedThreatCoordinates
    }

    private fun isMikushInRange(
        currentLocation: Coordinate,
        coordinates: List<Coordinate>
    ): Boolean {

        for (coord in coordinates) {
            val distance = distanceMeters(currentLocation, coord)
            if (distance <= KnowledgeBase.MIKUSH_RANGE_METERS)
                return true
        }

        return false
    }

    private fun isLOSLocalIndex(
        currentLocation: Coordinate,
        threatCoordinatesExploded: List<Coordinate>
    ): Boolean {
        var locationCoordinates = listOf(currentLocation)
        val buildingAtLocation =
            vectorIndex.getVectorQuad(currentLocation.longitude, currentLocation.latitude)
        if (buildingAtLocation != null) {
            locationCoordinates = getCoordinatesForAnalysis(buildingAtLocation.polygon)
            val locationHeight = buildingAtLocation.properties["height"]!!.toDouble()
            locationCoordinates.forEach { bc -> bc.heightMeters = locationHeight }
        }

        return isLOS(locationCoordinates, threatCoordinatesExploded)

    }

    private fun isLOS(coords1: List<Coordinate>, coords2: List<Coordinate>): Boolean {
        for (c1 in coords1) {
            val coordinatesDistances = ArrayList<CoordinateDistance>()
            for (c2 in coords2) {
                val distance = distanceMeters(c1, c2)
                coordinatesDistances.add(CoordinateDistance(c1, c2, distance))
            }
            coordinatesDistances.sort()

            for (cd in coordinatesDistances) {
                if (isLOS(cd.c1, cd.c2)) {
                    return true
                }
            }
        }
        return false
    }

    fun isLOS(from: Coordinate, coords2: List<Coordinate>): Boolean {
        val coordinatesDistances = ArrayList<CoordinateDistance>()
        for (c2 in coords2) {
            val distance = distanceMeters(from, c2)
            coordinatesDistances.add(CoordinateDistance(from, c2, distance))
        }
        coordinatesDistances.sort()
        for (cd in coordinatesDistances) {
            if (isLOS(cd.c1, cd.c2)) {
                return true
            }
        }
        return false
    }

    fun isLOS(from: Coordinate, to: Coordinate): Boolean {

        val min: Coordinate
        val max: Coordinate
        from.heightMeters = getHeight(from)
        // if located inside building, than reduce the height
        if (from.heightMeters > 0 && from.heightMeters != LOS_HEIGHT_METERS) {
            from.heightMeters -= 0.5
        }
        to.heightMeters = getHeight(to)
        if (from.heightMeters > to.heightMeters) {
            max = from
            min = to
        } else {
            max = to
            min = from
        }

        val routePoints = calcRoutePointsLinear(from, to, false, 1.0)


        val currDistance = distanceMeters(from, to)
        val currTan = abs(max.heightMeters - min.heightMeters) / currDistance
        for (i in 0 until routePoints.size) {
            val candidate = routePoints[i]
            val canDistance = distanceMeters(min, candidate)
            candidate.heightMeters = getHeight(candidate)
            val maxHeight = canDistance * currTan
            val canHeight = candidate.heightMeters - min.heightMeters
            //   console.log(canHeight + "," + maxHeight);
            if (canHeight > maxHeight) {
                return false
            }
        }

        return true
    }

    fun calcRoutePointsLinear(
        fromPoint: Coordinate,
        toPoint: Coordinate,
        skipFirst: Boolean,
        distanceOfPoints: Double
    ): ArrayList<Coordinate> {
        val aCoordiates = ArrayList<Coordinate>()

        // The distance between the latitudes
        val latDis = toPoint.latitude - fromPoint.latitude
        // The distance between the longitudes
        val longDis = toPoint.longitude - fromPoint.longitude
        val samples = ceil(distanceMeters(fromPoint, toPoint) / distanceOfPoints).toInt()
        // get the minimum between the samples and Max samples
        //samples = Math.min(samples,256);
        val deltaLat = latDis / samples
        val deltaLong = longDis / samples
        // start from the first point
        var currentLat = fromPoint.latitude
        var currentLong = fromPoint.longitude
        var i = 0
        while (i < samples) {
            if (!skipFirst || i > 0) {
                val aPoint = Coordinate(currentLat, currentLong)
                aCoordiates.add(aPoint)
            }
            currentLat += deltaLat
            currentLong += deltaLong
            i++
        }
        // add the last point
        aCoordiates.add(toPoint)

        return aCoordiates
    }

    fun getVisiblePoints(
        origin: Coordinate,
        projection: List<Coordinate>
    ): List<Coordinate> {
        val visiblePoints: ArrayList<Coordinate> = ArrayList()
        var currentAlpha: Double
        var current: Coordinate
        var bestAlpha = -10000.0
        val aSize: Int = projection.size
        for (i in 1 until aSize) {
            current = projection[i]
            currentAlpha = angle(origin, current, LOS_HEIGHT_METERS, LOS_HEIGHT_METERS)
            if (currentAlpha > bestAlpha) {
                bestAlpha = angle(origin, current, LOS_HEIGHT_METERS, 0.0)
                visiblePoints.add(current)
            }
        }
        return visiblePoints
    }

    fun isPointVisible(origin: Coordinate, current: Coordinate, bestAlpha: Double): Boolean {
        val currentAlpha = angle(origin, current, LOS_HEIGHT_METERS, LOS_HEIGHT_METERS)
        if (currentAlpha > bestAlpha) {
            return true
        }
        return false
    }

    fun getProjectionBestAlpha(origin: Coordinate, projection: List<Coordinate>): Double {
        var bestAlpha = -10000.0
        projection.forEach { current -> bestAlpha = getPointBestAlpha(origin, current, bestAlpha) }

        return bestAlpha
    }

    private fun getPointBestAlpha(
        origin: Coordinate,
        current: Coordinate,
        bestAlpha: Double
    ): Double {
        val currentAlpha = angle(origin, current, LOS_HEIGHT_METERS, LOS_HEIGHT_METERS)
        var newBestAlpha = bestAlpha
        if (currentAlpha > bestAlpha) {
            newBestAlpha = angle(origin, current, LOS_HEIGHT_METERS, 0.0)
        }
        return newBestAlpha
    }

    private fun angle(
        origin: Coordinate,
        aPoint: Coordinate,
        observerHeight: Double,
        targetHeight: Double
    ): Double {
        val aDistance: Double = distanceMeters(origin, aPoint)
        return atan2(
            getHeight(aPoint) + targetHeight - (getHeight(origin) + observerHeight),
            aDistance
        )
    }


    private fun getHeight(c1: Coordinate): Double {
        if (c1.heightMeters == -10000.0) {
            return vectorIndex.getHeight(c1.longitude, c1.latitude)
        }
        return c1.heightMeters
    }

    private fun getHeightMapBox(c1: Coordinate): Double {
        if (c1.heightMeters == -10000.0) {
            val buildingAtLocation = getBuildingAtLocation(LatLng(c1.latitude, c1.longitude))
            return buildingAtLocation?.getNumberProperty("height")?.toDouble() ?: LOS_HEIGHT_METERS
        }
        return c1.heightMeters
    }

    private fun getBuildingAtLocation(
        location: LatLng
    ): Feature? {

        val point = mapboxMap.projection.toScreenLocation(location)
        val features = mapboxMap.queryRenderedFeatures(point, Constants.BUILDINGS_LAYER_ID) //????

        if (features.isNullOrEmpty())
            return null

        val sortedByName =
            features.sortedBy { myObject -> myObject.getNumberProperty("height").toDouble() }
        return sortedByName.last()
    }

    private fun getCoordinatesForAnalysis(featureGeometry: Geometry): List<Coordinate> {
        val corners = getGeometryCoordinates(featureGeometry)
        return explodeCornerCoordinates(corners)
    }


    private fun explodeCornerCoordinates(originalCoordinates: List<Coordinate>): List<Coordinate> {
        val coordinates: ArrayList<Coordinate> = ArrayList()
        for ((index, value) in originalCoordinates.withIndex()) {
            coordinates.add(value)
            if (index < originalCoordinates.size - 1) {
                val explodedCoordinates =
                    calcRoutePointsLinear(value, originalCoordinates[index + 1], true, 4.0)
                coordinates.addAll(explodedCoordinates)
            }
        }

        return coordinates
    }

    private fun getCoordinates(polygonModel: PolygonModel): List<Coordinate> {
        return polygonModel.coordinates[0].map { coords ->
            Coordinate(coords[1], coords[0])
        }
    }

    /*
    getCoordinatesFromZone:
    if buildings inside the threat zone: return the internal building coords
      else return the threat zone corners exploded
     */
    private fun getCoordinatesFromZone(
        polygon: List<Coordinate>,
        threatHeight: Double
    ): List<Coordinate> {

        val multipleVectors =
            vectorIndex.getMultipleVectors(getEnvelope(polygon), getPolygon(polygon))

        if (multipleVectors != null && multipleVectors.size > 0) {
            val result = ArrayList<Coordinate>()
            for (vector in multipleVectors) {
                val corners = getCoordinates(vector.coordinates)
                val explodeCornerCoordinates = explodeCornerCoordinates(corners)
                explodeCornerCoordinates.forEach { bc ->
                    bc.heightMeters = vector.properties["height"]!!.toDouble()
                }
                result.addAll(explodeCornerCoordinates)
            }

            return result
        } else {
            val buildingCoordinates = explodeCornerCoordinates(polygon)
            buildingCoordinates.forEach { bc -> bc.heightMeters = threatHeight }
            return buildingCoordinates
        }
    }

    internal fun getCoordinates(points: List<Point>): List<Coordinate> {
        return points.map { point ->
            Coordinate(point.latitude(), point.longitude())
        }
    }

    private fun getPolygon(points: List<Coordinate>): Polygon {
        val pointList = points.map { coord ->
            Point.fromLngLat(coord.longitude, coord.latitude)
        }
        val list = java.util.ArrayList<List<Point>>()
        list.add(pointList)
        return Polygon.fromLngLats(list)
    }

    private fun getEnvelope(polygon: List<Coordinate>): Envelope {

        var minLongitude = java.lang.Double.MAX_VALUE
        var maxLongitude = java.lang.Double.MIN_VALUE
        var minLatitude = java.lang.Double.MAX_VALUE
        var maxLatitude = java.lang.Double.MIN_VALUE

        for (c in polygon) {
            if (c.longitude < minLongitude)
                minLongitude = c.longitude
            if (c.latitude < minLatitude)
                minLatitude = c.latitude
            if (c.longitude > maxLongitude)
                maxLongitude = c.longitude
            if (c.latitude > maxLatitude)
                maxLatitude = c.latitude
        }

        return Envelope(maxLongitude, minLongitude, maxLatitude, minLatitude)
    }


    fun getGeometryCoordinates(featureGeometry: Geometry): List<Coordinate> {
        val geometry: Geometry
        var coordinates: List<Coordinate> = ArrayList()
        when (featureGeometry.type()) {
            "Polygon" -> {
                geometry = featureGeometry as Polygon
                coordinates = geometry.coordinates()[0].map { point ->
                    Coordinate(
                        point.latitude(),
                        point.longitude()
                    )
                } as MutableList<Coordinate>
            }
            "Point" -> {
                geometry = featureGeometry as Point
                coordinates = ArrayList()
                coordinates.add(Coordinate(geometry.coordinates()[0], geometry.coordinates()[1]))
            }
            "MultiPolygon" -> {
                geometry = featureGeometry as MultiPolygon
                // TODO: returning only single geometry
                coordinates = geometry.coordinates()[0][0].map { point ->
                    Coordinate(
                        point.latitude(),
                        point.longitude()
                    )
                } as MutableList<Coordinate>
            }
            "LineString" -> {
                geometry = featureGeometry as LineString
                coordinates = geometry.coordinates().map { point ->
                    Coordinate(
                        point.latitude(),
                        point.longitude()
                    )
                } as MutableList<Coordinate>
            }
//            "MultiLineString" -> {
//                geometry = featureGeometry as MultiLineString
//                TODO("not implemented")
//            }
//            "MultiPoint" -> {
//                geometry = featureGeometry as MultiPoint
//                TODO("not implemented")
//            }
            else -> {
//                geometry = featureGeometry as CoordinateContainer<*>
            }
        }
        return coordinates
    }

    /**
     * This uses the ‘haversine’ formula to calculate the great-circle distance(km) between two points –
     * that is, the shortest distance over the earth’s surface
     *
     * @param from the source coordinate
     * @param to   the target coordinate
     * @return the aerial distance between two coordinates (Meters)
     */
    fun distanceMeters(from: Coordinate, to: Coordinate): Double {
        var startLat = from.latitude
        val startLong = from.longitude
        var endLat = to.latitude
        val endLong = to.longitude
        val dLat = Math.toRadians(endLat - startLat)
        val dLong = Math.toRadians(endLong - startLong)

        startLat = Math.toRadians(startLat)
        endLat = Math.toRadians(endLat)

        val a = haversin(dLat) + cos(startLat) * cos(endLat) * haversin(dLong)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return Constants.EARTH_RADIUS_KM * c * 1000.0
    }

    private fun haversin(value: Double): Double {
        return sin(value / 2.0).pow(2.0)
    }

    /**
     * Returns the destination point from ‘this’ point having travelled the given distance on the
     * given initial bearing (bearing normally varies around path followed).
     *
     * @param   {number} distance - Distance travelled, in same units as earth radius (default: metres).
     * @param   {number} bearing - Initial bearing in degrees from north.
     * @returns {LatLon} Destination point.
     *
     * @example
     * var p1 = new LatLon(51.4778, -0.0015);
     * var p2 = p1.destinationPoint(7794, 300.7); // 51.5135°N, 000.0983°W
     */
    fun destinationPoint(
        fromLat: Double,
        fromLon: Double,
        distance: Double,
        bearing: Double
    ): Coordinate {
        val radius = Constants.EARTH_RADIUS_KM * 1000

        // sinφ2 = sinφ1⋅cosδ + cosφ1⋅sinδ⋅cosθ
        // tanΔλ = sinθ⋅sinδ⋅cosφ1 / cosδ−sinφ1⋅sinφ2
        // see mathforum.org/library/drmath/view/52049.html for derivation

        val delta = distance / radius // angular distance in radians
        val theta = Math.toRadians(bearing)

        val phi1 = Math.toRadians(fromLat)
        val lambda1 = Math.toRadians(fromLon)

        val sinPhi1 = sin(phi1)
        val cosPhi1 = cos(phi1)
        val sinDelta = sin(delta)
        val cosDelta = cos(delta)
        val sinTheta = sin(theta)
        val cosTheta = cos(theta)

        val sinPhi2 = sinPhi1 * cosDelta + cosPhi1 * sinDelta * cosTheta
        val phi2 = asin(sinPhi2)
        val y = sinTheta * sinDelta * cosPhi1
        val x = cosDelta - sinPhi1 * sinPhi2
        val lambda2 = lambda1 + atan2(y, x)

        return Coordinate(
            Math.toDegrees(phi2),
            (Math.toDegrees(lambda2) + 540) % 360 - 180
        ) // normalise to −180..+180°
    }

    /**
     * Returns the (initial) bearing from source point to destination point.
     *
     * @param   from point - Latitude/longitude of source point.
     * @param   to point - Latitude/longitude of destination point.
     * @return {number} Initial bearing in degrees from north.
     *
     * @apiNote
     * var p1 = new Coordinate(52.205, 0.119);
     * var p2 = new Coordinate(48.857, 2.351);
     * var b1 = bearing(p1,p2); // 156.2°
     */
    fun bearing(from: Coordinate, to: Coordinate): Double {
        // tanθ = sinΔλ⋅cosφ2 / cosφ1⋅sinφ2 − sinφ1⋅cosφ2⋅cosΔλ
        // see mathforum.org/library/drmath/view/55417.html for derivation

        val phi1 = Math.toRadians(from.latitude)
        val phi2 = Math.toRadians(to.latitude)
        val deltaLambda = Math.toRadians(to.longitude - from.longitude)
        val y = sin(deltaLambda) * cos(phi2)
        val x =
            cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)

        return (Math.toDegrees(theta) + 360) % 360
    }

}