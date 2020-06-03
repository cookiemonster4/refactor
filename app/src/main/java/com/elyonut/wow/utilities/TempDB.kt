package com.elyonut.wow.utilities

import android.content.Context
import com.elyonut.wow.SingletonHolder
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LayerModel
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class TempDB private constructor(context: Context) {
    private val layers = ArrayList<LayerModel>()
    private var threatLayer: List<FeatureModel> = listOf()

    init {
        val gson = GsonBuilder().create()

        val buffer =
            BufferedReader(InputStreamReader(context.assets.open("constructionFeatures2.geojson")))
        val restaurantsBuffer =
            BufferedReader(InputStreamReader(context.assets.open("restaurants.geojson")))
        val centerTLVBuffer =
            BufferedReader(InputStreamReader(context.assets.open("buildingsCenterTLVExtended.geojson")))

        threatLayer = gson.fromJson(buffer, Array<FeatureModel>::class.java).toList()
        val restaurantsFeatures = gson.fromJson(restaurantsBuffer, Array<FeatureModel>::class.java)
        val centerTLVFeatures = gson.fromJson(centerTLVBuffer, Array<FeatureModel>::class.java)

        val restaurantsLayerModel =
            LayerModel("restaurants", "מסעדות", restaurantsFeatures.toList())
        val centerTLVLayerModel =
            LayerModel(Constants.BUILDINGS_LAYER_ID, "מרכז תל אביב", centerTLVFeatures.toList())

        layers.add(restaurantsLayerModel)
        layers.add(centerTLVLayerModel)
    }

    companion object : SingletonHolder<TempDB, Context>(::TempDB)

    fun getLayers(): ArrayList<LayerModel> = layers
    fun getThreatLayer() = threatLayer
}