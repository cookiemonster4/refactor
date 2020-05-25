package com.elyonut.wow.model

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import kotlinx.android.parcel.RawValue

class Threat(id: String, geometry: PolygonModel, properties: JsonObject) :
    FeatureModel(id, geometry, properties, "feature"), Parcelable {
    var name: String = ""
    var description: String = "" // do i need it outside of properties?
    var level: ThreatLevel = ThreatLevel.None
    var creator: String = ""
    var distanceMeters: Double = 0.0
    lateinit var location: GeoLocation // Geometry?
    lateinit var feature: Feature

    //    var properties: @RawValue JsonObject? = JsonObject()
    var isLos: Boolean = false
    var azimuth: Double = 0.0
    var enemyType: String = ""
    var height: Double = 0.0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var eAmount: String = ""
    var knowledgeType: String = ""
    var range: Double = 0.0

    init {
        name = properties.get("namestr")?.asString ?: ""
        height = properties.get("height")?.asDouble ?: 0.0
        latitude = properties.get("latitude")?.asDouble ?: 0.0
        longitude = properties.get("longitude")?.asDouble ?: 0.0
        enemyType = properties.get("type")?.asString ?: ""
        range = properties.get("range")?.asDouble ?: 0.0
    }
//    constructor(id: String, geometry: Geometry, properties: JsonObject) : this() {
//        this.id = id
//        name = properties.get("namestr")?.asString ?: ""
//        height = properties.get("height")?.asDouble ?: 0.0
//        latitude = properties.get("latitude")?.asDouble ?: 0.0
//        longitude = properties.get("longitude")?.asDouble ?: 0.0
//        enemyType = properties.get("type")?.asString ?: ""
//        range = properties.get("range")?.asDouble ?: 0.0
//    }

    constructor(featureModel: FeatureModel) : this(
        featureModel.id,
        featureModel.geometry,
        featureModel.properties
    ) {
//        Threat(featureModel.id, featureModel.geometry, featureModel.properties)
    }

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readParcelable(PolygonModel::class.java.classLoader),
        parcel.readValue(JsonObject::class.java.classLoader) as JsonObject
    ) {
        name = parcel.readString()
        description = parcel.readString()
        creator = parcel.readString()
        distanceMeters = parcel.readDouble()
        level = ThreatLevel.valueOf(parcel.readString())
        location = parcel.readParcelable(GeoLocation::class.java.classLoader)
        feature = Feature.fromJson(parcel.readString())
        isLos = parcel.readInt() == 1
        azimuth = parcel.readDouble()
        enemyType = parcel.readString()
        height = parcel.readDouble()
        latitude = parcel.readDouble()
        longitude = parcel.readDouble()
        eAmount = parcel.readString()
        knowledgeType = parcel.readString()
        range = parcel.readDouble()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(creator)
        parcel.writeDouble(distanceMeters)
        parcel.writeString(level.name)
        parcel.writeParcelable(location, PARCELABLE_WRITE_RETURN_VALUE)
        parcel.writeString(feature.toJson())
        parcel.writeInt(if (isLos) 1 else 0)
        parcel.writeDouble(azimuth)
        parcel.writeString(enemyType)
        parcel.writeDouble(height)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(eAmount)
        parcel.writeString(knowledgeType)
        parcel.writeDouble(range)
        parcel.writeValue(properties)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Threat> {
        override fun createFromParcel(parcel: Parcel): Threat {
            return Threat(parcel)
        }

        override fun newArray(size: Int): Array<Threat?> {
            return arrayOfNulls(size)
        }

        fun color(threat: Threat): Int {
            return when (threat.level) {
                ThreatLevel.None -> RiskStatus.NONE.color
                ThreatLevel.Low -> RiskStatus.LOW.color
                ThreatLevel.Medium -> RiskStatus.MEDIUM.color
                ThreatLevel.High -> RiskStatus.HIGH.color
            }
        }
    }
}