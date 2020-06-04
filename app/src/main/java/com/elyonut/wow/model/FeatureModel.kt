package com.elyonut.wow.model

import android.os.Parcelable
import com.google.gson.JsonObject
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.lang.StringBuilder

@Parcelize
open class FeatureModel(
    val id: String,
    var geometry: PolygonModel,
    var properties: @RawValue JsonObject = JsonObject(),
    var type: String
) : Parcelable {
    open fun toStringProperties(): String {
        val builder = StringBuilder()
        properties.keySet()
            .forEach { key -> builder.append(key + ": " + properties[key] + "\n") }
        return builder.toString()
    }

    open fun getTitle() = "Feature $id"
    open fun getImageUrl(): Int? = properties.get("imageUrl").asInt
}