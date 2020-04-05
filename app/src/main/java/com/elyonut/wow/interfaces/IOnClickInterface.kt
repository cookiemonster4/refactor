package com.elyonut.wow.interfaces

import android.location.Location
import android.view.View
import com.elyonut.wow.model.RiskStatus
import java.util.*
import kotlin.collections.ArrayList

interface OnClickInterface {
    fun setClick(view: View, position: Int)
}