package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.elyonut.wow.VectorLayersManager
import com.elyonut.wow.R
import com.elyonut.wow.adapter.LocationService
import com.elyonut.wow.adapter.PermissionsService
import com.elyonut.wow.analysis.ThreatAnalyzer
import com.elyonut.wow.interfaces.ILocationService
import com.elyonut.wow.interfaces.IPermissions
import com.elyonut.wow.model.Coordinate
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.utilities.MapStates
import com.google.android.material.checkbox.MaterialCheckBox
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.*

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val vectorLayersManager = VectorLayersManager.getInstance(application)
    private val threatAnalyzer = ThreatAnalyzer.getInstance(getApplication())
    private var _mapStateChanged = MutableLiveData<MapStates>()
    val mapStateChanged: LiveData<MapStates>
        get() = _mapStateChanged
    val chosenLayerId = MutableLiveData<String>()
    val selectedExperimentalOption = MutableLiveData<Int>()
    val filterSelected = MutableLiveData<Boolean>()
    val coverageSettingsSelected = MutableLiveData<Boolean>()
    val shouldDefineArea = MutableLiveData<Boolean>()
    val shouldOpenAlertsFragment = MutableLiveData<Boolean>()
    val shouldOpenThreatsFragment = MutableLiveData<Boolean>()
    val chosenTypeToFilter = MutableLiveData<Pair<String, Boolean>>()
    val isSelectAllChecked = MutableLiveData<Boolean>()
    private var _isPermissionRequestNeeded = MutableLiveData<Boolean>()
    val isPermissionRequestNeeded: LiveData<Boolean>
        get() = _isPermissionRequestNeeded
    private var _isPermissionDialogShown = MutableLiveData<Boolean>()
    val isPermissionDialogShown: LiveData<Boolean>
        get() = _isPermissionDialogShown
    private var locationService: ILocationService = LocationService.getInstance(getApplication())
    private val permissions: IPermissions =
        PermissionsService.getInstance(application)
    private var coverageSearchHeightMetersChecked: Boolean = false
    val coordinatesFeaturesInCoverage = MutableLiveData<List<Feature>>()
    private val _isProgressBarVisible = MutableLiveData<Boolean>()
    val isProgressBarVisible: LiveData<Boolean>
        get() = _isProgressBarVisible
    var mapLayers: LiveData<List<LayerModel>> =
        Transformations.map(vectorLayersManager.layers, ::layersUpdated)

    private fun layersUpdated(layers: List<LayerModel>) = layers

    fun locationSetUp() {
        if (permissions.isLocationPermitted()) {
            startLocationService()
        } else {
            _isPermissionRequestNeeded.postValue(true)
        }
    }

    private fun startLocationService() {
        if (!locationService.isGpsEnabled()) {
            _isPermissionDialogShown.postValue(true)
        }

        locationService.startLocationService()
    }

    fun mapClicked(
        latLng: LatLng
    ) {
        _isProgressBarVisible.postValue(false)
        when (_mapStateChanged.value) {
            MapStates.LOS_BUILDINGS_TO_LOCATION -> {
                // How to get the building at location? How to pass it here?
                _mapStateChanged.value = MapStates.REGULAR
            }
            MapStates.CALCULATE_COORDINATES_IN_RANGE -> {
                calculateCoverage(latLng)
            }
        }

    }

    private fun calculateCoverage(latLng: LatLng) {
        val coverageRangeMeters: Double = Constants.DEFAULT_COVERAGE_RANGE_METERS
        val coverageResolutionMeters: Double = Constants.DEFAULT_COVERAGE_RESOLUTION_METERS
        val coverageSearchHeightMeters: Double = Constants.DEFAULT_COVERAGE_HEIGHT_METERS
        var coordinates: Deferred<List<Coordinate>>
        CoroutineScope(Dispatchers.Default).launch {
            coordinates = async {
                if (coverageSearchHeightMetersChecked) {
                    return@async threatAnalyzer.calculateCoverageAlpha(
                        latLng,
                        coverageRangeMeters,
                        coverageResolutionMeters,
                        coverageSearchHeightMeters
                    )
                } else {
                    return@async threatAnalyzer.calculateCoverageAlpha(
                        latLng,
                        coverageRangeMeters,
                        coverageResolutionMeters,
                        Constants.DEFAULT_COVERAGE_HEIGHT_METERS
                    )
                }
            }

            coordinatesFeaturesInCoverage.postValue(coordinates.await().map { coordinate ->
                Feature.fromGeometry(
                    Point.fromLngLat(
                        coordinate.longitude,
                        coordinate.latitude
                    )
                )
            })
            _isProgressBarVisible.postValue(true)
            _mapStateChanged.postValue(MapStates.REGULAR) // Maybe make it a toggle? a mode that should be stopped, like the area of interest
        }
    }

    fun coverageSearchHeightMetersCheckedChanged(coverageSearchHeightChecked: Boolean) {
        coverageSearchHeightMetersChecked = coverageSearchHeightChecked
    }

    fun onNavigationItemSelected(item: MenuItem): Boolean {
        var shouldCloseDrawer = true

        when {
            item.groupId == R.id.nav_layers -> {
                val layerModel = item.actionView.tag as LayerModel
                chosenLayerId.postValue(layerModel.id)
                shouldCloseDrawer = false
            }
            item.itemId == R.id.filterButton -> {
                filterSelected.postValue(true)
                shouldCloseDrawer = false
            }
            item.itemId == R.id.select_all -> {
                isSelectAllChecked.postValue((item.actionView as MaterialCheckBox).isChecked)
            }
            item.groupId == R.id.filter_options -> {
                chosenTypeToFilter.postValue(
                    Pair(
                        item.actionView.tag as String,
                        (item.actionView as MaterialCheckBox).isChecked
                    )
                )
                shouldCloseDrawer = false
            }
            item.itemId == R.id.los_buildings_to_location -> {
                _mapStateChanged.value = MapStates.LOS_BUILDINGS_TO_LOCATION
                Toast.makeText(getApplication(), "Select Location", Toast.LENGTH_LONG).show()
            }
            item.itemId == R.id.calculate_coverage -> {
                _mapStateChanged.value = MapStates.CALCULATE_COORDINATES_IN_RANGE
            }
            item.itemId == R.id.define_area -> {
                _mapStateChanged.value = MapStates.DRAWING
                if (shouldDefineArea.value == null || !shouldDefineArea.value!!) {
                    shouldDefineArea.postValue(true)
                }
            }
            item.itemId == R.id.alerts -> {
                shouldOpenAlertsFragment.postValue(true)
            }
            item.itemId == R.id.coverage_settings -> {
                coverageSettingsSelected.postValue(true)
                shouldCloseDrawer = false
            }
            item.itemId == R.id.awareness_status || item.itemId == R.id.threat_list_menu_item -> {
                shouldOpenThreatsFragment.postValue(true)
            }
        }

        return shouldCloseDrawer
    }

    fun getLayerTypeValues(): List<String>? {
        return vectorLayersManager.getValuesOfLayerProperty(Constants.THREAT_LAYER_ID, "type")
    }
}