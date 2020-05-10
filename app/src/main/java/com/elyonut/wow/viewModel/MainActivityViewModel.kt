package com.elyonut.wow.viewModel

import android.app.Application
import android.view.MenuItem
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.R
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

    fun onNavigationItemSelected(item: MenuItem): Boolean {
        var shouldCloseDrawer = true

        when {
            item.groupId == R.id.nav_layers -> {
                val layerModel = item.actionView.tag as LayerModel
                chosenLayerId.value = layerModel.id
                shouldCloseDrawer = false
            }
            item.itemId == R.id.filterButton -> {
                filterSelected.value = true
                shouldCloseDrawer = false
            }
            item.itemId == R.id.select_all -> {
                isSelectAllChecked.value = (item.actionView as MaterialCheckBox).isChecked
            }
            item.groupId == R.id.filter_options -> {
                chosenTypeToFilter.value = Pair(item.actionView.tag as String, (item.actionView as MaterialCheckBox).isChecked)
                shouldCloseDrawer = false
            }
            item.groupId == R.id.nav_experiments ->
                this.selectedExperimentalOption.value = item.itemId
            item.itemId == R.id.define_area -> {
                if (shouldDefineArea.value == null || !shouldDefineArea.value!!) {
                    shouldDefineArea.value = true
                }
            }
            item.itemId == R.id.alerts -> {
                shouldOpenAlertsFragment.value = true
            }
            item.itemId == R.id.coverage_settings -> {
                coverageSettingsSelected.value = true
                shouldCloseDrawer = false
            }
            item.itemId == R.id.visibility_status -> {
                shouldOpenThreatsFragment.value = true
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

