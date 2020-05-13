package com.elyonut.wow.model

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE
import com.mapbox.geojson.Feature

class Threat() : Parcelable {
    var id: String = ""
    var name: String = ""
    var description: String = ""
    var level: ThreatLevel = ThreatLevel.None
    var creator: String = ""
    var distanceMeters: Double = 0.0
    lateinit var location: GeoLocation
    lateinit var feature: Feature
    var isLos: Boolean = false
    var azimuth: Double = 0.0
    var type: String = ""
    var height: Double = 0.0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var eAmount: String = ""
    var knowledgeType: String = ""
    var range: Double = 0.0

    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        name = parcel.readString()
        description = parcel.readString()
        creator = parcel.readString()
        distanceMeters = parcel.readDouble()
        level = ThreatLevel.valueOf(parcel.readString())
        location = parcel.readParcelable(GeoLocation::class.java.classLoader)
        feature = Feature.fromJson(parcel.readString())
        isLos = parcel.readInt() == 1
        azimuth = parcel.readDouble()
        type = parcel.readString()
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
        parcel.writeString(type)
        parcel.writeDouble(height)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(eAmount)
        parcel.writeString(knowledgeType)
        parcel.writeDouble(range)
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