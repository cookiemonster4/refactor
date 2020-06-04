package com.elyonut.wow.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.view.Gravity
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.elyonut.wow.VectorLayersManager
import com.elyonut.wow.R
import com.elyonut.wow.adapter.LocationService
import com.elyonut.wow.adapter.PermissionsService
import com.elyonut.wow.adapter.TimberLogAdapter
import com.elyonut.wow.analysis.*
import com.elyonut.wow.interfaces.ILocationService
import com.elyonut.wow.interfaces.ILogger
import com.elyonut.wow.interfaces.IPermissions
import com.elyonut.wow.model.*
import com.elyonut.wow.parser.MapboxParser
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.utilities.Constants.Companion.LOCATION_CHECK_INTERVAL
import com.elyonut.wow.utilities.MapStates
import com.elyonut.wow.utilities.Maps
import com.elyonut.wow.utilities.NumericFilterTypes
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.toTypedArray

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val logger: ILogger = TimberLogAdapter()
    var selectLocationManual: Boolean = false
    var selectLocationManualConstruction: Boolean = false
    var selectLocationManualCoverage: Boolean = false
    var selectLocationManualCoverageAll: Boolean = false
    lateinit var map: MapboxMap
    private var locationService: ILocationService = LocationService.getInstance(getApplication())
    private val permissions: IPermissions = PermissionsService.getInstance(application)
    val vectorLayersManager = VectorLayersManager.getInstance(application)
    private var topographyService = TopographyService
    var threatAnalyzer = ThreatAnalyzer.getInstance(getApplication())
    var currentThreats = MutableLiveData<ArrayList<Threat>>()
    var mapLayers: LiveData<List<LayerModel>> =
        Transformations.map(vectorLayersManager.layers, ::layersUpdated)
    var selectedBuilding = MutableLiveData<FeatureModel>()
    private var _buildingsWithinLOS = MutableLiveData<List<Feature>>()
    val buildingsWithinLOS: LiveData<List<Feature>>
        get() = _buildingsWithinLOS
    var isAreaSelectionMode = false
    private var _areaOfInterest = MutableLiveData<Polygon>()
    val areaOfInterest: LiveData<Polygon>
        get() = _areaOfInterest
    var lineLayerPointList = ArrayList<Point>()
    private var currentLineLayerPointList = ArrayList<Point>()
    private var currentCircleLayerFeatureList = ArrayList<Feature>()
    private lateinit var circleSource: GeoJsonSource // areaOfInterest
    private lateinit var fillSource: GeoJsonSource // areaOfInterest
    private lateinit var firstPointOfPolygon: Point // areaOfInterest
    private var allCoverageTask: CalcThreatCoverageAllConstructionAsync? = null
    var threatAlerts = MutableLiveData<ArrayList<Threat>>()
    var isFocusedOnLocation = MutableLiveData<Boolean>()
    var shouldDisableAreaSelection = MutableLiveData<Boolean>()
    private var _calculateCoverage = MutableLiveData<LatLng>()
    val calculateCoverage: LiveData<LatLng>
        get() = _calculateCoverage
    private var _locationClickedIcon = MutableLiveData<LatLng>()
    val locationClickedIcon: LiveData<LatLng>
        get() = _locationClickedIcon
    private var _mapStateChanged = MutableLiveData<MapStates>()
    val mapStateChanged: LiveData<MapStates>
        get() = _mapStateChanged

    init {
        logger.initLogger()
    }

    fun areaOfInterestUpdated(polygon: Polygon) {
        _areaOfInterest.value = polygon
    }

    private fun layersUpdated(layers: List<LayerModel>) = layers

    fun updateCurrentThreats() {
        val threatLayer =
            mapLayers.value?.find { layerModel -> layerModel.id == Constants.ACTIVE_THREATS_LAYER_ID }?.features
                ?: arrayListOf()

        if (threatLayer.isNotEmpty()) {
            currentThreats.value =
                threatLayer as ArrayList<Threat> // Will be fixed when we delete current threats :)
            threatAlerts.value = currentThreats.value
        }
    }

    @SuppressLint("WrongConstant")
    fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        setMapStyle(Maps.MAPBOX_STYLE_URL) {
            viewModelScope.launch { startLocationService() }
        }

        setCameraMoveListener()
        map.uiSettings.compassGravity = Gravity.RIGHT
    }

    private suspend fun startLocationService() {
        while (!locationService.isGpsEnabled() || !permissions.isLocationPermitted()) {
            delay(LOCATION_CHECK_INTERVAL)
        }

        initMapLocationComponent()
        locationService.subscribeToLocationChanges {
            locationChanged(it)
        }
    }

    private fun initMapLocationComponent() {
        val myLocationComponentOptions = LocationComponentOptions.builder(getApplication())
            .trackingGesturesManagement(true)
            .accuracyColor(ContextCompat.getColor(getApplication(), R.color.myLocationColor))
            .build()

        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(getApplication(), map.style!!)
                .locationComponentOptions(myLocationComponentOptions).build()

        map.locationComponent.apply {
            activateLocationComponent(locationComponentActivationOptions)
            isLocationComponentEnabled = true
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }
    }

    private fun locationChanged(location: Location) {
        map.locationComponent.forceLocationUpdate(location)
    }

    fun setMapStyle(URL: String, callback: (() -> Unit)? = null) {
        map.setStyle(URL) { style ->
            addLayersToMapStyle(style)
            addThreatCoverageLayer(style)
            setSelectedBuildingLayer(style)
            setThreatLayerOpacity(style, Constants.REGULAR_OPACITY)
            circleSource = initCircleSource(style)
            fillSource = initLineSource(style)
            initCircleLayer(style)
            initLineLayer(style)
            callback?.invoke()
        }
    }

    // TODO make generic
    private fun setThreatLayerOpacity(loadedMapStyle: Style, opacity: Float) {
        val threatLayer = loadedMapStyle.getLayer(Constants.THREAT_LAYER_ID)
        (threatLayer as FillExtrusionLayer).withProperties(
            fillExtrusionOpacity(
                opacity
            )
        )
    }

    // TODO make generic
    private fun setSelectedBuildingLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(Constants.SELECTED_BUILDING_SOURCE_ID))
        loadedMapStyle.addLayer(
            FillExtrusionLayer(
                Constants.SELECTED_BUILDING_LAYER_ID,
                Constants.SELECTED_BUILDING_SOURCE_ID
            ).withProperties(
                fillExtrusionOpacity(Constants.HIGH_OPACITY),
                fillExtrusionColor(Color.parseColor("#870485")),
                fillExtrusionHeight(get("height"))
            )
        )
    }

    // TODO Make generic
    private fun addThreatCoverageLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(
            GeoJsonSource(
                Constants.THREAT_COVERAGE_SOURCE_ID
            )
        )

        val circleLayer = CircleLayer(
            Constants.THREAT_COVERAGE_LAYER_ID,
            Constants.THREAT_COVERAGE_SOURCE_ID
        )
        circleLayer.setProperties(
            circleRadius(4f),
            circleColor(Color.RED),
            circleBlur(1f),
            visibility(NONE)
        )

        loadedMapStyle.addLayer(circleLayer)
    }

    // TODO maybe rename
    fun layerSelected(layerId: String) {
        toggleLayerVisibility(layerId)
    }

    private fun toggleLayerVisibility(layerId: String) {
        val layer = map.style?.getLayer(layerId)

        if (layer != null) {
            if (layer.visibility.getValue() == VISIBLE) {
                layer.setProperties(visibility(NONE))
            } else {
                layer.setProperties(visibility(VISIBLE))
            }
        }
    }

    fun setLayerVisibility(layerId: String, visibility: PropertyValue<String>) {
        val layer = map.style?.getLayer(layerId)
        layer?.setProperties(visibility)
    }

    // TODO remove filter
    fun applyFilter(
        loadedStyle: Style,
        layerId: String,
        propertyId: String,
        isStringType: Boolean,
        numericType: NumericFilterTypes,
        stringValue: String,
        specificValue: Number,
        minValue: Number,
        maxValue: Number
    ) {
        if (isStringType) {
            FilterHandler.filterLayerByStringProperty(loadedStyle, layerId, propertyId, stringValue)
        } else {
            when (numericType) {
                NumericFilterTypes.RANGE -> {
                    FilterHandler.filterLayerByNumericRange(
                        loadedStyle,
                        layerId,
                        propertyId,
                        minValue,
                        maxValue
                    )
                }
                NumericFilterTypes.LOWER -> {
                    FilterHandler.filterLayerByMaxValue(loadedStyle, layerId, propertyId, maxValue)
                }
                NumericFilterTypes.GREATER -> {
                    FilterHandler.filterLayerByMinValue(loadedStyle, layerId, propertyId, minValue)
                }
                NumericFilterTypes.SPECIFIC -> {
                    FilterHandler.filterLayerBySpecificNumericProperty(
                        loadedStyle,
                        layerId,
                        propertyId,
                        specificValue
                    )
                }
            }
        }
    }

    // TODO remove filter
    fun removeFilter(style: Style, layerId: String) {
        FilterHandler.removeFilter(style, layerId)
    }

    fun filterLayerByType(newFilter: Pair<String, Boolean>) {
        val layer = map.style!!.getLayer(Constants.THREAT_LAYER_ID)
        addFilterToLayer(newFilter, layer!!)
    }

    // Beginning area of interest
