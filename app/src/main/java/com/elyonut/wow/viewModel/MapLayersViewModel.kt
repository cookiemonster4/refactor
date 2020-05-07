package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.MapsManager
import com.elyonut.wow.model.MapLayer

class MapLayersViewModel(application: Application): AndroidViewModel(application) {
    private var mapsManager: MapsManager = MapsManager(application)
    var mapLayers: MutableLiveData<ArrayList<MapLayer>>?

    init {
        mapLayers = mapsManager.maps
    }
}