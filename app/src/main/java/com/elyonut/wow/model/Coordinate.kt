package com.elyonut.wow.model

import android.os.Parcel
import android.os.Parcelable

class Coordinate (var latitude: Double, var longitude: Double) : Parcelable {

    var heightMeters: Double = -10000.0

    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Coordinate> {
        override fun createFromParcel(parcel: Parcel): Coordinate {
            return Coordinate(parcel)
        }

        override fun newArray(size: Int): Array<Coordinate?> {
            return arrayOfNulls(size)
        }
    }


}
