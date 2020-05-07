package com.elyonut.wow

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.model.MapLayer
import com.elyonut.wow.utilities.Maps

class MapsManager(context: Context) {
    var maps = MutableLiveData<ArrayList<MapLayer>>()

    init {
        maps.value = arrayListOf(
            MapLayer(
                Maps.MAPBOX_STYLE_URL,
                "Basic",
                context.resources.getIdentifier("basic_map", "drawable", context.packageName)
            ),
            MapLayer(
                Maps.MAPBOX_MAP1,
                "Blue",
                context.resources.getIdentifier("blue_map", "drawable", context.packageName)
            ),
            MapLayer(
                Maps.MAPBOX_MAP2,
                "Red",
                context.resources.getIdentifier("red_map", "drawable", context.packageName)
            ),
            MapLayer(
                Maps.MAPBOX_MAP3,
                "Green",
                context.resources.getIdentifier("green_map", "drawable", context.packageName)
            ),
            MapLayer(
                Maps.MAPBOX_SATELLITE,
                "Satellite"
            )
        )
    }
}
