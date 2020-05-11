package com.elyonut.wow.viewModel

import android.app.Application
import android.content.res.Resources
import android.widget.FrameLayout
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elyonut.wow.utilities.BuildingTypeMapping

class DataCardViewModel(application: Application) : AndroidViewModel(application) {
    var isReadMoreButtonClicked = MutableLiveData<Boolean>()
    var shouldCloseCard = MutableLiveData<Boolean>()
    private val _navigateToMapFragment = MutableLiveData<Boolean>()
    val navigateToMapFragment
        get() = _navigateToMapFragment

    init {
        isReadMoreButtonClicked.postValue(false)
    }

    fun readMoreButtonClicked() {
        isReadMoreButtonClicked.postValue(!isReadMoreButtonClicked.value!!)
    }

    fun onSwiftLeftOrRight() {
        _navigateToMapFragment.postValue(true)
    }

    fun onCloseClicked() {
        _navigateToMapFragment.postValue(true)
    }

    fun doneNavigating() {
        _navigateToMapFragment.postValue(null)
    }

    fun close() {
        shouldCloseCard.postValue(true)
    }

    fun getRelativeLayoutParams(sizeRelativelyToScreen: Double): FrameLayout.LayoutParams { // TODO Should this be here ???
        return FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            (getDeviceHeight() * sizeRelativelyToScreen).toInt()
        )
    }

    private fun getDeviceHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    fun getImageUrl(buildingType: String): Int? {
        return BuildingTypeMapping.mapping[buildingType]
    }
}