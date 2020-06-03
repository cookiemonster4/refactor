package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.AlertsManager
import com.elyonut.wow.database.DB
import com.elyonut.wow.utilities.Constants
import com.elyonut.wow.utilities.NumericFilterTypes
import com.elyonut.wow.model.Threat
import com.elyonut.wow.utilities.MapStates
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.geometry.LatLng

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    val selectedLayerId = MutableLiveData<String>()
    val selectedExperimentalOption = MutableLiveData<Int>()
    val selectedThreatItem = MutableLiveData<Threat>()
    var layerToFilterId = ""
    var chosenPropertyId = ""
    var chosenPropertyValue = ""
    var minValue: Number = 0
    var maxValue: Number = 0
    var specificValue: Number = 0
    val shouldApplyFilter = MutableLiveData<Boolean>()
    var isStringType: Boolean = false
    lateinit var numericType: NumericFilterTypes
    var shouldDefineArea = MutableLiveData<Boolean>()
    var areaOfInterest: Polygon? = null
    var mapState = MapStates.REGULAR
    var coverageRangeMeters: Double = Constants.DEFAULT_COVERAGE_RANGE_METERS
    var coverageResolutionMeters: Double = Constants.DEFAULT_COVERAGE_RESOLUTION_METERS
    var coverageSearchHeightMeters: Double = Constants.DEFAULT_COVERAGE_HEIGHT_METERS
    var coverageSearchHeightMetersChecked = MutableLiveData<Boolean>()
    var mapClickedLatlng = MutableLiveData<LatLng>()
    var alertsManager = AlertsManager(application, DB.getInstance(application).alertDatabaseDao)
    var isExposed = MutableLiveData<Boolean>()
    val chosenTypeToFilter = MutableLiveData<Pair<String, Boolean>>()
    val isSelectAllChecked = MutableLiveData<Boolean>()
    val mapStyleURL = MutableLiveData<String>()
    val shouldRemoveSelectedBuildingLayer = MutableLiveData<Boolean>()
    val coordinatesFeaturesInCoverage = MutableLiveData<List<Feature>>()

    fun applySaveCoverageSettingsButtonClicked(
        coverageRange: Double,
        resolution: Double,
        height: Double?,
        heightChecked: Boolean
    ) {
        this.coverageRangeMeters = coverageRange
        this.coverageResolutionMeters = resolution

        height?.let {
            this.coverageSearchHeightMeters = it
        }
        this.coverageSearchHeightMetersChecked.postValue(heightChecked)
    }
}