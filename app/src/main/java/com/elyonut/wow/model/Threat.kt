package com.elyonut.wow.model

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE
import com.elyonut.wow.utilities.BuildingTypeMapping
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import kotlinx.android.parcel.RawValue
import java.lang.StringBuilder

class Threat(id: String, geometry: PolygonModel, properties: JsonObject) :
    FeatureModel(id, geometry, properties, "feature"), Parcelable {
    var name: String
    var level: ThreatLevel = ThreatLevel.None
    var distanceMeters: Double = 0.0
    var isLos: Boolean = false
    var azimuth: Double = 0.0
    var enemyType: String
    var height: Double
    var latitude: Double
    var longitude: Double
    var range: Double

    init {
        name = properties.get("namestr")?.asString ?: ""
        height = properties.get("height")?.asDouble ?: 0.0
        latitude = properties.get("latitude")?.asDouble ?: 0.0
        longitude = properties.get("longitude")?.asDouble ?: 0.0
        enemyType = properties.get("type")?.asString ?: ""
        range = properties.get("range")?.asDouble ?: 0.0
    }

    constructor(featureModel: FeatureModel) : this(
        featureModel.id,
        featureModel.geometry,
        featureModel.properties
    )

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readParcelable(PolygonModel::class.java.classLoader),
        parcel.readValue(JsonObject::class.java.classLoader) as JsonObject
    ) {
        name = parcel.readString()
        distanceMeters = parcel.readDouble()
        level = ThreatLevel.valueOf(parcel.readString())
        isLos = parcel.readInt() == 1
        azimuth = parcel.readDouble()
        enemyType = parcel.readString()
        height = parcel.readDouble()
        latitude = parcel.readDouble()
        longitude = parcel.readDouble()
        range = parcel.readDouble()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeDouble(distanceMeters)
        parcel.writeString(level.name)
        parcel.writeInt(if (isLos) 1 else 0)
        parcel.writeDouble(azimuth)
        parcel.writeString(enemyType)
        parcel.writeDouble(height)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeDouble(range)
        parcel.writeValue(properties)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toStringProperties(): String {
        val builder = StringBuilder()
        builder.append("מרחק: $distanceMeters \n")
        builder.append("אזימוט: $azimuth \n")
        builder.append("האם בקו ראיה: ${if (isLos) "כן" else "לא"}\n")
        builder.append(super.toStringProperties())

        return builder.toString()
    }

    override fun getTitle(): String = name

    override fun getImageUrl(): Int? {
        return BuildingTypeMapping.mapping[enemyType]
    }

    companion object CREATOR : Parcelable.Creator<Threat> {
        override fun createFromParcel(parcel: Parcel): Threat {
            return Threat(parcel)
        }

        override fun newArray(size: Int): Array<Threat?> {
            return arrayOfNulls(size)
        }
    }
}