// TODO maybe rename things (including function)
// TODO rewrite generic drawing
    private fun drawPolygonMode(latLng: LatLng) {
        val mapTargetPoint = Point.fromLngLat(latLng.longitude, latLng.latitude)

        if (currentCircleLayerFeatureList.isEmpty()) {
            firstPointOfPolygon = mapTargetPoint
        }

        currentCircleLayerFeatureList.add(Feature.fromGeometry(mapTargetPoint))
        circleSource.setGeoJson(FeatureCollection.fromFeatures(currentCircleLayerFeatureList))

        when {
            currentCircleLayerFeatureList.size < 3 -> currentLineLayerPointList.add(mapTargetPoint)
            currentCircleLayerFeatureList.size == 3 -> {
                currentLineLayerPointList.add(mapTargetPoint)
                currentLineLayerPointList.add(firstPointOfPolygon)
            }
            else -> {
                currentLineLayerPointList.removeAt(currentCircleLayerFeatureList.size - 1)
                currentLineLayerPointList.add(mapTargetPoint)
                currentLineLayerPointList.add(firstPointOfPolygon)
            }
        }

        fillSource.setGeoJson(makePolygonFeatureCollection(currentLineLayerPointList))
    }

    fun enableAreaSelection() {
        shouldDisableAreaSelection.value = false
        removeAreaFromMap()
    }

    private fun removeAreaFromMap() {
        currentCircleLayerFeatureList = ArrayList()
        currentLineLayerPointList = ArrayList()
        circleSource.setGeoJson(FeatureCollection.fromFeatures(currentCircleLayerFeatureList))
        fillSource.setGeoJson(makeLineFeatureCollection(currentLineLayerPointList))
    }

    fun undo() {
        if (currentCircleLayerFeatureList.isNotEmpty()) {
            when {
                currentCircleLayerFeatureList.size < 3 -> {
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                }
                currentCircleLayerFeatureList.size == 3 -> {
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                }
                else -> {
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                    currentLineLayerPointList.removeAt(currentLineLayerPointList.size - 1)
                    currentLineLayerPointList.add(currentLineLayerPointList[0])
                }
            }

            currentCircleLayerFeatureList.removeAt(currentCircleLayerFeatureList.size - 1)
            circleSource.setGeoJson(FeatureCollection.fromFeatures(currentCircleLayerFeatureList))
            fillSource.setGeoJson(makeLineFeatureCollection(currentLineLayerPointList))
        }
    }

    fun applyAreaClicked() {
        circleSource.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))
        lineLayerPointList = currentLineLayerPointList

        if (lineLayerPointList.isEmpty()) {
            _areaOfInterest.postValue(null)
        } else {
            _areaOfInterest.postValue(Polygon.fromLngLats(listOf(lineLayerPointList)))
        }

        shouldDisableAreaSelection.value = true
        _mapStateChanged.value = MapStates.REGULAR
    }

    fun cancelAreaClicked() {
        currentCircleLayerFeatureList = ArrayList()
        currentLineLayerPointList = ArrayList()
        circleSource.setGeoJson(FeatureCollection.fromFeatures(ArrayList()))
        fillSource.setGeoJson(makePolygonFeatureCollection(lineLayerPointList))
        shouldDisableAreaSelection.value = true
        _mapStateChanged.value = MapStates.REGULAR
    }

    private fun makeLineFeatureCollection(pointArrayList: ArrayList<Point>): FeatureCollection {
        return FeatureCollection.fromFeatures(
            arrayOf(
                Feature.fromGeometry(
                    LineString.fromLngLats(
                        pointArrayList
                    )
                )
            )
        )
    }

    private fun makePolygonFeatureCollection(pointArrayList: ArrayList<Point>): FeatureCollection {
        return FeatureCollection.fromFeatures(
            arrayOf(
                Feature.fromGeometry(
                    Polygon.fromLngLats(
                        listOf(pointArrayList)
                    )
                )
            )
        )
    }

    private fun initCircleLayer(loadedMapStyle: Style) {
        val circleLayer = CircleLayer(
            Constants.CIRCLE_LAYER_ID,
            Constants.CIRCLE_SOURCE_ID
        )
        circleLayer.setProperties(
            circleRadius(7f),
            circleColor(Color.WHITE)
        )

        loadedMapStyle.addLayer(circleLayer)
    }

    private fun initCircleSource(loadedMapStyle: Style): GeoJsonSource {
        val circleFeatureCollection = FeatureCollection.fromFeatures(ArrayList())
        val circleGeoJsonSource = GeoJsonSource(Constants.CIRCLE_SOURCE_ID, circleFeatureCollection)
        loadedMapStyle.addSource(circleGeoJsonSource)

        return circleGeoJsonSource
    }

    private fun initLineSource(loadedMapStyle: Style): GeoJsonSource {
        val lineFeatureCollection = makePolygonFeatureCollection(lineLayerPointList)
        val lineGeoJsonSource = GeoJsonSource(Constants.LINE_SOURCE_ID, lineFeatureCollection)
        loadedMapStyle.addSource(lineGeoJsonSource)

        return lineGeoJsonSource
    }

    private fun initLineLayer(loadedMapStyle: Style) {
        val lineLayer = LineLayer(
            Constants.LINE_LAYER_ID,
            Constants.LINE_SOURCE_ID
        )

        lineLayer.setProperties(
            lineColor(Color.parseColor("#494949")),
            lineWidth(2.5f)
        )

        loadedMapStyle.addLayerBelow(lineLayer, Constants.CIRCLE_LAYER_ID)
    }
