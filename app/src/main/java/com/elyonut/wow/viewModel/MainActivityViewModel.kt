package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.R
import com.elyonut.wow.adapter.LocationService
import com.elyonut.wow.adapter.PermissionsService
import com.elyonut.wow.interfaces.ILocationService
import com.elyonut.wow.interfaces.IPermissions
import com.elyonut.wow.utilities.TempDB
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.utilities.Constants
import com.google.android.material.checkbox.MaterialCheckBox

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val layerManager = LayerManager(TempDB((application)))
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

    fun getLayersList(): List<LayerModel>? {
        return layerManager.layers
    }

    fun getLayerTypeValues(): List<String>? {
        return layerManager.getValuesOfLayerProperty(Constants.THREAT_LAYER_ID, "type")
    }
}

