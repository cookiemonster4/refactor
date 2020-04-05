package com.elyonut.wow.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.LayerManager
import com.elyonut.wow.utilities.NumericFilterTypes
//import com.elyonut.wow.utilities.NumericFilterTypes
import com.elyonut.wow.utilities.TempDB
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class FilterViewModel(application: Application) : AndroidViewModel(application) {
    // TODO: null handling

    private val layerManager = LayerManager(TempDB((application)))
    private lateinit var propertiesList: List<String>
    private var propertiesHashMap = HashMap<String, KClass<*>>()
    var chosenLayerId = MutableLiveData<String>()
    var chosenProperty = MutableLiveData<String>()
    var layersIdsList: List<String>
    var numberFilterOptions: List<String>
    var isStringType = MutableLiveData<Boolean>()
    var numericTypeChosen = MutableLiveData<NumericFilterTypes>()
    val shouldApplyFilter = MutableLiveData<Boolean>()

    init {
        layersIdsList = layerManager.initLayersIdList()!!
        numberFilterOptions =
            NumericFilterTypes.values().map { filterType -> filterType.hebrewName }.toList()
    }

    fun applyFilterButtonClicked(shouldApply: Boolean) {
        shouldApplyFilter.value = shouldApply
    }

    fun initPropertiesList(layerId: String): List<String>? {
        propertiesHashMap = layerManager.getLayerProperties(layerId)
        propertiesList = propertiesHashMap.keys.toList()
        chosenProperty.value = propertiesList.first()

        return propertiesList
    }

    fun initOptionsList(propertyName: String) {
        checkPropertyType(propertyName)
    }

    private fun checkPropertyType(propertyName: String) {
        val propertyType = getPropertyType(propertyName)
        if (propertyType != null) {
            if (propertyType.isSubclassOf(java.lang.Number::class)) {
                onNumberItemSelected(0)
                isStringType.value = false

            } else if (propertyType.isSubclassOf(java.lang.String::class)) {
                onNumberItemSelected(0)
                isStringType.value = true
            }
        }
    }

    private fun getPropertyType(propertyName: String): KClass<*>? {
        return propertiesHashMap[propertyName]
    }

    fun onLayerItemSelected(position: Int) {
        chosenLayerId.value = layersIdsList[position]
    }

    fun onPropertyItemSelected(position: Int) {
        propertiesHashMap =
            layerManager.getLayerProperties(chosenLayerId.value!!)
        chosenProperty.value = propertiesList[position]
    }

    fun initStringPropertyOptions(propertyName: String): List<String>? {
        return layerManager.getValuesOfLayerProperty(
            chosenLayerId.value!!,
            propertyName
        ) // TODO: null handling
    }

    fun onNumberItemSelected(position: Int) {
        when (numberFilterOptions[position]) {
            NumericFilterTypes.GREATER.hebrewName -> {
                numericTypeChosen.value = NumericFilterTypes.GREATER
            }
            NumericFilterTypes.LOWER.hebrewName -> {
                numericTypeChosen.value = NumericFilterTypes.LOWER
            }
            NumericFilterTypes.RANGE.hebrewName -> {
                numericTypeChosen.value = NumericFilterTypes.RANGE
            }
            NumericFilterTypes.SPECIFIC.hebrewName -> {
                numericTypeChosen.value = NumericFilterTypes.SPECIFIC
            }
        }
    }
}