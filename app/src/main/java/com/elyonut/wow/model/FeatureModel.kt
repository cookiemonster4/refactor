package com.elyonut.wow.model

import android.os.Parcelable
import com.google.gson.JsonObject
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
open class FeatureModel(
    val id: String,
    var geometry: PolygonModel,
    var properties: @RawValue JsonObject = JsonObject(),
    var type: String
) : Parcelable