// End of area of interest

    // TODO check if it follows me and if not maybe make generic
    fun focusOnUserLocationClicked() {
        if (map.locationComponent.isLocationComponentActivated) {
            map.locationComponent.apply {
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }
        }
    }

    // Beginning of onMapClick by our beloved uniqAI
    private fun updateBuildingsWithinLOS(latLng: LatLng) {
        _buildingsWithinLOS.value = threatAnalyzer.getBuildingsWithinLOS(
            latLng,
            getBuildingAtLocation(latLng, Constants.BUILDINGS_LAYER_ID)
        ).map { MapboxParser.parseToMapboxFeature(it) }
    }

    // Should we delete this?
    fun calculateCoverageForAll(
        coverageResolutionMeters: Double,
        coverageSearchHeightMeters: Double,
        progressBar: ProgressBar
    ) {

        if (allCoverageTask != null && allCoverageTask!!.status != AsyncTask.Status.FINISHED) {
            return //Returning as the current task execution is not finished yet.
        }

        allCoverageTask = CalcThreatCoverageAllConstructionAsync(this, progressBar)
        allCoverageTask!!.execute(
            ThreatCoverageData(
                coverageResolutionMeters,
                coverageSearchHeightMeters
            )
        )
    }
// End of beloved uniqAI onMapClick

    // TODO Not finished!
    fun onMapClicked(currentMapState: MapStates, latLng: LatLng) {
        when (currentMapState) {
            MapStates.LOS_BUILDINGS_TO_LOCATION -> {
                _locationClickedIcon.value = latLng
                updateBuildingsWithinLOS(latLng)
                _mapStateChanged.value = MapStates.REGULAR
                selectLocationManual = false
            }
            MapStates.CALCULATE_COORDINATES_IN_RANGE -> {
                _locationClickedIcon.value = latLng
                _calculateCoverage.postValue(latLng)
            }
            MapStates.DRAWING -> {
                drawPolygonMode(latLng)
            }
            MapStates.REGULAR -> {
                val selectedBuildingSource =
                    map.style?.getSourceAs<GeoJsonSource>(Constants.SELECTED_BUILDING_SOURCE_ID)

                getBuildingAtLocation(
                    latLng,
                    Constants.THREAT_LAYER_ID
                )?.let {
                    selectedBuilding.value = MapboxParser.parseToFeatureModel(it)
                    selectedBuildingSource?.setGeoJson(FeatureCollection.fromFeatures(arrayListOf(it)))
                }
            }
        }
    }


    // TODO rename getThreatMetadata
    fun buildingThreatToCurrentLocation(building: Feature): Threat {
        val currentLocation = locationService.getCurrentLocation()
        val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)

        val threatCoordinates = topographyService.getGeometryCoordinates(building.geometry()!!)
        val threatHeight = building.getNumberProperty("height").toDouble()
        val feature = getBuildingAtLocation(
            LatLng(currentLocation.latitude, currentLocation.longitude),
            Constants.BUILDINGS_LAYER_ID // why this layer and not threat ???
        )
        val isLOS = topographyService.isLOS(
            feature,
            Coordinate(
                currentLatLng.latitude,
                currentLatLng.longitude
            ), threatCoordinates, threatHeight
        )

        return threatAnalyzer.featureToThreat(
            MapboxParser.parseToFeatureModel(building),
            currentLatLng,
            isLOS
        )
    }

    fun getBuildingAtLocation(
        location: LatLng,
        layerId: String
    ): Feature? {

        val point = map.projection.toScreenLocation(location)
        val features = map.queryRenderedFeatures(point, layerId) //????

        if (features.isNullOrEmpty())
            return null

        // How is it a list? why do we sort them and send the last one?
        val sortedByName =
            features.sortedBy { myObject -> myObject.getNumberProperty("height").toDouble() }
        return sortedByName.last()
    }
    // End of beloved uniqAI onMapClick

    // TODO restructure, part to alertManager. create function zoomOnGivenLocation
    fun zoomOnLocation(threatID: String) {
        zoomOnGivenLocation(vectorLayersManager.getFeatureLocation(threatID))
    }

    private fun zoomOnGivenLocation(location: LatLngModel) {
        val position = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(17.0)
            .build()

        map.locationComponent.cameraMode = CameraMode.NONE
        map.easeCamera(
            CameraUpdateFactory
                .newCameraPosition(position)
        )
    }

    // TODO move to threat/alerts
    fun getFeatureName(threatID: String): String {
        return vectorLayersManager.getFeatureName(threatID)
    }

    // TODO rename
    private fun setCameraMoveListener() {
        map.addOnCameraMoveListener {
            val cameraLocation =
                LatLng(map.cameraPosition.target.latitude, map.cameraPosition.target.longitude)
            val currentLocation = LatLng(
                locationService.getCurrentLocation().latitude,
                locationService.getCurrentLocation().longitude
            )

            isFocusedOnLocation.postValue(
                cameraLocation.distanceTo(currentLocation)
                        <= Constants.MAX_DISTANCE_TO_CURRENT_LOCATION
            )
        }
    }

    private fun addLayersToMapStyle(style: Style) {
        mapLayers.value?.forEach { layerModel ->
            val features = layerModel.features.map { featureModel ->
                MapboxParser.parseToMapboxFeature(featureModel)
            }

            val layerGeoJsonSource =
                GeoJsonSource(layerModel.id, FeatureCollection.fromFeatures(features))
            style.addSource(layerGeoJsonSource)

            val layer: Layer = when (layerModel.id) {
                Constants.THREAT_LAYER_ID ->
                    FillExtrusionLayer(layerModel.id, layerModel.id).withProperties(
                        fillExtrusionColor(Color.RED), fillExtrusionOpacity(0.5f),
                        fillExtrusionHeight(get("height"))
                    )
                Constants.BUILDINGS_LAYER_ID ->
                    FillExtrusionLayer(layerModel.id, layerModel.id).withProperties(
                        fillExtrusionColor(Color.LTGRAY), fillExtrusionOpacity(0.8f),
                        fillExtrusionHeight(get("height"))
                    )
                Constants.ACTIVE_THREATS_LAYER_ID ->
                    FillExtrusionLayer(
                        Constants.ACTIVE_THREATS_LAYER_ID,
                        Constants.ACTIVE_THREATS_LAYER_ID
                    ).withProperties(
                        fillExtrusionOpacity(Constants.HIGH_OPACITY),
                        fillExtrusionHeight(get("height"))
                    )

                else -> {
                    FillLayer(layerModel.id, layerModel.id)
                }
            }

            style.addLayerAt(layer, style.layers.size - 1)
        }
    }

    fun clean() {
        locationService.cleanLocationService()
    }

    fun filterLayerByAllTypes(shouldFilter: Boolean) {
        val types =
            vectorLayersManager.getValuesOfLayerProperty(Constants.THREAT_LAYER_ID, "type")
                ?.toTypedArray()
        val filters = types?.map { type -> Pair(type, shouldFilter) }
        val layer = map.style!!.getLayer(Constants.THREAT_LAYER_ID)

        filters?.forEach {
            addFilterToLayer(it, layer!!)
        }
    }

    private fun addFilterToLayer(filter: Pair<String, Boolean>, layer: Layer) {
        val typeToFilter = filter.first
        val isChecked = filter.second

        (layer as FillExtrusionLayer).setFilter(
            if (isChecked) {
                any(
                    layer.filter,
                    all(eq(get("type"), typeToFilter))
                )
            } else {
                if (layer.filter != null) {
                    all(
                        layer.filter,
                        all(neq(get("type"), typeToFilter))
                    )
                } else {
                    all(
                        all(neq(get("type"), typeToFilter))
                    )
                }
            }
        )
    }
}

class FilterHandler {
    companion object {
        fun filterLayerByStringProperty(
            style: Style,
            layerId: String,
            propertyId: String,
            type: String
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(eq(get(propertyId), type))
            )
        }

        fun filterLayerBySpecificNumericProperty(
            style: Style,
            layerId: String,
            propertyId: String,
            value: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                (eq(
                    get(propertyId),
                    value
                ))
            )
        }

        fun filterLayerByNumericRange(
            style: Style,
            layerId: String,
            propertyId: String,
            minValue: Number,
            maxValue: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(
                    gte(
                        get(propertyId),
                        minValue
                    ), lte(get(propertyId), maxValue)
                )
            )
        }

        fun filterLayerByMinValue(
            style: Style,
            layerId: String,
            propertyId: String,
            minValue: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(
                    gte(
                        get(propertyId),
                        minValue
                    )
                )
            )
        }

        fun filterLayerByMaxValue(
            style: Style,
            layerId: String,
            propertyId: String,
            maxValue: Number
        ) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(
                all(
                    lte(get(propertyId), maxValue)
                )
            )
        }

        fun removeFilter(style: Style, layerId: String) {
            val layer = style.getLayer(layerId)
            (layer as FillExtrusionLayer).setFilter(literal(true))
        }
    }
}
