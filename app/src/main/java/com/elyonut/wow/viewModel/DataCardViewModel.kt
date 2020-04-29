package com.elyonut.wow.viewModel

import android.app.Application
import android.content.res.Resources
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProviders
import com.elyonut.wow.utilities.BuildingTypeMapping

class DataCardViewModel(application: Application) : AndroidViewModel(application) {
    var isReadMoreButtonClicked = MutableLiveData<Boolean>()
    var shouldCloseCard = MutableLiveData<Boolean>()

    init {
        isReadMoreButtonClicked.value = false
    }

    fun readMoreButtonClicked() {
        isReadMoreButtonClicked.value =
            !(isReadMoreButtonClicked.value != null && isReadMoreButtonClicked.value!!)
    }

    fun close() {
        shouldCloseCard.value = true
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

    fun getImageUrl(featureType: String): Int? {
        return BuildingTypeMapping.mapping[featureType]
    }
}