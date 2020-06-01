package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import android.widget.ProgressBar
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
import com.google.android.material.checkbox.MaterialCheckBox
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.*
import timber.log.Timber

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val mapVectorLayersManager = VectorLayersManager.getInstance(application)
    private val threatAnalyzer = ThreatAnalyzer.getInstance(getApplication())
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
    private val _removeProgressBar = MutableLiveData<ProgressBar>()
    val removeProgressBar: LiveData<ProgressBar>
        get() = _removeProgressBar
    var mapLayers: LiveData<List<LayerModel>> =
        Transformations.map(mapVectorLayersManager.layers, ::layersUpdated)

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
        latLng: LatLng,
        progressBar: ProgressBar
    ) {
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
            Timber.i(
                "main activity coordinates:  %s",
                coordinatesFeaturesInCoverage.value.toString()
            )
            _removeProgressBar.postValue(progressBar)
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
            item.groupId == R.id.nav_experiments ->
                this.selectedExperimentalOption.postValue(item.itemId)
            item.itemId == R.id.define_area -> {
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
            item.itemId == R.id.visibility_status -> {
                shouldOpenThreatsFragment.postValue(true)
            }
        }

        return shouldCloseDrawer
    }

    fun getLayerTypeValues(): List<String>? {
        return mapVectorLayersManager.getValuesOfLayerProperty(Constants.THREAT_LAYER_ID, "type")
    }
}

