package com.elyonut.wow

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LatLngModel
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.utilities.TempDB
import com.google.gson.JsonPrimitive
import kotlin.reflect.KClass

class VectorLayersManager private constructor(context: Context) {
    private val tempDB = TempDB.getInstance(context)
    private var _layers = MutableLiveData<MutableList<LayerModel>>()
    val layers: LiveData<MutableList<LayerModel>>
        get() = _layers


    init {
        _layers.value = tempDB.getLayers()
    }

    companion object : SingletonHolder<VectorLayersManager, Context>(::VectorLayersManager)

    fun addLayer(id: String, name: String, features: List<FeatureModel>) {
        _layers.value?.add(LayerModel(id, name, features))
    }

    fun updateLayer(id: String, features: List<FeatureModel>) {
        val tempLayersList =_layers.value
        tempLayersList?.find { layer -> id == layer.id }?.features = features
        _layers.value = tempLayersList
    }

    fun getLayerById(id: String): List<FeatureModel>? {
        return _layers.value?.find { layer -> id == layer.id }?.features
    }

    fun initLayersIdList(): List<String>? {
        return _layers.value?.map { it.id }
    }

    // for filter
    fun getLayerProperties(id: String): HashMap<String, KClass<*>> {
        val currentLayer = getLayerById(id)
        val propertiesHashMap = HashMap<String, KClass<*>>()

        // Temp- until we have a real DB and real data
        currentLayer?.first()?.properties?.entrySet()?.forEach {
            if ((it.value as JsonPrimitive).isNumber) {
                propertiesHashMap[it.key] = Number::class
            } else if ((it.value as JsonPrimitive).isString) {
                propertiesHashMap[it.key] = String::class
            }
        }

        return propertiesHashMap
    }

    // for filter
    fun getValuesOfLayerProperty(layerId: String, propertyName: String): List<String>? {
        return getLayerById(layerId)?.map { a -> a.properties.get(propertyName)!!.asString }
            ?.distinct()
    }

    fun getFeatureLocation(featureID: String): LatLngModel {
        var feature: FeatureModel? = null
        _layers.value?.forEach { it ->
            feature = it.features.find { it.id == featureID }

            if (feature != null) {
                return LatLngModel(
                    feature?.properties?.get("latitude")!!.asDouble,
                    feature?.properties?.get("longitude")!!.asDouble
                )
            }
        }

        return LatLngModel(
            feature?.properties?.get("latitude")!!.asDouble,
            feature?.properties?.get("longitude")!!.asDouble
        )
    }

    fun getFeatureName(featureID: String): String {
        var feature: FeatureModel? = null
        _layers.value?.forEach { it ->
            feature = it.features.find { it.id == featureID }

            if (feature != null) {
                return feature?.properties?.get("namestr")!!.asString
            }

        }

        return feature?.properties?.get("namestr")!!.asString
    }
}
