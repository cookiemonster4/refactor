package com.elyonut.wow.model

import android.os.Parcel
import android.os.Parcelable

class GeoLocation(var locationType: LocationType, var coordinates: ArrayList<Coordinate>) : Parcelable{
    constructor(parcel: Parcel) : this(
        LocationType.valueOf(parcel.readString()),
        parcel.readArrayList(Coordinate.javaClass.classLoader) as ArrayList<Coordinate>
    ) {


    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(locationType.name)
        parcel.writeList(coordinates)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GeoLocation> {
        override fun createFromParcel(parcel: Parcel): GeoLocation {
            return GeoLocation(parcel)
        }

        override fun newArray(size: Int): Array<GeoLocation?> {
            return arrayOfNulls(size)
        }
    }
}