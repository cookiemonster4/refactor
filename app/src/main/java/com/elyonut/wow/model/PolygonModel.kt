package com.elyonut.wow.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PolygonModel(var coordinates: List<List<List<Double>>>, var type: String) : Geometry, Parcelable