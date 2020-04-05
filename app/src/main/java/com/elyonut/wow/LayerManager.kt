package com.elyonut.wow

import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LatLngModel
import com.elyonut.wow.model.LayerModel
import com.elyonut.wow.utilities.TempDB
import com.google.gson.JsonPrimitive
import kotlin.reflect.KClass

class LayerManager(tempDB: TempDB) {
    var layers: List<LayerModel>? = null

    init {
        layers = tempDB.getFeatures()
    }

    fun getLayer(id: String): List<FeatureModel>? {
        return layers?.find { layer -> id == layer.id }?.features
    }

    fun initLayersIdList(): List<String>? {
        return layers?.map { it.id }
    }

    fun getLayerProperties(id: String): HashMap<String, KClass<*>> {
        val currentLayer = getLayer(id)
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

    fun getValuesOfLayerProperty(layerId: String, propertyName: String): List<String>? {
           return getLayer(layerId)?.map { a -> a.properties?.get(propertyName)!!.asString}?.distinct()
    }

    fun getFeatureLocation(featureID: String): LatLngModel {
        var feature: FeatureModel? = null
        layers?.forEach { it ->
            feature = it.features.find { it.id == featureID }

            if(feature != null){
                return LatLngModel(feature?.properties?.get("latitude")!!.asDouble, feature?.properties?.get("longitude")!!.asDouble)
            }
        }

        return LatLngModel(feature?.properties?.get("latitude")!!.asDouble, feature?.properties?.get("longitude")!!.asDouble)
    }

    fun getFeatureName(featureID: String): String {
        var feature: FeatureModel? = null
        layers?.forEach { it ->
            feature = it.features.find { it.id == featureID }

            if(feature != null){
                return feature?.properties?.get("namestr")!!.asString
            }

        }

        return feature?.properties?.get("namestr")!!.asString
    }
}
